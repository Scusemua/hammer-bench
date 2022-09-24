/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package io.hops.experiments.benchmarks.interleaved;

import io.hops.experiments.benchmarks.common.BenchMarkFileSystemName;
import io.hops.experiments.benchmarks.common.config.BMConfiguration;
import io.hops.experiments.utils.BMOperationsUtils;
import io.hops.experiments.benchmarks.common.Benchmark;
import io.hops.experiments.benchmarks.common.BenchmarkDistribution;
import io.hops.experiments.benchmarks.common.BenchmarkOperations;
import io.hops.experiments.benchmarks.common.BMOpStats;
import io.hops.experiments.benchmarks.common.commands.NamespaceWarmUp;
import io.hops.experiments.benchmarks.interleaved.coin.InterleavedMultiFaceCoin;
import io.hops.experiments.controller.Logger;
import io.hops.experiments.controller.commands.BenchmarkCommand;
import io.hops.experiments.controller.commands.WarmUpCommand;
import io.hops.experiments.utils.DFSOperationsUtils;
import io.hops.experiments.workload.generator.FilePool;
import io.hops.experiments.workload.limiter.DistributionRateLimiter;
import io.hops.experiments.workload.limiter.ParetoGenerator;
import io.hops.experiments.workload.limiter.PoissonGenerator;
import io.hops.experiments.workload.limiter.RateLimiter;
import io.hops.experiments.workload.limiter.RateNoLimiter;
import io.hops.experiments.workload.limiter.WorkerRateLimiter;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.math3.stat.descriptive.SynchronizedDescriptiveStatistics;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author salman
 */
public class InterleavedBenchmark extends Benchmark {
  public static final Log LOG = LogFactory.getLog(InterleavedBenchmark.class);
  private long duration;
  private long startTime = 0;
  AtomicLong operationsCompleted = new AtomicLong(0);
  AtomicLong operationsFailed = new AtomicLong(0);
  Map<BenchmarkOperations, AtomicLong> operationsStats = new HashMap<BenchmarkOperations, AtomicLong>();
  HashMap<BenchmarkOperations, ArrayList<BMOpStats>> opsStats = new HashMap<BenchmarkOperations, ArrayList<BMOpStats>>();
  SynchronizedDescriptiveStatistics avgLatency = new SynchronizedDescriptiveStatistics();
  protected final RateLimiter limiter;
  protected boolean debug = false;

  public InterleavedBenchmark(Configuration conf, BMConfiguration bmConf) {
    super(conf, bmConf);
    BenchmarkDistribution distribution = bmConf.getInterleavedBMIaTDistribution();
    if (distribution == BenchmarkDistribution.POISSON) {
      LOG.debug("Using a Poisson-based rate limiter.");
      limiter = new DistributionRateLimiter(bmConf, new PoissonGenerator(bmConf));
    } else if (distribution == BenchmarkDistribution.PARETO) {
      LOG.debug("Using a Pareto-based rate limiter.");
      limiter = new DistributionRateLimiter(bmConf, new ParetoGenerator(bmConf));
    } else {
      LOG.debug("Not using any rate limiter.");
      limiter = new RateNoLimiter();
    }

    if (bmConf.getBenchMarkFileSystemName() == BenchMarkFileSystemName.HDFS || bmConf.getBenchMarkFileSystemName() == BenchMarkFileSystemName.HopsFS) {
      if (!Objects.equals(bmConf.getHadoopHomeDir(), "")) {
        System.setProperty("hadoop.home.dir", bmConf.getHadoopHomeDir());
      }
    }
  }

