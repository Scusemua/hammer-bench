import argparse
import io
import time
import os
import random
import datetime
import subprocess

import sys
sys.path.append('/home/ben/.local/lib/python3.6/site-packages')

from pssh.clients import ParallelSSHClient

parser = argparse.ArgumentParser()
parser.add_argument("-i", "--input", type = str, default = "./ndb_internal_ips.txt", help = "File containing clients' IPs.")
parser.add_argument("--config", type = str, default = "", help = "Private ip of nds manager")
parser.add_argument("--start", action = 'store_true', help = "Start the datanodes.")
parser.add_argument("--initial", action = 'store_true', help = "Initial start the datanodes.")
parser.add_argument("-k", "--key-file", dest = "key_file", type = str, default = "~/.ssh/id_rsa", help = "Path to keyfile.")
parser.add_argument("-u", "--user", type = str, default = "zhangjyr", help = "Username for SSH.")

args = parser.parse_args()

ip_file_path = args.input
key_file = args.key_file
user = args.user

hosts = []
with open(ip_file_path, 'r') as ip_file:
    hosts = [x.strip() for x in ip_file.readlines()]
    print("Hosts: %s" % str(hosts))

client = ParallelSSHClient(hosts)

output = None
if args.config != "":
    print("Configuring NDS manager's ip to {}.".format(args.config))
    output = client.run_command("sudo sed -i -e 's|^\\(ndb-connectstring\\)=.*|\\1={}|' /etc/my.cnf".format(args.config))
    output = client.run_command("sudo /etc/init.d/mysql.server restart")

if args.start:
    print("Starting the datanodes.")
    output = client.run_command("sudo ndbmtd")
if args.initial:
    print("Starting the datanodes.")
    output = client.run_command("sudo ndbmtd --initial")
elif output == None:
    print("[WARNING] Neither '--sync' nor '--start' was specified. Doing nothing.")

for i in range(len(output)):
    host_output = output[i]
    for line in host_output.stdout:
        print(line)
