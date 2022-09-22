import os 
import sys
import subprocess
import time 
from datetime import datetime
import pandas as pd 
import argparse
import logging

logging.basicConfig(
     level=logging.DEBUG,
     format= '[%(asctime)s] {%(pathname)s:%(lineno)d} %(levelname)s - %(message)s',
     datefmt='%H:%M:%S'
 )

console = logging.StreamHandler()
logging.getLogger('').addHandler(console)

logger = logging.getLogger(__name__)

parser = argparse.ArgumentParser()
parser.add_argument("-i", "--interval", type = float, default = 0.5, help = "Interval to check for active NNs (in seconds).")
args = parser.parse_args()

interval = args.interval

start_time = datetime.now()

if interval <= 0 or interval > 2:
    logger.error("Interval must be within the interval (0, 2).")

res = []
for i in range(0, 5):
  result = subprocess.run(["kubectl", "get", "pods"], stdout=subprocess.PIPE)
  lines = result.stdout.decode().split("\n")
  current_num_nns = 0
  for line in lines:
    if "namenode" not in line:
      continue 
    #if "prewarm" in line:
    #  continue 
    current_num_nns += 1
  
  now = datetime.now()
  logger.info("%s: %d NNs" % (now.strftime("%d/%m/%Y %H:%M:%S"), current_num_nns))
  
  res.append((time.time(), current_num_nns))
  
  time.sleep(interval)

df = pd.DataFrame(res, columns = ["time", "nns"])

df.to_csv("%s_nns.csv" % start_time.strftime("%d-%m-%Y_%H-%M-%S"))
