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
package io.hops.experiments.benchmarks.blockreporting.nn;


import org.apache.hadoop.hdfs.protocol.ClientProtocol;
import org.apache.hadoop.hdfs.server.protocol.DatanodeProtocol;
import org.apache.hadoop.hdfs.server.protocol.DatanodeRegistration;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.Map;

public interface BlockReportingNameNodeSelector {

  public static interface BlockReportingNameNodeHandle{
    ClientProtocol getRPCHandle();
    DatanodeProtocol getDataNodeRPC();
    String  getHostName();
  }

  BlockReportingNameNodeHandle getNextNameNodeRPCS() throws Exception;

  BlockReportingNameNodeHandle getLeader() throws IOException;

  DatanodeProtocol getNameNodeToReportTo(long blocksCount, DatanodeRegistration dnReg,
                                         boolean ignoreBRLoadBalancer/*hopsfs only*/) throws
                                         Exception;

  List<BlockReportingNameNodeHandle> getNameNodes() throws Exception;

  public void closeAllHandles();

  Map<String, Integer> getReportsStats();
}
