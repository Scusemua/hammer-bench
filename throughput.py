import argparse
import numpy as np
import pandas as pd
import time
import random
import matplotlib as mpl
import matplotlib.pyplot as plt

from mpl_toolkits.axes_grid1.inset_locator import zoomed_inset_axes, mark_inset

plt.style.use('ggplot')
mpl.rcParams['text.color'] = 'black'
mpl.rcParams['xtick.color'] = 'black'
mpl.rcParams['ytick.color'] = 'black'
mpl.rcParams["figure.figsize"] = (8,6)

font = {'weight' : 'bold',
        'size'   : 16}
mpl.rc('font', **font)

parser = argparse.ArgumentParser()

parser.add_argument("-i", "--input", default = "./ALL_DATA.txt", help = "Path to file containing ALL data.")
parser.add_argument("-d", "--duration", default = 60, help = "Duration of the experiment in seconds.")

args = parser.parse_args()

input_path = args.input
duration = args.duration

df = pd.read_csv(input_path)
df = df.sort_values('timestamp')

min_val = min(df['timestamp'])
max_val = max(df['timestamp'])
#print("max_val - min_val =", max_val - min_val)
def adjust(x):
    return (x - min_val) / 1e9

df['ts'] = df['timestamp'].map(adjust)
df2 = df[((df['ts'] >= duration+5))]
min_val2 = min(df2['ts'])

def adjust2(x):
    if x >= min_val2:
        return x - min_val2
    return x

df['ts'] = df['ts'].map(adjust2)

print("Total number of points: %d" % len(df))

buckets = [0 for _ in range(0, duration + 1)]
total = 0
for i in range(1, duration + 1):
    start = i-1
    end = i
    res = df[((df['ts'] >= start) & (df['ts'] <= end))]
    print("%d points between %d and %d" % (len(res), start, end))
    buckets[i] = len(res)
    total += len(res)

df.to_csv("test.csv")

# for _, row in df.iterrows():
#     t = row['ts']
#     buckets[round(t)] += 1

for i, t in enumerate(buckets):
    print("%d,%d" % (i,t))

#print(sum(buckets))
#print(len(df))

plt.figure(figsize=(12,8))
plt.plot(list(range(len(buckets))), buckets)
plt.xlabel("Time (seconds)")
plt.ylabel("Throughput (ops/sec)")
plt.show()