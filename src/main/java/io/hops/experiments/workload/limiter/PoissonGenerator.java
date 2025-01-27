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
package io.hops.experiments.workload.limiter;

import io.hops.experiments.benchmarks.common.config.BMConfiguration;

/**
 *
 * @author Tianium
 */
public class PoissonGenerator extends DistributionGenerator {
  protected double lambda;

  public PoissonGenerator(BMConfiguration bmConf) {
    super(bmConf);
    this.lambda = bmConf.getInterleavedBMIaTPoissonLambda();
  }

  @Override
  public double get() {
    double num = -1.0;
    double log1, log2;
    log1 = 0.0;
    log2 = -lambda;
    do {
      double p = getProbability();
      log1 += Math.log(p);
      num++;
    } while (log1 >= log2);
    return num;
  }
}