  @Override
  protected WarmUpCommand.Response warmUp(WarmUpCommand.Request cmd)
          throws IOException, InterruptedException {
    // Warn up is done in two stages.
    // In the first phase all the parent dirs are created
    // and then in the second stage we create the further
    // file/dir in the parent dir.
    LOG.debug("Performing warm-up now!\n\n\n");

    if (bmConf.getFilesToCreateInWarmUpPhase() > 1) {
      List<Callable<Object>> workers = new ArrayList<Callable<Object>>();
      // Stage 1
      threadsWarmedUp.set(0);

      int numThreads = 1;
      while (numThreads <= bmConf.getSlaveNumThreads()) {
        if (numThreads > 128)
          throw new IllegalStateException("Attempting to create too many threads: " + numThreads);

        threadsWarmedUp.set(0);
        LOG.info("Creating " + numThreads + " workers now...");
        for (int i = 0; i < numThreads; i++) {
          Callable<Object> worker = new BaseWarmUp(1, bmConf,
                  "Warming up. Stage0: Warming up clients. ", numThreads);
          workers.add(worker);
        }

        executor.invokeAll(workers); // blocking call
        workers.clear();

        numThreads += 8;

        if (numThreads <= bmConf.getSlaveNumThreads())
          Thread.sleep(500); // Don't sleep after last iteration.
      }
      threadsWarmedUp.set(0);

      LOG.info("Finished initial warm-up. Moving onto Stage 1 of Warm-Up: Creating Parent Dirs.");

      for (int i = 0; i < bmConf.getSlaveNumThreads(); i++) {
        Callable<Object> worker = new BaseWarmUp(1, bmConf, "Warming up. Stage1: Creating Parent Dirs. ");
        workers.add(worker);
      }
      executor.invokeAll(workers); // blocking call
      workers.clear();

      LOG.info("Finished creating parent dirs. Moving onto Stage 2.");
      Thread.sleep(500);

      // Stage 2
      threadsWarmedUp.set(0);
      for (int i = 0; i < bmConf.getSlaveNumThreads(); i++) {
        Callable<Object> worker = new BaseWarmUp(bmConf.getFilesToCreateInWarmUpPhase() - 1,
                bmConf, "Warming up. Stage2: Creating files/dirs. ");
        workers.add(worker);
      }
      executor.invokeAll(workers); // blocking call
      LOG.debug("Finished. Warmup Phase. Created ("+bmConf.getSlaveNumThreads()+"*"+bmConf.getFilesToCreateInWarmUpPhase()+") = "+
              (bmConf.getSlaveNumThreads()*bmConf.getFilesToCreateInWarmUpPhase())+" files. ");
      workers.clear();
    }

    return new NamespaceWarmUp.Response();
  }

  @Override
  public String toString() {
    return "InterleavedBenchmark(startTime=" + startTime + ", duration=" + duration + ")";
  }

  @Override
  protected BenchmarkCommand.Response processCommandInternal(BenchmarkCommand.Request command) throws IOException, InterruptedException {
    BMConfiguration config = ((InterleavedBenchmarkCommand.Request) command).getConfig();

    duration = config.getInterleavedBmDuration();
    LOG.info("Starting " + command.getBenchMarkType() + " for duration " + duration + "\n\n\n");
    List<Callable<Object>> workers = new ArrayList<>();
    // Add limiter as a worker if supported
    WorkerRateLimiter workerLimiter = null;
    if (limiter instanceof WorkerRateLimiter) {
      workerLimiter = (WorkerRateLimiter) limiter;
      workers.add(workerLimiter);
    }
    LOG.debug("Creating workers...");
    for (int i = 0; i < bmConf.getSlaveNumThreads(); i++) {
      Callable<Object> worker = new Worker(config);
      workers.add(worker);
    }
    LOG.debug("Created " + bmConf.getSlaveNumThreads() + " workers...");
    startTime = System.currentTimeMillis();
    if (workerLimiter != null) {
      workerLimiter.setStart(startTime);
      workerLimiter.setDuration(duration);
      workerLimiter.setStat("completed", operationsCompleted);
    }

//    FailOverMonitor failOverTester = null;
//    List<String> failOverLog = null;
//    if (config.testFailover()) {
//      boolean canIKillNamenodes = InetAddress.getLocalHost().getHostName().compareTo(config.getNamenodeKillerHost()) == 0;
//      if (canIKillNamenodes) {
//        LOG.debug("Responsible for killing/restarting namenodes");
//      }
//      failOverTester = startFailoverTestDeamon(
//              config.getNameNodeRestartCommands(),
//              config.getFailOverTestDuration(),
//              config.getFailOverTestStartTime(),
//              config.getNameNodeRestartTimePeriod(),
//              canIKillNamenodes);
//    }

    Logger.resetTimer();

    LOG.debug("Invoking workers...");
    executor.invokeAll(workers); // blocking call
//    if (config.testFailover()) {
//      failOverTester.stop();
//      failOverLog = failOverTester.getFailoverLog();
//    }
    LOG.debug("Invoked workers...");

    long totalTime = System.currentTimeMillis() - startTime;

    System.out.println("Finished " + command.getBenchMarkType() + " in " + totalTime);
    LOG.debug("Finished " + command.getBenchMarkType() + " in " + totalTime);

    double speed = (operationsCompleted.get() / (double) totalTime) * 1000;

    int aliveNNsCount = 0;
    if (!dryrun) {
      aliveNNsCount = getAliveNNsCount();
    }
    InterleavedBenchmarkCommand.Response response =
            new InterleavedBenchmarkCommand.Response(totalTime, operationsCompleted.get(), operationsFailed.get(), speed, opsStats, avgLatency.getMean(), null, aliveNNsCount);
    return response;
  }

