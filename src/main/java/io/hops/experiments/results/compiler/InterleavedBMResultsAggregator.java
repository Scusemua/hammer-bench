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
package io.hops.experiments.results.compiler;

import io.hops.experiments.benchmarks.common.BMOpStats;
import io.hops.experiments.benchmarks.common.BMResult;
import io.hops.experiments.benchmarks.common.BenchmarkOperations;
import io.hops.experiments.benchmarks.interleaved.InterleavedBMResults;
import io.hops.experiments.benchmarks.interleaved.InterleavedBenchmarkCommand;
import io.hops.experiments.benchmarks.common.config.ConfigKeys;
import io.hops.experiments.benchmarks.common.config.BMConfiguration;

import java.io.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import io.hops.experiments.controller.Master;
import io.hops.experiments.utils.DFSOperationsUtils;
import io.hops.metrics.OperationPerformed;
import io.hops.metrics.TransactionEvent;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.apache.hadoop.hdfs.DistributedFileSystem;

/**
 *
 * @author salman
 */
public class InterleavedBMResultsAggregator extends Aggregator {
  public static final Log LOG = LogFactory.getLog(InterleavedBMResultsAggregator.class);

  private Map<String /*workload name*/, Map<Integer/*NN Count*/, InterleavedAggregate/*aggregates*/>> allWorkloadsResults =
          new HashMap<String, Map<Integer, InterleavedAggregate>>();

  @Override
  public void processRecord(BMResult result) {
    //LOG.info(result);
    InterleavedBMResults ilResult = (InterleavedBMResults) result;
    if(ilResult.getSpeed()<=0){
      return;
    }

    String workloadName = ilResult.getWorkloadName();
    Map<Integer, InterleavedAggregate> workloadResults = allWorkloadsResults.get(workloadName);
    if (workloadResults == null) {
      workloadResults = new HashMap<Integer, InterleavedAggregate>();
      allWorkloadsResults.put(workloadName, workloadResults);
    }

    InterleavedAggregate agg = workloadResults.get(ilResult.getNoOfExpectedAliveNNs());

    if (agg == null) {
      agg = new InterleavedAggregate();
      workloadResults.put(ilResult.getNoOfExpectedAliveNNs(), agg);
    }

    agg.addSpeed(ilResult.getSpeed());
    agg.addFailedOps(ilResult.getFailedOps());
    agg.addSucessfulOps(ilResult.getSuccessfulOps());
    agg.addRunDuration(ilResult.getDuration());
  }

  @Override
  public boolean validate(BMResult result) {
    InterleavedBMResults ilResult = (InterleavedBMResults) result;
    if (ilResult.getSpeed() > 0 && ilResult.getNoOfAcutallAliveNNs() == ilResult.getNoOfExpectedAliveNNs()) {
      return true;
    }
    System.err.println("Inconsistent/Wrong results.  Speed: "+ilResult.getSpeed()+
        " Expected NNs: "+ilResult.getNoOfExpectedAliveNNs()+" Actual NNs: "+ilResult.getNoOfAcutallAliveNNs());
    return false;
  }

  public Map<String, Map<Integer, InterleavedAggregate>> getResults() {
    return allWorkloadsResults;
  }

