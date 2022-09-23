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
package io.hops.experiments.controller;

//import io.hops.experiments.benchmarks.blockreporting.BlockReportBMResults;
//import io.hops.experiments.benchmarks.blockreporting.BlockReportingBenchmarkCommand;
//import io.hops.experiments.benchmarks.blockreporting.BlockReportingWarmUp;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import io.hops.experiments.benchmarks.common.BMResult;
import io.hops.experiments.benchmarks.common.BenchmarkOperations;
import io.hops.experiments.benchmarks.common.BenchmarkType;
import io.hops.experiments.benchmarks.common.commands.NamespaceWarmUp;
import io.hops.experiments.benchmarks.common.config.ConfigKeys;
import io.hops.experiments.benchmarks.common.config.BMConfiguration;
import io.hops.experiments.benchmarks.interleaved.InterleavedBMResults;
import io.hops.experiments.benchmarks.interleaved.InterleavedBenchmarkCommand;
import io.hops.experiments.benchmarks.rawthroughput.RawBMResults;
import io.hops.experiments.benchmarks.rawthroughput.RawBenchmarkCommand;
import io.hops.experiments.benchmarks.rawthroughput.RawBenchmarkCreateCommand;
import io.hops.experiments.controller.commands.Handshake;
import io.hops.experiments.controller.commands.KillFollower;
import io.hops.experiments.controller.commands.WarmUpCommand;
import io.hops.experiments.results.compiler.InterleavedBMResultsAggregator;
import io.hops.experiments.results.compiler.RawBMResultAggregator;
import org.apache.commons.io.FileUtils;

import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.sql.SQLException;
import java.util.*;

/**
 *
 * @author salman
 */
public class Master {
  public static final Log LOG = LogFactory.getLog(Master.class);

  Set<InetAddress> misbehavingSlaves = new HashSet<InetAddress>();
  Map<InetAddress, FollowerConnection> followerConnections = new HashMap<InetAddress, FollowerConnection>();
  List<BMResult> results = new ArrayList<BMResult>();
  BMConfiguration config;

  public static void main(String[] argv) throws Exception {
    System.out.println("Master has started running.");
    String configFilePath = "master.properties";
    if (argv.length == 1) {
      if (argv[0].compareToIgnoreCase("help") == 0) {
        BMConfiguration.printHelp();
        System.exit(0);
      } else {
        configFilePath = argv[0];
      }
    }
    new Master().start(configFilePath);
  }

  public void start(String configFilePath) throws Exception {
    try {
      printMasterLogMessages("*** Starting the master ***");
      config = new BMConfiguration(configFilePath);

      printMasterLogMessages("Removing existing result files...");
      removeExistingResultsFiles();

      printMasterLogMessages("Starting remote logger...");
      startRemoteLogger(config.getSlavesList().size());

      printMasterLogMessages("Connecting to followers...");
      connectFollowers();

      printMasterLogMessages("Performing handshake with followers...");
      handshakeWithFollowers(); // Let all the clients know show is the master

      printMasterLogMessages("Warming up followers...");
      warmUpFollowers();

      startCommander();

      printMasterLogMessages("Generating the results file...");
      generateResultsFile();

      printMasterLogMessages("Printing all results...");
      printAllResults();
    } catch (Exception e) {
      LOG.error("Exception encountered: ", e);
    } finally {
      printMasterLogMessages("Sending KILL command to all followers.");
      sendToAllFollowers(new KillFollower(), 0/*delay*/);
      System.exit(0);
    }
  }

  private void startRemoteLogger(int maxSlaves) {
    Logger.LogListener listener = new Logger.LogListener(config.getRemoteLoggingPort(),maxSlaves);
    Thread thread = new Thread(listener);
    thread.start();
    printMasterLogMessages("Remote logger started.");
  }