  public class Worker implements Callable<Object> {

    private FileSystem dfs;
    private FilePool filePool;
    private InterleavedMultiFaceCoin opCoin;
    private BMConfiguration config = null;
    private long lastMsg = System.currentTimeMillis();

    public Worker(BMConfiguration config) {
      this.config = config;
      this.lastMsg = System.currentTimeMillis();
    }

    @Override
    public Object call() throws Exception {
      LOG.debug("Worker has been called!");

      if (!dryrun) {
        dfs = DFSOperationsUtils.getDFSClient(false);
      }

      filePool = DFSOperationsUtils.getFilePool(bmConf.getBaseDir(),
              bmConf.getDirPerDir(), bmConf.getFilesPerDir(), bmConf.isFixedDepthTree(),
              bmConf.getTreeDepth(), bmConf.getFileSizeDistribution(),
              bmConf.getReadFilesFromDisk(), bmConf.getDiskNameSpacePath());
      
      opCoin = new InterleavedMultiFaceCoin(config.getInterleavedBmCreateFilesPercentage(),
              config.getInterleavedBmAppendFilePercentage(),
              config.getInterleavedBmReadFilesPercentage(),
              config.getInterleavedBmRenameFilesPercentage(),
              config.getInterleavedBmDeleteFilesPercentage(),
              config.getInterleavedBmLsFilePercentage(),
              config.getInterleavedBmLsDirPercentage(),
              config.getInterleavedBmChmodFilesPercentage(),
              config.getInterleavedBmChmodDirsPercentage(),
              config.getInterleavedBmMkdirPercentage(),
              config.getInterleavedBmSetReplicationPercentage(),
              config.getInterleavedBmGetFileInfoPercentage(),
              config.getInterleavedBmGetDirInfoPercentage(),
              config.getInterleavedBmFileChangeOwnerPercentage(),
              config.getInterleavedBmDirChangeOwnerPercentage()
      );
      while (true) {
        // Every 5000 operations, we'll print how many we've completed.
        // I do this instead of something like:
        // if (opsPerformed % 5000 == 0) { <print> }
        // so that I don't have to do the mod operation every loop.
        for (int i = 0; i < 5000; i++) {
          try {
            if ((System.currentTimeMillis() - startTime) > duration) {
              return null;
            }

            BenchmarkOperations op = opCoin.flip();

            if (LOG.isDebugEnabled())
              LOG.debug("Randomly generated " + op.name() + " operation!");

            // Wait for the limiter to allow the operation
            if (!limiter.checkRate()) {
              return null;
            }

            performOperation(op);

            if (!config.testFailover()) {
              log();
            }

          } catch (Exception e) {
            Logger.error(e);
          }
        }

        long opsCompleted = operationsCompleted.get();
        LOG.info("Completed " + opsCompleted + " operations.");
      }
    }


