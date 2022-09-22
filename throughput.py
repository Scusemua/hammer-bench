import argparse
import pandas as pd

parser = argparse.ArgumentParser()

parser.add_argument("-i", "--input", default = "./ALL_DATA.txt", help = "Path to file containing ALL data.")
parser.add_argument("-d", "--duration", default = 60, help = "Duration of the experiment in seconds.")

args = parser.add_argument()

input_path = args.input
duration = args.duration

df = pandas.read_csv(input_path)
df = df.sort_values('timestamp')

def adjust(x):
    return x / 1e9

df['ts'] = df['timestamp'].map(adjust)

buckets = [0 for _ in range(0, duration)]
for i in range(1, duration):
    start = i-1
    end = i
    res = df[((df >= start) & (df <= end)).all(axis=1)]
    buckets = len(res)

print(sum(buckets))