  private void startCommander() throws IOException, InterruptedException, ClassNotFoundException {
    if (config.getBenchMarkType() == BenchmarkType.RAW) {
      printMasterLogMessages("Starting RAW commander...");
      startRawCommander();
    } else if (config.getBenchMarkType() == BenchmarkType.INTERLEAVED) {
      printMasterLogMessages("Starting INTERLEAVED commander...");
      startInterleavedCommander();
    } else if (config.getBenchMarkType() == BenchmarkType.BR) {
      // startBlockReportingCommander();
      throw new IllegalStateException("Block Reporting benchmarking is not supported for serverless HopsFS.");
    } else {
      throw new IllegalStateException("Unsupported Benchmark ");
    }

  }

  private void startRawCommander() throws IOException, InterruptedException, ClassNotFoundException {
    if (config.getRawBmMkdirPhaseDuration() > 0) {
      printMasterLogMessages("Performing MKDIR phase for " + config.getRawBmMkdirPhaseDuration() + " ms.");
      startRawBenchmarkPhase(new RawBenchmarkCommand.Request(
              BenchmarkOperations.MKDIRS, config.getRawBmMkdirPhaseDuration()));
    }

    if (config.getRawBmFilesCreationPhaseDuration() > 0) {
      printMasterLogMessages("Performing CREATE phase for " + config.getRawBmFilesCreationPhaseDuration() + " ms.");
      startRawBenchmarkPhase(new RawBenchmarkCreateCommand.Request(
              config.getRawBmMaxFilesToCreate(),
              BenchmarkOperations.CREATE_FILE,
              config.getRawBmFilesCreationPhaseDuration()));
    }

    if (config.getRawBmAppendFilePhaseDuration() > 0) {
      printMasterLogMessages("Performing APPEND phase for " + config.getRawBmAppendFilePhaseDuration() + " ms.");
      startRawBenchmarkPhase(new RawBenchmarkCommand.Request(
              BenchmarkOperations.APPEND_FILE,
              config.getRawBmAppendFilePhaseDuration()));
    }

    if (config.getRawBmReadFilesPhaseDuration() > 0) {
      printMasterLogMessages("Performing READ phase for " + config.getRawBmReadFilesPhaseDuration() + " ms.");
      startRawBenchmarkPhase(new RawBenchmarkCommand.Request(
              BenchmarkOperations.READ_FILE,
              config.getRawBmReadFilesPhaseDuration()));
    }

    if (config.getRawBmLsFilePhaseDuration() > 0) {
      printMasterLogMessages("Performing LS FILE phase for " + config.getRawBmLsFilePhaseDuration() + " ms.");
      startRawBenchmarkPhase(new RawBenchmarkCommand.Request(
              BenchmarkOperations.LS_FILE,
              config.getRawBmLsFilePhaseDuration()));
    }

    if (config.getRawBmLsDirPhaseDuration() > 0) {
      printMasterLogMessages("Performing LS DIR phase for " + config.getRawBmLsDirPhaseDuration() + " ms.");
      startRawBenchmarkPhase(new RawBenchmarkCommand.Request(
              BenchmarkOperations.LS_DIR,
              config.getRawBmLsDirPhaseDuration()));
    }

    if (config.getRawBmChmodFilesPhaseDuration() > 0) {
      printMasterLogMessages("Performing CHMOD FILES phase for " + config.getRawBmChmodFilesPhaseDuration() + " ms.");
      startRawBenchmarkPhase(new RawBenchmarkCommand.Request(
              BenchmarkOperations.CHMOD_FILE,
              config.getRawBmChmodFilesPhaseDuration()));
    }

    if (config.getRawBmChmodDirsPhaseDuration() > 0) {
      printMasterLogMessages("Performing CHMOD DIRS phase for " + config.getRawBmChmodDirsPhaseDuration() + " ms.");
      startRawBenchmarkPhase(new RawBenchmarkCommand.Request(
              BenchmarkOperations.CHMOD_DIR,
              config.getRawBmChmodDirsPhaseDuration()));
    }

    if (config.getRawBmSetReplicationPhaseDuration() > 0) {
      printMasterLogMessages("Performing SET REPLICATION phase for " + config.getRawBmSetReplicationPhaseDuration() + " ms.");
      startRawBenchmarkPhase(new RawBenchmarkCommand.Request(
              BenchmarkOperations.SET_REPLICATION,
              config.getRawBmSetReplicationPhaseDuration()));
    }

    if (config.getRawBmGetFileInfoPhaseDuration() > 0) {
      printMasterLogMessages("Performing GET FILE INFO phase for " + config.getRawBmGetFileInfoPhaseDuration() + " ms.");
      startRawBenchmarkPhase(new RawBenchmarkCommand.Request(
              BenchmarkOperations.FILE_INFO,
              config.getRawBmGetFileInfoPhaseDuration()));
    }

    if (config.getRawBmGetDirInfoPhaseDuration() > 0) {
      printMasterLogMessages("Performing GET DIR INFO phase for " + config.getRawBmGetDirInfoPhaseDuration() + " ms.");
      startRawBenchmarkPhase(new RawBenchmarkCommand.Request(
              BenchmarkOperations.DIR_INFO,
              config.getRawBmGetDirInfoPhaseDuration()));
    }


    if (config.getRawFileChangeUserPhaseDuration() > 0) {
      printMasterLogMessages("Performing FILE CHANGE USER phase for " + config.getRawFileChangeUserPhaseDuration() + " ms.");
      startRawBenchmarkPhase(new RawBenchmarkCommand.Request(
              BenchmarkOperations.CHOWN_FILE,
              config.getRawFileChangeUserPhaseDuration()));
    }


    if (config.getRawDirChangeUserPhaseDuration() > 0) {
      printMasterLogMessages("Performing DIR CHANGE USER phase for " + config.getRawDirChangeUserPhaseDuration() + " ms.");
      startRawBenchmarkPhase(new RawBenchmarkCommand.Request(
              BenchmarkOperations.CHOWN_DIR,
              config.getRawDirChangeUserPhaseDuration()));
    }


    if (config.getRawBmRenameFilesPhaseDuration() > 0) {
      printMasterLogMessages("Performing RENAME FILES phase for " + config.getRawBmRenameFilesPhaseDuration() + " ms.");
      startRawBenchmarkPhase(new RawBenchmarkCommand.Request(
              BenchmarkOperations.RENAME_FILE,
              config.getRawBmRenameFilesPhaseDuration()));
    }

    if (config.getRawBmDeleteFilesPhaseDuration() > 0) {
      printMasterLogMessages("Performing DELETE FILES phase for " + config.getRawBmDeleteFilesPhaseDuration() + " ms.");
      startRawBenchmarkPhase(new RawBenchmarkCommand.Request(
              BenchmarkOperations.DELETE_FILE,
              config.getRawBmDeleteFilesPhaseDuration()));
    }
  }

//  private void startBlockReportingCommander() throws IOException, ClassNotFoundException {
//    System.out.println("Starting BlockReporting Benchmark ...");
//    prompt();
//    BlockReportingBenchmarkCommand.Request request = new BlockReportingBenchmarkCommand.Request();
//
//    sendToAllSlaves(request, 0/*delay*/);
//
//    Collection<Object> responses = receiveFromAllSlaves(Integer.MAX_VALUE);
//    DescriptiveStatistics successfulOps = new DescriptiveStatistics();
//    DescriptiveStatistics failedOps = new DescriptiveStatistics();
//    DescriptiveStatistics speed = new DescriptiveStatistics();
//    DescriptiveStatistics avgTimePerReport = new DescriptiveStatistics();
//    DescriptiveStatistics avgTimeTogetANewNameNode = new DescriptiveStatistics();
//    DescriptiveStatistics noOfNNs = new DescriptiveStatistics();
//
//    for (Object obj : responses) {
//      if (!(obj instanceof BlockReportingBenchmarkCommand.Response)) {
//        throw new IllegalStateException("Wrong response received from the client");
//      } else {
//        BlockReportingBenchmarkCommand.Response response = (BlockReportingBenchmarkCommand.Response) obj;
//        successfulOps.addValue(response.getSuccessfulOps());
//        failedOps.addValue(response.getFailedOps());
//        speed.addValue(response.getSpeed());
//        avgTimePerReport.addValue(response.getAvgTimePerReport());
//        avgTimeTogetANewNameNode.addValue(response.getAvgTimeTogetNewNameNode());
//        noOfNNs.addValue(response.getNnCount());
//      }
//    }
//
//    BlockReportBMResults result = new BlockReportBMResults(config.getNamenodeCount(),
//            (int)Math.floor(noOfNNs.getMean()),
//            config.getNdbNodesCount(),
//            speed.getSum(), successfulOps.getSum(),
//            failedOps.getSum(), avgTimePerReport.getMean(), avgTimeTogetANewNameNode.getMean());
//
//    printMasterResultMessages(result);
//  }

