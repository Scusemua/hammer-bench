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
package io.hops.experiments.benchmarks.blockreporting;

import com.google.common.collect.Lists;
import io.hops.experiments.benchmarks.blockreporting.nn.BlockReportingNameNodeSelector;
import io.hops.experiments.benchmarks.blockreporting.nn.NameNodeSelectorFactory;
import io.hops.experiments.benchmarks.common.BenchMarkFileSystemName;
import io.hops.experiments.controller.Logger;
import io.hops.experiments.utils.DFSOperationsUtils;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.hdfs.protocol.HdfsConstants;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static io.hops.experiments.benchmarks.blockreporting.nn.BlockReportingNameNodeSelector.BlockReportingNameNodeHandle;

public class TinyDatanodes {

  private final BlockReportingNameNodeSelector nameNodeSelector;
  private final String baseDir;
  private final int nrDatanodes;
  private final int blocksPerReport;
  private final int blocksPerFile;
  private final int filesPerDirectory;
  private final short replication;
  private final TinyDatanode[] datanodes;
  private final TinyDatanodesHelper helper;
  private final boolean ignoreBRLoadBalancing;
  private final int numBuckets;
  private final int blockSize;
  private final boolean skipCreation;
  private AtomicInteger allBlksCount = new AtomicInteger(0);

  public TinyDatanodes(Configuration conf, String baseDir,
                       int numOfDataNodes, int blocksPerReport,
                       int blocksPerFile, int filesPerDirectory,
                       int replication, int blockSize, int slaveId,
                       String databaseConnection,
                       BenchMarkFileSystemName fsName,
                       boolean ignoreBRLoadBalancing,
                       int numBuckets,
                       boolean skipCreation)
          throws IOException, Exception {
    this.baseDir = baseDir;
    this.blocksPerReport = blocksPerReport;
    this.blocksPerFile = blocksPerFile;
    this.filesPerDirectory = filesPerDirectory;
    this.replication = (short) replication;
    this.blockSize = blockSize;
    this.ignoreBRLoadBalancing = ignoreBRLoadBalancing;
    this.numBuckets = numBuckets;
    this.helper = new TinyDatanodesHelper(slaveId, databaseConnection);

    this.skipCreation = skipCreation;
    if(skipCreation){
      //read the number of datanodes from the stored file
      this.nrDatanodes = helper.getDNCountFromDisk();
    }else{
      this.nrDatanodes = numOfDataNodes;
    }

    this.datanodes = new TinyDatanode[nrDatanodes];

    nameNodeSelector = NameNodeSelectorFactory.getSelector(fsName, conf, FileSystem
            .getDefaultUri(conf));

    createDatanodes();
  }

  public void createDatanodes() throws Exception {
    String prevDNName = "";
    for (int idx = 0; idx < nrDatanodes; idx++) {
      System.out.println("register DN " + idx);
      datanodes[idx] = new TinyDatanode(nameNodeSelector,
               idx, ignoreBRLoadBalancing, numBuckets,
               blocksPerReport, blocksPerFile, 5 /*threds for creation of blks*/,
               baseDir, blockSize, filesPerDirectory,
               replication, helper,
               this);
      datanodes[idx].register(skipCreation);
      assert datanodes[idx].getXferAddr().compareTo(prevDNName)
              > 0 : "Data-nodes must be sorted lexicographically.";
      datanodes[idx].sendHeartbeat();
      prevDNName = datanodes[idx].getXferAddr();
    }

    helper.updateDatanodes(datanodes);

  }

  public void leaveSafeMode() throws IOException {
    BlockReportingNameNodeHandle leader = nameNodeSelector.getLeader();
    leader.getRPCHandle().setSafeMode(HdfsConstants.SafeModeAction.SAFEMODE_LEAVE, false);
  }

  public TinyDatanode[] getAllDatanodes(){
    return datanodes;
  }

  public void generateInput(boolean skipCreation, ExecutorService executor) throws Exception {
    // create data-nodes
    if (skipCreation) {
      helper.readDataNodesStateFromDisk(datanodes);
    } else {
      //load from disk
      createFiles(executor);
    }

    // prepare block reports
    for (int idx = 0; idx < nrDatanodes; idx++) {
      datanodes[idx].formBlockReport(skipCreation);
    }

    //save to disk
    helper.writeDataNodesStateToDisk(datanodes);
  }

  private void createFiles(ExecutorService executor) throws Exception {
    List writers = Lists.newArrayList();

    for (int idx = 0; idx < nrDatanodes; idx++) {
      writers.addAll(datanodes[idx].createWriterThreads());
    }

    System.out.println("Workers "+writers.size());
    executor  = Executors.newFixedThreadPool(writers.size());
    executor.invokeAll(writers);
  }

  long[] executeOp(int dnIdx)
          throws Exception {
    assert dnIdx < nrDatanodes : "Wrong dnIdx.";
    TinyDatanode dn = datanodes[dnIdx];
    return dn.blockReport();
  }

  void printStats() throws IOException {
    Logger.printMsg("Reports " + nameNodeSelector.getReportsStats().toString());
  }

  long lastCount=0;
  long startTime = 0;
  public void log() {
    if (Logger.canILog()) {
      if(startTime == 0){
        startTime = System.currentTimeMillis();
      }

      long max = blocksPerReport*nrDatanodes;
      double percent = ((double)allBlksCount.get() / (double)(max)) * 100.0;
      long speed = (allBlksCount.get() - lastCount)/5;
      lastCount = allBlksCount.get();

      double timePassed = System.currentTimeMillis()- startTime;
      double blksPerMs = (allBlksCount.get()/timePassed);
      double totalTimeRequired = max / blksPerMs;
      double totalTimeRequiredRemaining = totalTimeRequired - timePassed;

      long x = ((long)totalTimeRequiredRemaining/1000);
      long seconds = ((long) totalTimeRequiredRemaining / 1000) % 60;
      x /= 60;
      long minutes = x % 60;
      x /= 60;
      long hours = x % 24;
      String time = String.format("%02d:%02d:%02d", hours,minutes,seconds);

      Logger.printMsg("Warmup " + DFSOperationsUtils.round(percent) +
              "% completed. Speed "+speed+ " blks/sec. " +
              "ETA : "+time);

    }
  }

  public void incAllBlksCount(){
    allBlksCount.incrementAndGet();
  }

}