  public static void combineResults(Map<String, Map<Integer, InterleavedAggregate>> hdfsAllWorkLoads, Map<String, Map<Integer, InterleavedAggregate>> hopsfsAllWorkloas, String outpuFolder) throws IOException {

    String plot = "set terminal postscript eps enhanced color font \"Helvetica,18\"  #monochrome\n";
    plot +=  "set output '| ps2pdf - interleaved.pdf'\n";
    plot +=  "#set size 1,0.75 \n ";
    plot +=  "set ylabel \"ops/sec\" \n";
    plot +=  "set xlabel \"Number of Namenodes\" \n";
    plot +=  "set format y \"%.0s%c\"\n";
    plot +=  "plot ";

    for (String workload : hopsfsAllWorkloas.keySet()) {
      Map<Integer, InterleavedAggregate> hopsWorkloadResult = hopsfsAllWorkloas.get(workload);
      Map<Integer, InterleavedAggregate> hdfsWorkloadResult = hdfsAllWorkLoads.get(workload);

      if (hopsWorkloadResult == null) {
        LOG.info("No data for hopsfs for workload " + workload);
        return;
      }

      double hdfsVal = 0;
      if (hdfsWorkloadResult != null) {
        if (hdfsWorkloadResult.keySet().size() > 1) {
          LOG.info("NN count for HDFS cannot be greater than 1");
          return;
        }

        if (hdfsWorkloadResult.keySet().size() == 1) {
          hdfsVal = ((InterleavedAggregate) hdfsWorkloadResult.values().toArray()[0]).getSpeed();
        }
      }

      if (hopsWorkloadResult.keySet().size() <= 0) {
        return;
      }

      plot +=  " '" + workload + "-interleaved.dat' using 2:xticlabels(1) not with lines, '' using 0:2:3:4:xticlabels(1) title \"HopsFS-" + workload + "\" with errorbars, " + hdfsVal + " title \"HDFS-" + workload + "\" \n";
      String data = "";
      SortedSet<Integer> sorted = new TreeSet<Integer>(); // Sort my number of NN
      sorted.addAll(hopsWorkloadResult.keySet());
      for (Integer nn : sorted) {
        InterleavedAggregate agg = hopsWorkloadResult.get(nn);
        data += CompileResults.format(nn + "") + CompileResults.format(agg.getSpeed() + "")
                + CompileResults.format(agg.getMinSpeed() + "") + CompileResults.format(agg.getMaxSpeed() + "")
                + "\n";
      }
      LOG.info(data);
      CompileResults.writeToFile(outpuFolder + "/" + workload + "-interleaved.dat", data, false);
    }

    LOG.info(plot);
    CompileResults.writeToFile(outpuFolder + "/interleaved.gnuplot", plot, false);
  }