    private void log() throws IOException {

      // Send a log message once every five second.
      // The logger also tires to rate limit the log messages
      // using the canILog() methods. canILog method is synchronized
      // method. Calling it frequently can slightly impact the performance
      // It is better that each thread call the canILog() method only
      // once every five sec
      if ((System.currentTimeMillis() - lastMsg) > 5000) {
        lastMsg = System.currentTimeMillis();
        String message = "";
        if (Logger.canILog()) {
          message += DFSOperationsUtils.format(25, "Completed Ops: " + operationsCompleted + " ");
          message += DFSOperationsUtils.format(25, "Failed Ops: " + operationsFailed + " ");
          message += DFSOperationsUtils.format(25, "Speed: " + DFSOperationsUtils.round(speedPSec(operationsCompleted.get(), startTime)));
//          if (avgLatency.getN() > 0) {
//            message += DFSOperationsUtils.format(20, "Avg. Op Latency: " + avgLatency.getMean() + " ms");
//          }
//
//          SortedSet<BenchmarkOperations> sorted = new TreeSet<BenchmarkOperations>();
//          sorted.addAll(operationsStats.keySet());
//
//          for (BenchmarkOperations op : sorted) {
//            AtomicLong stat = operationsStats.get(op);
//            if (stat != null) {
//
//              double percent = DFSOperationsUtils.round(((double) stat.get() / operationsCompleted.get()) * 100);
//              String msg = op + ": [" + percent + "%] ";
//              message += DFSOperationsUtils.format(op.toString().length() + 14, msg);
//            }
//          }
          Logger.printMsg(message);
        }
      }
    }

    private void performOperation(BenchmarkOperations opType) {
      String path = BMOperationsUtils.getPath(opType, filePool);
      if (path != null) {
        boolean retVal = false;
        long opExeTime = 0;
        long opStartTime = System.nanoTime();
        try {
          if (dryrun) {
            LOG.debug("Performing simulated " + opType.name() + " on '" + path + "' now...");
            TimeUnit.MILLISECONDS.sleep(2);
          } else {
            LOG.debug("Performing " + opType.name() + " on '" + path + "' now...");
            BMOperationsUtils.performOp(dfs, opType, filePool, path, config.getReplicationFactor(),
                                        config.getAppendFileSize());
          }    

          opExeTime = System.nanoTime() - opStartTime;
          retVal = true;
        } catch (Exception e) {
          Logger.error(e);
        }
        updateStats(opType, retVal, new BMOpStats(opStartTime, opExeTime));
      } else {
        Logger.printMsg("Could not perform operation " + opType + ". Got Null from the file pool");
      }
    }

    private void updateStats(BenchmarkOperations opType, boolean success, BMOpStats stats) {
      AtomicLong stat = operationsStats.get(opType);
      if (stat == null) { // this should be synchronized to get accurate stats. However, this will slow down and these stats are just for log messages. Some inconsistencies are OK
        stat = new AtomicLong(0);
        operationsStats.put(opType, stat);
      }
      stat.incrementAndGet();

      if (success) {
        operationsCompleted.incrementAndGet();
        avgLatency.addValue(stats.OpDuration);
        if (bmConf.isPercentileEnabled()) {
          synchronized (opsStats) {
            ArrayList<BMOpStats> times = opsStats.get(opType);
            if (times == null) {
              times = new ArrayList<>();
              opsStats.put(opType, times);
            }
            times.add(stats);
          }
        }
      } else {
        operationsFailed.incrementAndGet();
      }

    }
  }

  private double speedPSec(long ops, long startTime) {
    long timePassed = (System.currentTimeMillis() - startTime);
    double opsPerMSec = (double) (ops) / (double) timePassed;
    return opsPerMSec * 1000;
  }

