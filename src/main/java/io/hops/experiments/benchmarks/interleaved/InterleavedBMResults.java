/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.hops.experiments.benchmarks.interleaved;

import io.hops.experiments.benchmarks.common.BMResult;
import io.hops.experiments.benchmarks.common.BenchmarkOperations;
import io.hops.experiments.benchmarks.common.BenchmarkType;
import io.hops.experiments.utils.DFSOperationsUtils;

import java.text.DecimalFormat;
import java.util.Map;

/**
 *
 * @author salman
 */
public class InterleavedBMResults extends BMResult {
  private final double speed;
  private final double duration;
  private final double successfulOps;
  private final double failedOps;
  private final Map<BenchmarkOperations,double[][]> percentile;
  private final String workloadName;
  private final double avgOpLatency;

  private final int cacheHits;
  private final int cacheMisses;

  public InterleavedBMResults(int noOfExpectedNNs, int noOfActualAliveNNs, int noOfNDBDataNodes, String workloadName,
                              double speed, double duration, double successfulOps, double failedOps,
                              Map<BenchmarkOperations,double[][]> percentile, double avgOpLatency,
                              int cacheHits, int cacheMisses) {
    super(noOfExpectedNNs, noOfActualAliveNNs, noOfNDBDataNodes, BenchmarkType.INTERLEAVED);
    this.speed = speed;
    this.duration = duration;
    this.successfulOps = successfulOps;
    this.failedOps = failedOps;
    this.percentile = percentile;
    this.workloadName = workloadName;
    this.avgOpLatency = avgOpLatency;
    this.cacheHits = cacheHits;
    this.cacheMisses = cacheMisses;
  }

  public String getWorkloadName() {
    return workloadName;
  }

  public Map<BenchmarkOperations,double[][]> getPercentile(){
    return percentile;
  }
  
  public double getSpeed() {
    return speed;
  }

  public double getDuration() {
    return duration;
  }

  public double getSuccessfulOps() {
    return successfulOps;
  }

  public double getFailedOps() {
    return failedOps;
  }

  public double getAvgOpLatency() {
    return avgOpLatency;
  }

  public double getCacheHitRate() {
    if (cacheHits > 0 || cacheMisses > 0)
      return (double)cacheHits / ((double)cacheHits + (double)cacheMisses);

    return -1;
  }

  @Override
  public String toString() {
    return "Speed-/sec: " + DFSOperationsUtils.round(speed)
            + "; Successful-Ops: " + DFSOperationsUtils.round(successfulOps)
            + "; Failed-Ops: " + DFSOperationsUtils.round(failedOps)
            + "; Avg-Ops-Latency: " + DFSOperationsUtils.round(avgOpLatency) + " ns"
            + " (" + DFSOperationsUtils.round(avgOpLatency / 1e6) + " ms)"
            + "; Avg-Test-Duration-sec " + DFSOperationsUtils.round(duration)
            + "; No of Expected NNs : " + super.getNoOfExpectedAliveNNs()
            + "; No of Actual Alive NNs : " + super.getNoOfAcutallAliveNNs()
            + "; Cache Hits: " + cacheHits
            + "; Cache Misses: " + cacheMisses
            + "; Cache Hit Rate: " + getCacheHitRate();

  }

  public int getCacheHits() {
    return cacheHits;
  }

  public int getCacheMisses() {
    return cacheMisses;
  }
}
