/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package io.hops.experiments.benchmarks.common;

//import io.hops.experiments.benchmarks.blockreporting.BlockReportingBenchmark;
import io.hops.experiments.benchmarks.common.config.BMConfiguration;
import io.hops.experiments.benchmarks.interleaved.InterleavedBenchmark;
import io.hops.experiments.benchmarks.rawthroughput.RawBenchmark;
import io.hops.experiments.controller.Logger;
import io.hops.experiments.controller.commands.BenchmarkCommand;
import io.hops.experiments.controller.commands.WarmUpCommand;
import io.hops.experiments.utils.DFSOperationsUtils;
import io.hops.experiments.workload.generator.FilePool;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.conf.Configuration;

import java.io.IOException;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.hdfs.DistributedFileSystem;

public abstract class Benchmark {
  public static final Log LOG = LogFactory.getLog(Benchmark.class);
  protected final Configuration conf;
  protected final ExecutorService executor;
  protected AtomicInteger threadsWarmedUp = new AtomicInteger(0);
  protected final BMConfiguration bmConf;
  protected boolean dryrun = false;

  public Benchmark(Configuration conf, BMConfiguration bmConf) {
    this.conf = conf;
    this.bmConf = bmConf;
    this.executor = Executors.newFixedThreadPool(bmConf.getSlaveNumThreads());
  }

  protected abstract WarmUpCommand.Response warmUp(WarmUpCommand.Request warmUp)
          throws Exception;

  protected abstract BenchmarkCommand.Response processCommandInternal(BenchmarkCommand.Request command) throws Exception,
          InterruptedException;

  public final BenchmarkCommand.Response processCommand(BenchmarkCommand.Request command)
          throws Exception {
    if (command instanceof WarmUpCommand.Request) {
      return warmUp((WarmUpCommand.Request) command);
    }
    return processCommandInternal(command);
  }

  @Override
  public String toString() {
    return "Benchmark";
  }
  
  public static Benchmark getBenchmark(Configuration conf, BMConfiguration bmConf, int slaveID) {
    if (bmConf.getBenchMarkType() == BenchmarkType.RAW) {
      return new RawBenchmark(conf, bmConf);
    } else if (bmConf.getBenchMarkType() == BenchmarkType.INTERLEAVED) {
      return new InterleavedBenchmark(conf, bmConf);
    } else if (bmConf.getBenchMarkType() == BenchmarkType.BR) {
         throw new UnsupportedOperationException(
                 "Block Report benchmarking is not currently supported for serverless HopsFS.");
    } else {
      throw new UnsupportedOperationException("Unsupported Benchmark " + bmConf.getBenchMarkType());
    }
  }
  
  protected AtomicLong filesCreatedInWarmupPhase = new AtomicLong(0);
  protected class BaseWarmUp implements Callable<Object> {
    private DistributedFileSystem dfs;
    private FilePool filePool;
    private final int filesToCreate;
    private final String stage;
    private final BMConfiguration bmConf;

    public BaseWarmUp(int filesToCreate, BMConfiguration bmConf,
                       String stage) throws IOException {
      this.filesToCreate = filesToCreate;
      this.stage = stage;
      this.bmConf = bmConf;
      dryrun = bmConf.getBenchmarkDryrun();
    }

    public FilePool getFilePool() {
      return filePool;
    }

    @Override
    public Object call() throws Exception {
      LOG.debug("BaseWarmUp thread has started running.");
      try {
        if (!dryrun) {
          dfs = DFSOperationsUtils.getDFSClient(conf);
          dfs.setConsistencyProtocolEnabled(false); // No consistency protocol during warm-up.
          dfs.setServerlessFunctionLogLevel("DEBUG");
        }
        filePool = DFSOperationsUtils.getFilePool(conf,
                bmConf.getBaseDir(), bmConf.getDirPerDir(),
                bmConf.getFilesPerDir(), bmConf.isFixedDepthTree(),
                bmConf.getTreeDepth(), bmConf.getFileSizeDistribution(),
                bmConf.getReadFilesFromDisk(), bmConf.getDiskNameSpacePath());
      } catch (Exception ex) {
        LOG.error("Exception encountered while obtaining DFS client:", ex);
      }

      System.out.println("File pool: " + filePool.toString());
      LOG.debug("File pool: " + filePool.toString());
      String filePath = null;

      System.out.println("Attempting to create a total of " + filesToCreate + " file(s).");
      LOG.debug("Attempting to create a total of " + filesToCreate + " file(s).");
      for (int i = 0; i < filesToCreate; i++) {
        try {
          filePath = filePool.getFileToCreate();
          System.out.println("Creating file '" + filePath + "' now...");
          if (!dryrun) {
            DFSOperationsUtils
                    .createFile(dfs, filePath, bmConf.getReplicationFactor(), filePool);
            filePool.fileCreationSucceeded(filePath);
            DFSOperationsUtils.readFile(dfs, filePath);
          } else {
            filePool.fileCreationSucceeded(filePath);
          }
          filesCreatedInWarmupPhase.incrementAndGet();
          log();
        } catch (Exception e) {
          Logger.error(e);
        }
      }
      log();
      threadsWarmedUp.incrementAndGet();
      while(threadsWarmedUp.get() != bmConf.getSlaveNumThreads()){ // this is to ensure that all the threads in
        // the
        // executor service are started during the warmup phase
        Thread.sleep(100);
      }

      System.out.println("WarmedUp");
      return null;
    }

    private void log() {
      if (Logger.canILog()) {
        long totalFilesThatWillBeCreated = filesToCreate * bmConf.getSlaveNumThreads();
        double percent = (filesCreatedInWarmupPhase.doubleValue() / totalFilesThatWillBeCreated) * 100;
        Logger.printMsg(stage+" " + DFSOperationsUtils.round(percent) + "%");
      }
    }
  };

  protected int getAliveNNsCount() throws IOException {
    return 999;
//    FileSystem fs = DFSOperationsUtils.getDFSClient(conf);
//    int actualNNCount = 0;
//    try {
//      actualNNCount = 999; // DFSOperationsUtils.getActiveNameNodesCount(bmConf.getBenchMarkFileSystemName(), fs);
//    } catch (Exception e) {
//      Logger.error(e);
//    }
//    return actualNNCount;
  }
}