  FailOverMonitor startFailoverTestDeamon(List<List<String>> commands, long failoverTestDuration, long failoverTestStartTime, long namenodeRestartTP, boolean canIKillNamenodes) {
    FailOverMonitor worker = new FailOverMonitor(commands, failoverTestDuration, failoverTestStartTime, namenodeRestartTP, canIKillNamenodes);
    Thread t = new Thread(worker);
    t.start();
    return worker;
  }

  class FailOverMonitor implements Runnable {
    boolean stop;
    List<List<String>> allCommands;
    List<String> log;
    int tick = 0;
    long namenodeRestartTP;
    long failoverTestDuration;
    long failoverStartTime;
    boolean canIKillNNs;

    public FailOverMonitor(List<List<String>> commands,
                           long failoverTestDuration, long failoverTestStartTime,
                           long namenodeRestartTP, boolean canIKillNNs) {
      this.allCommands = commands;
      this.namenodeRestartTP = namenodeRestartTP;
      this.stop = false;
      this.failoverStartTime = failoverTestStartTime;
      this.failoverTestDuration = failoverTestDuration;
      this.log = new LinkedList<String>();
      this.canIKillNNs = canIKillNNs;
    }

    @Override
    public void run() {
      int rrIndex = 0;
      long previousSuccessfulOps = 0;
      final long startTime = System.currentTimeMillis();
      long lastFailOver = 0;
      while (!stop) {
        long speed = 0;
        if (previousSuccessfulOps == 0) {
          speed = operationsCompleted.get();
          previousSuccessfulOps = speed;
        } else {
          speed = (operationsCompleted.get() - previousSuccessfulOps);
          previousSuccessfulOps = operationsCompleted.get();
        }

        log.add(tick + " " + speed);
        LOG.debug("Time: " + tick + " sec. Speed: " + speed);


        if (canIKillNNs) {
          if (((System.currentTimeMillis() - startTime) > failoverStartTime)) {
            if ((startTime + failoverStartTime + failoverTestDuration) > System.currentTimeMillis()) {
              if (System.currentTimeMillis() - lastFailOver > namenodeRestartTP) {
                int index = (rrIndex++) % allCommands.size();
                List<String> nnCommands = allCommands.get(index);
                new Thread(new FailOverCommandExecutor(nnCommands)).start();
                lastFailOver = System.currentTimeMillis();
                log.add("#NameNode Restart Initiated");
                LOG.debug("#NameNode Restart Initiated");
              }
            }
          }
        }

        try {
          Thread.sleep(1000); //[s] this is not very precise. TODO subtract the time spent in the the while loop
          tick++;
        } catch (InterruptedException e) {
          e.printStackTrace();
        }
      }

    }

    public void stop() {
      stop = true;
    }

    public List<String> getFailoverLog() {
      return log;
    }
  }

  class FailOverCommandExecutor implements Runnable {
    List<String> commands;

    FailOverCommandExecutor(List<String> commands) {
      this.commands = commands;
    }

    @Override
    public void run() {
      for (String command : commands) {
        runCommand(command);
      }
    }

  }

  private void runCommand(String command) {
    try {
      LOG.debug("Going to execute command " + command);
      Process p = Runtime.getRuntime().exec(command);
      printErrors(p.getErrorStream());
      printErrors(p.getInputStream());
      p.waitFor();

      if (command.contains("kill")) { //[s] for some reason NameNode does not start soon after it is killed. TODO: fix it
        Thread.sleep(1000);
      }
    } catch (IOException e) {
      e.printStackTrace();
      LOG.error("Exception During Restarting the NameNode Command " + command + ":", e);
    } catch (InterruptedException e) {
      Logger.error(e);
      LOG.error("Exception During Restarting the NameNode Command " + command + ":", e);
    }
  }

  private void printErrors(InputStream errorStream) throws IOException {
    String line;
    BufferedReader input = new BufferedReader(new InputStreamReader(errorStream));
    while ((line = input.readLine()) != null) {
      Logger.printMsg(line);
    }
    input.close();
  }

}
