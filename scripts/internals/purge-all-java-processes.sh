#!/bin/bash
# Author: Salman Niazi 2014
# This script broadcasts all files required for running a HOP instance.
# A password-less sign-on should be setup prior to calling this script



if [ -z $1 ]; then
	echo "please, specify the user name. usage [uername] [process name] i.e. root java"
	exit
fi

if [ -z $2 ]; then
        echo "please, specify the process name. usage [uername] [process name] i.e. root java"
        exit
fi

#All Unique Hosts
# All_Hosts_To_Purge=("10.241.64.12" "10.241.64.14")
All_Hosts_To_Purge=("35.194.69.127")

for i in ${All_Hosts_To_Purge[@]}
do
	connectStr="$1@$i"
#	ssh $connectStr "ps -ax | grep "java""
        pids=""
        pids=`ssh $connectStr pgrep -u $1 $2`
	
	if [ -z "${pids}" ]; then
		echo "There is no process named $2 running on $i" 
	else
        	echo "Killing $2 on $i. PIDS to kill "$pids
		ssh $connectStr  kill -9 $pids
	fi
done