  private void startInterleavedCommander() throws IOException, ClassNotFoundException, InterruptedException {
    printMasterLogMessages("Starting Interleaved Benchmark ...");
    prompt();

    // Monitor for an additional twenty seconds beyond the duration of the benchmark.
    long interleavedBenchmarkDuration = config.getInterleavedBmDuration() + (1000 * 20);
    double interval = config.getNameNodeMonitorInterval();

    // Start Python script to monitor the NNs.
    String command = "python3.7 ~/home/ben/repos/hammer-bench/monitor_nns.py -d " + interleavedBenchmarkDuration +
            " -i " + interval;
    Process pythonMonitoringProcess = Runtime.getRuntime().exec(command);

    InterleavedBenchmarkCommand.Request request =
            new InterleavedBenchmarkCommand.Request(config);
    sendToAllFollowers(request, 0/*delay*/);

    Thread.sleep(interleavedBenchmarkDuration);
    Collection<Object> responses = receiveFromAllSlaves(120 * 1000 /*sec wait*/);
    InterleavedBMResults result = InterleavedBMResultsAggregator.processInterleavedResults(responses, config);
    printMasterResultMessages(result);
  }

  private void handshakeWithFollowers() throws IOException, ClassNotFoundException {
    //send request
    printMasterLogMessages("Starting handshake protocol");
    prompt();
    sendHandshakeToAllSlaves(new Handshake.Request(config));
    Collection<Object> allResponses = receiveFromAllSlaves(120 * 1000 /*sec wait*/);

    for (Object response : allResponses) {
      if (!(response instanceof Handshake.Response)) {
        throw new IllegalStateException("Follower responded with something other than handshake response");
      }
    }
    printMasterLogMessages("Handshake with all followers completed");
  }