  public static InterleavedBMResults processInterleavedResults(Collection<Object> responses, BMConfiguration args)
          throws IOException {
    Map<BenchmarkOperations, double[][]> allOpsPercentiles = new HashMap<>();
    LOG.info("Processing the results ");
    DescriptiveStatistics successfulOps = new DescriptiveStatistics();
    DescriptiveStatistics failedOps = new DescriptiveStatistics();
    DescriptiveStatistics speed = new DescriptiveStatistics();
    DescriptiveStatistics duration = new DescriptiveStatistics();
    DescriptiveStatistics opsLatency = new DescriptiveStatistics();
    DescriptiveStatistics noOfNNs = new DescriptiveStatistics();
    for (Object obj : responses) {
      if (!(obj instanceof InterleavedBenchmarkCommand.Response)) {
        throw new IllegalStateException("Wrong response received from the client");
      } else {
        InterleavedBenchmarkCommand.Response response = (InterleavedBenchmarkCommand.Response) obj;
        successfulOps.addValue(response.getTotalSuccessfulOps());
        failedOps.addValue(response.getTotalFailedOps());
        speed.addValue(response.getOpsPerSec());
        duration.addValue(response.getRunTime());
        opsLatency.addValue(response.getAvgOpLatency());
        noOfNNs.addValue(response.getNnCount());
      }
    }

    LOG.info("Deserialized all responses.");

    int cacheHits = -1;
    int cacheMisses = -1;
    LOG.info("Getting HDFS client");
    //DistributedFileSystem hdfs = DFSOperationsUtils.getDFSClient(false);
    LOG.info("Got HDFS client. Clearing metrics.");
    //hdfs.clearStatistics(true, true, true);

    LOG.info("Cleared metrics.");

    int counter = 0;
    for (Object obj : responses) {
      if (!(obj instanceof InterleavedBenchmarkCommand.Response)) {
        throw new IllegalStateException("Wrong response received from the client");
      } else {
        LOG.info("Processing response " + counter + "/" + responses.size());
        InterleavedBenchmarkCommand.Response response = (InterleavedBenchmarkCommand.Response) obj;

//        LOG.info("Calculating cache hits/misses for response " + counter);
//        for (OperationPerformed operationPerformed : response.getOperationPerformedInstances()) {
//          cacheHits += operationPerformed.getMetadataCacheHits();
//          cacheMisses += operationPerformed.getMetadataCacheMisses();
//        }

        LOG.info("Extracting ops performed");
        //hdfs.addOperationPerformeds(response.getOperationPerformedInstances());
        LOG.info("Extracting latencies");
        //hdfs.addLatencies(response.getTcpLatencies().getValues(), response.getHttpLatencies().getValues());
        LOG.info("Extracting tx events");
        //hdfs.mergeTransactionEvents(response.getTxEvents(), true);
        LOG.info("Processed response " + counter + "/" + responses.size());

        counter += 1;
      }
    }

    LOG.info("Printing operations performed now");
    //printOperationsPerformed(hdfs, args.getResultsDir() + "_OperationsPerformed");
    //DFSOperationsUtils.returnHdfsClient(hdfs);

    //write the response objects to files. 
    //these files are processed by CalculatePercentiles.java
    int responseCount = 0;
    for (Object obj : responses) {
      if (!(obj instanceof InterleavedBenchmarkCommand.Response)) {
        throw new IllegalStateException("Wrong response received from the client");
      } else {
        String filePath = args.getResultsDir();
        InterleavedBenchmarkCommand.Response response = (InterleavedBenchmarkCommand.Response) obj;
        filePath += "ResponseRawData" + responseCount++ + ConfigKeys.RAW_RESPONSE_FILE_EXT;
        LOG.info("Writing raw results to " + filePath);
        FileOutputStream fout = new FileOutputStream(filePath);
        ObjectOutputStream oos = new ObjectOutputStream(fout);
        oos.writeObject(response);
        oos.close();

        LOG.info("Writing CSV results ");
        HashMap<BenchmarkOperations, ArrayList<BMOpStats>> stats = response.getOpsStats();
        for (BenchmarkOperations op : stats.keySet()) {
          filePath = args.getResultsDir();
          filePath += op.toString() + ".txt";
          FileWriter out = new FileWriter(filePath, true);
          for (BMOpStats stat : stats.get(op)) {
            out.write(String.valueOf(stat.OpStart));
            out.write(",");
            out.write(String.valueOf(stat.OpDuration));
            out.write(",");
            out.write(stat.Path);
            out.write("\n");
          }
          out.close();

        }
      }
    }

    InterleavedBMResults result = new InterleavedBMResults(args.getNamenodeCount(),
            (int)Math.floor(noOfNNs.getMean()),
            args.getNdbNodesCount(), args.getInterleavedBmWorkloadName(),
            (successfulOps.getSum() / ((duration.getMean() / 1000))), (duration.getMean() / 1000),
            (successfulOps.getSum()), (failedOps.getSum()), allOpsPercentiles, opsLatency.getMean(),
            cacheHits, cacheMisses);

    LOG.info("Returning result");
    return result;
  }

  public static void printOperationsPerformed(DistributedFileSystem hdfs, String filePath) throws IOException {
    hdfs.printOperationsPerformed();

    ConcurrentHashMap<String, List<TransactionEvent>> transactionEvents = hdfs.getTransactionEvents();
    ArrayList<TransactionEvent> allTransactionEvents = new ArrayList<>();

    for (Map.Entry<String, List<TransactionEvent>> entry : transactionEvents.entrySet()) {
      allTransactionEvents.addAll(entry.getValue());
    }

    System.out.println("====================== Transaction Events ====================================================================================");

    System.out.println("\n-- SUMS ----------------------------------------------------------------------------------------------------------------------");
    System.out.println(TransactionEvent.getMetricsHeader());
    System.out.println(TransactionEvent.getMetricsString(TransactionEvent.getSums(allTransactionEvents)));

    System.out.println("\n-- AVERAGES ------------------------------------------------------------------------------------------------------------------");
    System.out.println(TransactionEvent.getMetricsHeader());
    System.out.println(TransactionEvent.getMetricsString(TransactionEvent.getAverages(allTransactionEvents)));

    System.out.println("\n==============================================================================================================================");

    BufferedWriter opsPerformedWriter = new BufferedWriter(new FileWriter(filePath + ".csv"));
    List<OperationPerformed> operationsPerformed = hdfs.getOperationsPerformed();

    opsPerformedWriter.write(OperationPerformed.getHeader());
    opsPerformedWriter.newLine();
    for (OperationPerformed op : operationsPerformed) {
      op.write(opsPerformedWriter);
    }
    opsPerformedWriter.close();
  }
}
