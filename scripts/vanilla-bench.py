import argparse
import io
import time
import os
import random
import datetime
import subprocess
import gevent
from termcolor import colored

import sys
sys.path.append('/home/ben/.local/lib/python3.6/site-packages')

from pssh.clients import ParallelSSHClient

parser = argparse.ArgumentParser()
parser.add_argument("-i", "--input", type = str, default = "./client_internal_ips.txt", help = "File containing clients' IPs.")
parser.add_argument("-fs", "--fs-default", type = str, default = "./nn_internal_ips.txt", help = "File containing fs default name.")
parser.add_argument("--sync-path", type = str, default = "~/hammer-bench/slave.target", help = "Folder to sync from the master.")
parser.add_argument("--sync-dest", type = str, default = "~/hammer-bench-slave", help = "Folder to sync to on the slaves.")
parser.add_argument("--sync", action = 'store_true', help = "Synchronize the execution files.")
parser.add_argument("--start", action = 'store_true', help = "Start the benchmark.")
parser.add_argument("--stop", action = 'store_true', help = "Stop the benchmark.")
parser.add_argument("--dryrun", action = 'store_true', help = "Run as simulation.")
parser.add_argument("-k", "--key-file", dest = "key_file", type = str, default = "~/.ssh/id_rsa", help = "Path to keyfile.")
parser.add_argument("-u", "--user", type = str, default = "zhangjyr", help = "Username for SSH.")

args = parser.parse_args()

sync_path = os.path.expanduser(args.sync_path)
sync_dest = os.path.expanduser(args.sync_dest)
ip_file_path = args.input
key_file = args.key_file
user = args.user

hosts = []
with open(ip_file_path, 'r') as ip_file:
    hosts = [x.strip() for x in ip_file.readlines()]
    print("Hosts: %s" % str(hosts))

fs_default = None
with open(args.fs_default, 'r') as nn_file:
    # read first line only.
    fs_default = "hdfs://{}:8020".format(nn_file.readline().strip())
    print("Fs default name: %s" % fs_default)

client = ParallelSSHClient(hosts)

output = None
if args.sync:
    print("Synchronize execution files form {} to {}".format(sync_path, sync_dest))
    client.run_command("mkdir -p {}".format(sync_dest), stop_on_errors=False)
    greenlet = client.copy_file(sync_path, sync_dest, recurse=True)
    gevent.joinall(greenlet, raise_error=True)
    output = list()

if args.start:
    print("Starting the slaves.")
    os.system("sed -i -e 's|^\\(benchmark.dryrun=\\).*|\\1{}|' ~/hammer-bench/master.properties".format("true" if args.dryrun else "false"))
    os.system("sed -i -e 's|^\\(list.of.slaves=\\).*|\\1{}|' ~/hammer-bench/master.properties".format(",".join(hosts)))
    os.system("sed -i -e 's|^\\(fs.defaultFS=\\).*|\\1{}|' ~/hammer-bench/master.properties".format(fs_default))
    output = client.run_command("cd {} && make bench".format(sync_dest), stop_on_errors=False)
if args.stop:
    print("Stopping the slaves.")
    output = client.run_command("kill -9 `ps aux | grep java | grep io.hops.experiments.controller.Slave | awk '{ print $2 }'`")
elif output == None:
    print("[WARNING] Neither '--sync' nor '--start' was specified. Doing nothing.")

for i in range(len(output)):
    host_output = output[i]
    for line in host_output.stdout:
        print(line)
