#!/bin/bash
# Author: Salman Niazi 2015
# Run all the damn benchmarks


DIR=$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )
source $DIR/experiment-env.sh

exp_master_prop_file="$DIR/master.properties"
exp_deploy_script="$DIR/internals/deploy-experiments.sh"
exp_start_script="$DIR/internals/start-exp.sh"
exp_stop_script="$DIR/internals/kill-exp.sh"
Start_HopsFS_Script="$DIR/internals/hdfs-kill-format-start.sh"
exp_stop_hdfs_script="$DIR/internals/stop-hdfs.sh"
kill_java_everywhere="$DIR/internals/kill-all-java-processes-on-all-machines.sh .*java"
exp_drop_create_schema="$DIR/internals/drop-create-schema.sh"
run_nmon_script="$DIR/internals/run-nmon.sh"
stop_nmon_script="$DIR/internals/stop-and-collect-nmon.sh"

kill_NNs=false
randomize_NNs_list=true

#############################################################################################################################
run() {
  echo "*************************** Exp Params Start ****************************"
  echo "Slaves $ExpMaster ${ExpSlaves[@]}"
  echo "Master $ExpMaster"
  echo "Threads/Slave $ClientsPerSlave"
  echo "currentExpDir $currentExpDir"
  echo "BenchMark $BenchMark"
  echo "HBTime $HBTime"
  echo "DataNodes $DNS_FullList_STR"
  echo "Bootstrap NN $BOOT_STRAP_NN"
  echo "*************************** Exp Params End ****************************"

  sed -i 's|list.of.slaves.*|list.of.slaves='"$ExpMaster ${ExpSlaves[@]}"'|g'       $exp_master_prop_file
  sed -i 's|benchmark.type.*|benchmark.type='$BenchMark'|g'                         $exp_master_prop_file
  sed -i 's|num.slave.threads.*|num.slave.threads='$ClientsPerSlave'|g'             $exp_master_prop_file
  sed -i 's|results.dir.*|results.dir='$exp_remote_bench_mark_result_dir'|g'        $exp_master_prop_file
  sed -i 's|fs.defaultFS=.*|fs.defaultFS='$BOOT_STRAP_NN'|g'                        $exp_master_prop_file
  sed -i 's|no.of.namenodes.*|no.of.namenodes='$TotalNNCount'|g'                    $exp_master_prop_file
  sed -i 's|no.of.ndb.datanodes=.*|no.of.ndb.datanodes='$NumberNdbDataNodes'|g'     $exp_master_prop_file
#  sed -i 's|warmup.phase.wait.time=.*|warmup.phase.wait.time='$EXP_WARM_UP_TIME'|g' $exp_master_prop_file

 source $run_nmon_script

  date1=$(date +"%s")
#: <<'END'
  DIR=$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )
 if [ $kill_NNs = true ]; then
    echo "*** Starting HopsFS ***"
    source $Start_HopsFS_Script;
 fi

  echo "*** starting the benchmark ***"
  ssh $HopsFS_User@$ExpMaster mkdir -p $exp_remote_bench_mark_result_dir
  source $exp_start_script $ExpMaster
  scp $HopsFS_User@$ExpMaster:$exp_remote_bench_mark_result_dir/* $currentExpDir/

  echo "*** shutting down the exp nodes ***"
  source $exp_stop_script           # kills exp

  #source sto_rename_delete.sh /test

 if [ $kill_NNs = true ]; then
    source $exp_stop_hdfs_script      # kills hdfs
    source $kill_java_everywhere;      # kills all zombie java processes
 fi

 mkdir -p $currentExpDir/nmon
 source $stop_nmon_script $currentExpDir/nmon/
#END
  date2=$(date +"%s")
  diff=$(($date2-$date1))
  cat $currentExpDir/*.txt
  echo "ExpTime $currentExpDir $(($diff / 60)) minutes and $(($diff % 60)) seconds."
}


shuffle() {
   if [ $randomize_NNs_list = true ]; then
     local i tmp size max rand
     # $RANDOM % (i+1) is biased because of the limited range of $RANDOM
     # Compensate by using a range which is a multiple of the array size.
     size=${#NNS_FullList[*]}
     max=$(( 32768 / size * size ))

     for ((i=size-1; i>0; i--)); do
       while (( (rand=$RANDOM) >= max )); do :; done
       rand=$(( rand % (i+1) ))
       tmp=${NNS_FullList[i]} NNS_FullList[i]=${NNS_FullList[rand]} NNS_FullList[rand]=$tmp
     done
   fi
}


rm -rf $All_Results_Folder
mkdir -p $All_Results_Folder
counter=0

echo "*** deploying experiment jars ***"
source $exp_deploy_script
echo "Repeating experiment $REPEAT_EXP_TIMES times."

while [  $counter -lt $REPEAT_EXP_TIMES ]; do
  let counter+=1
  echo "Preparing for executing # $counter of experiment..."
  currentDir="$All_Results_Folder/run_$counter"
  mkdir -p $currentDir

  currentNNIndex=$EXP_START_INDEX
  for ((e_x = 0; e_x < ${#Benchmark_Types[@]}; e_x++)) do
    BenchMark=${Benchmark_Types[$e_x]}

    echo "Running benchmark '$BenchMark' now"

    DNS_FullList_STR=""
    HBTime=3
    for ((e_dn = 0; e_dn < ${#DNS_FullList[@]}; e_dn++)) do
      DNS_FullList_STR="$DNS_FullList_STR ${DNS_FullList[$e_dn]}"
    done
    HBTime=3

    currentDirBM="$currentDir/$BenchMark"
    mkdir -p $currentDirBM
    TotalNNCount=1

    TotalSlaves=${#BM_Machines_FullList[@]}

    ClientsPerSlave=1
    EXP_WARM_UP_TIME=30000 #10 mins
    TotalClients=$(echo "scale=2; ($TotalNNCount * $DFS_CLIENTS_PER_NAMENODE)" | bc)
    ClientsPerSlave=$(echo "scale=2; ($TotalClients)/$TotalSlaves" | bc)

    #ceiling
    ClientsPerSlave=$(echo "scale=2; ($ClientsPerSlave + 0.5) " | bc)
    ClientsPerSlave=$(echo "($ClientsPerSlave/1)" | bc)
    TotalClients=$(echo "($ClientsPerSlave * $TotalSlaves)" | bc) #recalculate

    echo "ClientsPerSlave: $ClientsPerSlave"
    echo "TotalClients: $TotalClients"
    echo "TotalSlaves: $TotalSlaves"

    ExpSlaves=""
    ExpMaster=""
    for ((e_k = 0; e_k < ${#BM_Machines_FullList[@]}; e_k++)) do
        if [ -z "$ExpMaster" ]; then
            ExpMaster=${BM_Machines_FullList[$e_k]}
        else
            ExpSlaves="$ExpSlaves ${BM_Machines_FullList[$e_k]}"
        fi
    done

    echo "ExpSlaves: $ExpSlaves"
    echo "ExpMaster: $ExpMaster"

    BOOT_STRAP_NN="$FsDefaultName"

#    if [ -z "$NameNodeRpcPort" ]; then
#       BOOT_STRAP_NN="hdfs://$Current_Leader_NN"
#    else
#       RPC_PORT=$(echo "($NameNodeRpcPort)" | bc)
#       BOOT_STRAP_NN="hdfs://$Current_Leader_NN:$RPC_PORT"
#    fi

    currentExpDir="$currentDirBM/$TotalNNCount-NN-$TotalClients-Clients-$BenchMark-BenchMark"
    mkdir -p  $currentExpDir
    run

  done

done
exit


