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

plt.style.use('ggplot')
mpl.rcParams['text.color'] = 'black'
mpl.rcParams['xtick.color'] = 'black'
mpl.rcParams['ytick.color'] = 'black'
mpl.rcParams["figure.figsize"] = (8,6)

font = {'weight' : 'bold',
        'size'   : 16}
mpl.rc('font', **font)

x_label_font_size = 24
y_label_font_size = 26
xtick_font_size = 20
markersize = 6
linewidth = 4

parser = argparse.ArgumentParser()

parser.add_argument("-ih", "--input-hopsfs", dest="input_hopsfs", help = "Path to file containing ALL data.")
parser.add_argument("-il", "--input-lambdamds", dest="input_lambdamds", help = "Path to file containing ALL data.")
parser.add_argument("-d", "--duration", default = 60, type = int, help = "Duration of the experiment in seconds.")

args = parser.parse_args()

input_hopsfs = args.input_hopsfs
input_lambdamds = args.input_lambdamds
duration = args.duration

vanilla_colors = ["#ffa822", "#124c6d", "#ff6150", "#1ac0c6", "#7c849c", "#6918b4", "#117e16", "#ff7c00", "#ff00c5"]
lambda_colors = ["#cc7a00", "#0b2e42", "#b31200", "#128387", "#424757", "#410f70", "#0c5a10", "#b35600", "#cc009c"]

def plot_data(input_path, axis = None, vanilla = False):
    # If we pass a single .txt file, then just create DataFrame from the .txt file.
    # Otherwise, merge all .txt files in the specified directory.
    if input_path.endswith(".txt"):
        df = pd.read_csv(input_path)
    else:
        print("input_path: " + input_path)
        print("joined: " + str(os.path.join(input_path, "*.txt")))
        all_files = glob.glob(os.path.join(input_path, "*.txt"))

        if vanilla:
            framework_name = "HopsFS"
            colors = vanilla_colors
            marker = "X"
            markersize = 8
        else:
            framework_name = r'$\lambda$' + "MDS"
            colors = lambda_colors
            marker = "*"
            markersize = 10

        # Merge the .txt files into a single DataFrame.
        for i, filename in enumerate(all_files):
            print("Reading file: " + filename)
            df = pd.read_csv(filename, index_col=None, header=0)
            df.columns = ['timestamp', 'latency']

            # Sort the DataFrame by timestamp.
            df = df.sort_values('latency')

            latencies = df['latency'].values

            current_label = "%s %s" % (framework_name, os.path.basename(filename)[:-4]) # remove the ".txt" with `[:-4]`

            ys = list(range(0, len(latencies)))
            ys = [y / len(ys) for y in ys]
            axis.plot(latencies, ys, label = current_label, linewidth = 2, markersize = markersize, marker = marker, markevery = 0.1, color = colors[i])

fig, axs = plt.subplots(nrows = 1, ncols = 1, figsize=(12,8))

plot_data(input_hopsfs, axis = axs, vanilla = True)
plot_data(input_lambdamds, axis = axs, vanilla = False)

axs.legend()
axs.set_yscale('linear')
axs.set_xlabel("Latency (ms)", fontsize = x_label_font_size)
axs.set_ylabel("Cumulative Probability", fontsize = y_label_font_size)
axs.tick_params(labelsize=xtick_font_size)
axs.set_title("CDF - Spotify Workload - Log Scale x-Axis")

plt.show()