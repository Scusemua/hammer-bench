import argparse
import numpy as np
import pandas as pd
import time
import random
import matplotlib as mpl
import matplotlib.pyplot as plt
import glob
import os

from mpl_toolkits.axes_grid1.inset_locator import zoomed_inset_axes, mark_inset

# # # # # # # # # # # # # # # # # #
# Plot throughput for \lambdaMDS. #
# # # # # # # # # # # # # # # # # #

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
parser.add_argument("-n", "--namenodes", default = None, help = "Path to associated NN monitoring CSV.")
parser.add_argument("-d", "--duration", default = 60, type = int, help = "Duration of the experiment in seconds.")
parser.add_argument("-u", "--units", default = "ns", type = str, help = "Units of input data. Enter 'ns' for nanoseconds and 'ms' for milliseconds.")
parser.add_argument("-c", "--columns", default = ["timestamp", "latency", "worker_id", "path"], nargs='+')
args = parser.parse_args()

input_path = args.input
duration = args.duration
namenodes_path = args.namenodes
units = args.units
COLUMNS = args.columns

print(COLUMNS)

# COLUMNS = ['timestamp', 'latency', 'worker_id', 'path']

if units == 'ns':
    adjust_divisor = 1e9
elif units == 'ms':
    adjust_divisor = 1e3
else:
    raise ValueError("Unknown/unsupported units: " + str(units))

# If we pass a single .txt file, then just create DataFrame from the .txt file.
# Otherwise, merge all .txt files in the specified directory.
if input_path.endswith(".txt"):
    df = pd.read_csv(input_path, index_col=None, header=0)
    df.columns = COLUMNS
else:
    print("input_path: " + input_path)
    print("joined: " + str(os.path.join(input_path, "*.txt")))
    all_files = glob.glob(os.path.join(input_path, "*.txt"))
    li = []
    print("Merging the following files: %s" % str(all_files))
    # Merge the .txt files into a single DataFrame.
    for filename in all_files:
        print("Reading file: " + filename)
        tmp_df = pd.read_csv(filename, index_col=None, header=0)
        tmp_df.columns = COLUMNS
        li.append(tmp_df)
    df = pd.concat(li, axis=0, ignore_index=True)
    df.columns = COLUMNS

print(df)
# Sort the DataFrame by timestamp.
print("Sorting now...")
start_sort = time.time()
df = df.sort_values('timestamp')
print("Sorted dataframe in %f seconds." % (time.time() - start_sort))

min_val = min(df['timestamp'])
max_val = max(df['timestamp'])
print("max_val - min_val =", max_val - min_val)
print("max_val - min_val =", (max_val - min_val) / adjust_divisor)
def adjust(x):
    return (x - min_val) / adjust_divisor

# Sometimes, there's a bunch of data with WAY different timestamps -- like, several THOUSAND
# seconds different. So, I basically adjust all of that data so it fits within the interval
# of the rest of the data.
df['ts'] = df['timestamp'].map(adjust)
df2 = df[((df['ts'] >= duration+5))]
if len(df2) > 0:
    min_val2 = min(df2['ts'])

    def adjust2(x):
        if x >= min_val2:
            return x - min_val2
        return x

    df['ts'] = df['ts'].map(adjust2)

print("Total number of points: %d" % len(df))

# For each second of the workload, count all the data points that occur during that second.
# These are the points that we'll plot.
buckets = [0 for _ in range(0, duration + 1)]
total = 0
for i in range(1, duration + 1):
    start = i-1
    end = i
    res = df[((df['ts'] >= start) & (df['ts'] <= end))]
    #print("%d points between %d and %d" % (len(res), start, end))
    buckets[i] = len(res)
    total += len(res)

# df.to_csv("test.csv")

# for _, row in df.iterrows():
#     t = row['ts']
#     buckets[round(t)] += 1

# for i, t in enumerate(buckets):
#     print("%d,%d" % (i,t))

#print(sum(buckets))
#print(len(df))

# Check for a nns.csv in the input path.
if namenodes_path is None:
    namenodes_path = input_path + "/nns.csv"
    if not os.path.isfile(namenodes_path):
        namenodes_path = None

if namenodes_path is not None:
    fig, axs = plt.subplots(nrows = 1, ncols = 2, figsize=(12,8))

    df_nns = pd.read_csv(namenodes_path)
    min_val = min(df_nns["time"])

    def adjust_nn_timestamp(timestamp):
        return (timestamp - min_val) / 1e3

    df_nns["ts"] = df_nns["time"].map(adjust_nn_timestamp)

    xs = df_nns["ts"]
    ys = df_nns["nns"]

    axs[1].plot(xs, ys)
    axs[1].set_xlabel("Time (seconds)")
    axs[1].set_ylabel("Number of Active NNs")

    axs[0].plot(list(range(len(buckets))), buckets)
    axs[0].set_xlabel("Time (seconds)")
    axs[0].set_ylabel("Throughput (ops/sec)")
else:
    fig, axs = plt.subplots(nrows = 1, ncols = 1, figsize=(12,8))

    axs.plot(list(range(len(buckets))), buckets)
    axs.set_xlabel("Time (seconds)")
    axs.set_ylabel("Throughput (ops/sec)")

plt.show()