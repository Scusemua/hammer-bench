import argparse
import pandas as pd

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
print("max_val - min_val =", max_val - min_val)
def adjust(x):
    return (x - min_val) / 1e9

df['ts'] = df['timestamp'].map(adjust)
df2 = df[((df['ts'] >= duration+1))]
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

print(sum(buckets))
print(len(df))