  private void warmUpFollowers()
          throws IOException, ClassNotFoundException, SQLException {
    prompt();
    WarmUpCommand.Request warmUpCommand = null;
    if (config.getBenchMarkType() == BenchmarkType.INTERLEAVED
            || config.getBenchMarkType() == BenchmarkType.RAW) {
      warmUpCommand = new NamespaceWarmUp.Request(config.getBenchMarkType(), config.getFilesToCreateInWarmUpPhase(), config.getReplicationFactor(),
              config.getFileSizeDistribution(), config.getAppendFileSize(),
              config.getBaseDir(), config.getReadFilesFromDisk(), config.getDiskNameSpacePath());
    } else if (config.getBenchMarkType() == BenchmarkType.BR) {
      // warmUpCommand = new BlockReportingWarmUp.Request(config);
      throw new UnsupportedOperationException("Block Reporting benchmarking is not supported for serverless HopsFS.");
    } else {
      throw new UnsupportedOperationException("Wrong Benchmark type for warm-up: " + config.getBenchMarkType());
    }

    printMasterLogMessages("Issuing warm-up request to followers: " + warmUpCommand);
    sendToAllFollowers(warmUpCommand, config.getSlaveWarmUpDelay()/*delay*/);

    Collection<Object> allResponses = receiveFromAllSlaves(config.getWarmUpPhaseWaitTime());

    for (Object response : allResponses) {
      if (!(response instanceof WarmUpCommand.Response)) {
        throw new IllegalStateException("Follower responded with something other than handshake response");
      }
    }
    printMasterLogMessages("All followers warmed Up");
  }

