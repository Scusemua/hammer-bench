import os 
import sys
import subprocess
import time 
from datetime import datetime
import pandas as pd 

start_time = datetime.now()

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
  print("%s: %d NNs" % (now.strftime("%d/%m/%Y %H:%M:%S"), current_num_nns))
  
  res.append((time.time(), current_num_nns))
  
  time.sleep(0.5)

df = pd.DataFrame(res, columns = ["time", "nns"])

df.to_csv("%s_nns.csv" % start_time.strftime("%d-%m-%Y_%H-%M-%S"))
