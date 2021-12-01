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
CPU_AFFINITY=
DIR=$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )
nohup JAVA_BIN  -Dlog4j.configuration=file:$DIR/log4j.properties  -Xmx10g -cp "$DIR/hop-experiments-1.0-SNAPSHOT-jar-with-dependencies.jar:/home/ubuntu/repos/hops/hadoop-dist/target/hadoop-3.2.0.3-SNAPSHOT/share/hadoop/hdfs/lib/*"  io.hops.experiments.controller.Slave  $DIR/slave.properties &> hops-slave.log &
PID=$!

for tid in $(ps --no-headers -ww -p "$PID" -L -olwp | sed 's/$/ /' | tr  -d '\n')
do
   taskset -pc $CPU_AFFINITY "${tid}"  > /dev/null
done

killer="$DIR/kill-slave.sh"
rm -rf $killer
touch $killer
chmod +x $killer

echo  \#\!/bin/bash >  $killer
echo  "kill -9 $PID" >> $killer
echo "Started Slave"