  public void startRawBenchmarkPhase(RawBenchmarkCommand.Request request) throws IOException, InterruptedException, ClassNotFoundException {
    printMasterLogMessages("Starting " + request.getPhase() + " using "
            + config.getSlaveNumThreads() * config.getSlavesList().size()
            + " client(s). Time phase duration "
            + request.getDurationInMS() / (double) (1000 * 60) + " mins");
    prompt();

    sendToAllFollowers(request,0/*delay*/);

    Collection<Object> responses =
            receiveFromAllSlaves((int) (request.getDurationInMS() + 120 * 1000)/*sec wait*/);

    RawBMResults result = RawBMResultAggregator.processSlaveResponses(responses, request, config);
    printMasterResultMessages(result);
  }

  private void connectFollowers() throws IOException {
    if (config != null) {
      List<InetAddress> followers = config.getSlavesList();
      for (InetAddress follower : followers) {
        printMasterLogMessages("Connecting to follower " + follower);
        try {
          FollowerConnection slaveConn = new FollowerConnection(follower, config.getSlaveListeningPort());
          followerConnections.put(follower, slaveConn);
        } catch (Exception e) {
          misbehavingSlaves.add(follower);
          printMasterLogMessages("*** ERROR  unable to connect " + follower);
        }
      }
      if (misbehavingSlaves.size() > config.getMaxSlavesFailureThreshold()) {
        printMasterLogMessages("*** Too many followers failed. Abort test. Failed Slaves Count "+misbehavingSlaves.size()+" Threshold: "+ config.getMaxSlavesFailureThreshold());
        System.exit(-1);
      }
    }
  }

  private void sendHandshakeToAllSlaves(Handshake.Request handshake) throws
          IOException {
    if (!followerConnections.isEmpty()) {
      int slaveId = 0;
      for (InetAddress follower : followerConnections.keySet()) {
        FollowerConnection conn = followerConnections.get(follower);
        handshake.setSlaveId(slaveId++);
        conn.sendToFollower(handshake);
      }
    }
  }

  private void sendToAllFollowers(Object obj, int delay) throws IOException {
    if (!followerConnections.isEmpty()) {
      for (InetAddress follower : followerConnections.keySet()) {
        FollowerConnection conn = followerConnections.get(follower);
        conn.sendToFollower(obj);
        try{
          Thread.sleep(delay);
        }catch(InterruptedException e){}
      }
    }
  }

  private Collection<Object> receiveFromAllSlaves(int timeout) throws ClassNotFoundException, UnknownHostException, IOException {
    Map<InetAddress, Object> responses = new HashMap<InetAddress, Object>();
    if (!followerConnections.isEmpty()) {
      for (InetAddress follower : followerConnections.keySet()) {
        FollowerConnection conn = followerConnections.get(follower);
        Object obj = conn.recvFromSlave(timeout);
        if (obj != null) {
          responses.put(follower, obj);
        }
      }
    }
    return responses.values();
  }

  private void prompt() throws IOException {
    if (!config.isSkipAllPrompt()) {
      printMasterLogMessages("Press Enter to start ");
      System.in.read();
    }
  }

  private void printMasterLogMessages(String msg) {
    redColoredText(msg);
  }

  private void printMasterResultMessages(BMResult result) throws FileNotFoundException, IOException {
    blueColoredText(result.toString());
    results.add(result);
  }
  
  private void removeExistingResultsFiles() throws IOException{
    File dir = new File(config.getResultsDir());
    if(dir.exists()){
      printMasterLogMessages("Existing results directory exists -- removing it now...");
       FileUtils.deleteDirectory(dir);
    }
    dir.mkdirs();
  }

