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

# 1) Run make install-slave  in ~/hammer-bench folder.
# 2) Run python3 hammer-bench.py --sync  to update codes on slaves.
# 3) Run python3 hammer-bench.py --start  to launch slaves.
# 4) Run make bench  in ~/hammer-bench to start benching.
#    After benching, you can
# 5) Run python3 hammer-bench.py --stop  to stop slaves.
#
# Steps 2 and 3 can execute in one command.

parser = argparse.ArgumentParser()
parser.add_argument("-i", "--input", type = str, default = "./client_internal_ips.txt", help = "File containing clients' IPs.")
parser.add_argument("--sync-path", type = str, default = "~/hammer-bench/slave.target", help = "Folder to sync from the master.")
parser.add_argument("--sync-dest", type = str, default = "~/hammer-bench-slave", help = "Folder to sync to on the slaves.")
parser.add_argument("--sync", action = 'store_true', help = "Synchronize the execution files.")
parser.add_argument("--start", action = 'store_true', help = "Start the benchmark.")
parser.add_argument("--stop", action = 'store_true', help = "Stop the benchmark.")
parser.add_argument("-k", "--key-file", dest = "key_file", type = str, default = "~/.ssh/id_rsa", help = "Path to keyfile.")
parser.add_argument("-u", "--user", type = str, default = "ben", help = "Username for SSH.")
parser.add_argument("--hdfs-site", type = str, dest = "hdfs_site", default = "/home/ubuntu/repos/hops/hadoop-dist/target/hadoop-3.2.0.3-SNAPSHOT/etc/hadoop/hdfs-site.xml", help = "Location of the hdfs-site.xml file.")

args = parser.parse_args()

sync_path = os.path.expanduser(args.sync_path)
sync_dest = os.path.expanduser(args.sync_dest)
ip_file_path = args.input
key_file = args.key_file
user = args.user
hdfs_site_path = args.hdfs_site

hosts = []
with open(ip_file_path, 'r') as ip_file:
    hosts = [x.strip() for x in ip_file.readlines()]
    print("Hosts: %s" % str(hosts))

client = ParallelSSHClient(hosts)

output = None
if args.sync:
    print("Synchronize execution files form {} to {}".format(sync_path, sync_dest))
    client.run_command("mkdir -p {}".format(sync_dest), stop_on_errors=False)
    greenlet = client.copy_file(sync_path, sync_dest, recurse=True)
    gevent.joinall(greenlet, raise_error=True)

    print("Next, copying hdfs-site.xml configuration file...")
    greenlet = client.copy_file(hdfs_site_path, hdfs_site_path, recurse=True)
    gevent.joinall(greenlet, raise_error=True)

    output = list()

if args.start:
    print("Starting the slaves.")
    os.system("sed -i -e 's|^\\(list.of.slaves=\\).*|\\1{}|' ~/hammer-bench/master.properties".format(",".join(hosts)))
    cmd = "cd {} && make-bench".format(sync_dest)
    print("Executing command: %s" % cmd)
    output = client.run_command(cmd, stop_on_errors=False)
if args.stop:
    print("Stopping the slaves.")
    output = client.run_command("kill -9 `ps aux | grep java | grep io.hops.experiments.controller.Slave | awk '{ print $2 }'`")
elif output == None:
    print("[WARNING] Neither '--sync' nor '--start' was specified. Doing nothing.")

for i in range(len(output)):
    host_output = output[i]
    for line in host_output.stdout:
        print(line)
