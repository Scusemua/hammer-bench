#!/bin/bash
#
#   Licensed to the Apache Software Foundation (ASF) under one or more
#   contributor license agreements.  See the NOTICE file distributed with
#   this work for additional information regarding copyright ownership.
#   The ASF licenses this file to You under the Apache License, Version 2.0
#   (the "License"); you may not use this file except in compliance with
#   the License.  You may obtain a copy of the License at
#
#       http://www.apache.org/licenses/LICENSE-2.0
#
#   Unless required by applicable law or agreed to in writing, software
#   distributed under the License is distributed on an "AS IS" BASIS,
#   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
#   See the License for the specific language governing permissions and
#   limitations under the License.
#
# Author: Salman Niazi 2015



#Experiments
HopsFS_Src_Folder=../
HopsFS_Experiments_Remote_Dist_Folder=/tmp/hops_benchmarks
HopsFS_Rebuild_Exp_Src=false
HopsFS_Upload_Exp=true

#Machines
BM_Machines_FullList=(`cat experiment-nodes`)
DNS_FullList=(`cat datanodes`) 
NNS_FullList=(`cat namenodes`)



#experiments to run
#NOTE all experiment related parameters are in master.properties file
Benchmark_Types=(
            RAW                                          #Test raw throughput of individual operations
            INTERLEAVED                                  #Test synthetic workload from spotify             
            #BR                                          #Block report testing. Set the hart beat time for the datanodes to Long.MAX_VALUE. We use a datanode class that does not send HBs  
            ) #space is delimeter

NN_INCREMENT=1            
EXP_START_INDEX=1
REPEAT_EXP_TIMES=1


All_Results_Folder="/tmp/hops-bm"                         #This is where the results are saved
exp_remote_bench_mark_result_file="/tmp/bm.log"           #This is file generated by master that contains results for the experiment     
NumberNdbDataNodes=4                                      #added to the results of the benchmarks. helps in data aggregation. for HDFS set it to 0             

            


#HopsFS Distribution Parameters
HopsFS_User=nzo
NameNodeRpcPort=26801
HopsFS_Remote_Dist_Folder=/tmp/nzo/hopsfs
Datanode_Data_Dir=$HopsFS_Remote_Dist_Folder/Data










                             