  private void generateResultsFile() throws FileNotFoundException, IOException {
    
    String filePath = config.getResultsDir();
    filePath += ConfigKeys.BINARY_RESULT_FILE_NAME;
    printMasterLogMessages("Writing results to "+filePath);
    FileOutputStream fout = new FileOutputStream(filePath);
    ObjectOutputStream oos = new ObjectOutputStream(fout);
    for (BMResult result : results) {
      oos.writeObject(result);
    }
    oos.close();
    
    
    filePath = config.getResultsDir();
    filePath += ConfigKeys.TEXT_RESULT_FILE_NAME;
    printMasterLogMessages("Writing results to "+filePath);
    FileWriter out = new FileWriter(filePath, false);
    for (BMResult result : results) {
      out.write(result.toString() + "\n");
    }
    out.close();

    if(config.getBenchMarkType() == BenchmarkType.RAW && config.isPercentileEnabled()){
      for (BMResult result : results) {
        RawBMResults rawResult = (RawBMResults) result;
        filePath = config.getResultsDir();
        filePath += rawResult.getOperationType()+".csv";
        out = new FileWriter(filePath, false);
        for(long l : rawResult.getLatencies()){
          out.write(((double)l/1000000.0)+"\n");
        }
        out.close();
      }
    }
  }

  private void redColoredText(String msg) {
    System.out.println((char) 27 + "[31m" + msg);
    System.out.println((char) 27 + "[0m");
  }

  public static void blueColoredText(String msg) {
    System.out.println((char) 27 + "[36m" + msg);
    System.out.println((char) 27 + "[0m");
  }

  private void printAllResults() throws FileNotFoundException, IOException {
    System.out.println("\n\n\n");
    System.out.println("************************ All Results ************************");
    System.out.println("\n\n\n");
    
    String filePath = config.getResultsDir();
    if(!filePath.endsWith("/")){
      filePath += "/";
    }
    filePath += ConfigKeys.TEXT_RESULT_FILE_NAME;
    
    printMasterLogMessages("Reading results from "+filePath);
    BufferedReader br = new BufferedReader(new FileReader(filePath));
    try {

      String line = br.readLine();

      while (line != null) {
        blueColoredText(line);
        line = br.readLine();
      }
    } finally {
      br.close();
    }
    System.out.println("\n\n\n");
  }

  public class FollowerConnection {

    private final Socket socket;

    FollowerConnection(InetAddress followerInetAddress, int followerPort) throws IOException {
      socket = new Socket(followerInetAddress, followerPort);
    }

    public void sendToFollower(Object obj) {

      if (isSlaveHealthy(socket.getInetAddress())) {
        try {
          printMasterLogMessages("SEND " + obj.getClass().getCanonicalName() + " to " + socket.getInetAddress());
          socket.setSendBufferSize(ConfigKeys.BUFFER_SIZE);
          ObjectOutputStream sendToFollower = new ObjectOutputStream(socket.getOutputStream());
          sendToFollower.writeObject(obj);
        } catch (Exception e) {
          handleMisBehavingSlave(socket.getInetAddress());
        }
      } else {
        printMasterLogMessages("*** ERROR send request to " + socket.getInetAddress() + " is ignored ");
      }
    }

    public Object recvFromSlave(int timeout) {
      if (isSlaveHealthy(socket.getInetAddress())) {
        try {
          socket.setSoTimeout(timeout);
          socket.setReceiveBufferSize(ConfigKeys.BUFFER_SIZE);
          ObjectInputStream recvFromSlave = new ObjectInputStream(socket.getInputStream());
          Object obj = recvFromSlave.readObject();
          printMasterLogMessages("RECVD " + obj.getClass().getCanonicalName() + " from " + socket.getInetAddress());
          socket.setSoTimeout(Integer.MAX_VALUE);
          return obj;
        } catch (Exception e) {
          handleMisBehavingSlave(socket.getInetAddress());
          return null;
        }
      } else {
        printMasterLogMessages("*** ERROR recv request from " + socket.getInetAddress() + " is ignored ");
        return null;
      }
    }

    private void handleMisBehavingSlave(InetAddress follower) {
      misbehavingSlaves.add(follower);
      printMasterLogMessages("*** Slaved Failed. " + follower);
      if (misbehavingSlaves.size() > config.getMaxSlavesFailureThreshold()) {
        printMasterLogMessages("*** HARD ERROR. Too many followers failed. ABORT Test.");
        System.exit(-1);
      }
    }

    private boolean isSlaveHealthy(InetAddress follower) {
      if (!misbehavingSlaves.contains(follower)) {
        return true;
      } else {
        return false;
      }
    }
  }
}
