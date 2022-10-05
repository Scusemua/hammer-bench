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

# python latency_individual.py -xlim 0.25 -ylim 0.75 -ih "G:\Documents\School\College\MasonLeapLab_Research\ServerlessMDS\Benchmark\HammerBench\Vanilla\hops-vanilla-bursty-15s-202209251644-nr" -il "G:\Documents\School\College\MasonLeapLab_Research\ServerlessMDS\Benchmark\HammerBench\HammerBenchServerless_120Thread_8VM_Loc50k_300sec_v2\latency_data" -n 25

#####################################################
# Latency comparison between HopsFS and \lambdaMDS. #
#####################################################
#
# Plots latencies INDIVIDUALLY for each operation.

plt.style.use('ggplot')
mpl.rcParams['text.color'] = 'black'
mpl.rcParams['xtick.color'] = 'black'
mpl.rcParams['ytick.color'] = 'black'
mpl.rcParams["figure.figsize"] = (8,6)

font = {'weight' : 'bold',
        'size'   : 16}
mpl.rc('font', **font)

x_label_font_size = 18
y_label_font_size = 16
xtick_font_size = 14
markersize = 6
linewidth = 4

parser = argparse.ArgumentParser()

parser.add_argument("-ih", "--input-hopsfs", dest="input_hopsfs", help = "Path to file containing ALL data.")
parser.add_argument("-il", "--input-lambdamds", dest="input_lambdamds", help = "Path to file containing ALL data.")

parser.add_argument("-ylim", default = 0.0, type = float, help = "Set the limit of each y-axis to this percent of the max value.")
parser.add_argument("-xlim", default = 1.0, type = float, help = "Set the limit of each x-axis to this percent of the max value.")

parser.add_argument("-n", default = 1, type = int, help = "Plot every `n` points (instead of all points).")

parser.add_argument("--show", action = 'store_true', help = "Show the plot rather than just write it to a file")

parser.add_argument("-o", "--output", type = str, default = None, help = "File path to write chart to. If none specified, then don't write to file.")
parser.add_argument("-c", "--columns", default = ["timestamp", "latency", "worker_id", "path"], nargs='+')

args = parser.parse_args()

input_hopsfs = args.input_hopsfs
input_lambdamds = args.input_lambdamds
xlim_percent = args.xlim
ylim_percent = args.ylim
n = args.n 
show_plot = args.show
output_path = args.output
COLUMNS = args.columns

print(COLUMNS)

vanilla_colors = ["#ffa822", "#124c6d", "#ff6150", "#1ac0c6", "#7c849c", "#6918b4", "#117e16", "#ff7c00", "#ff00c5"]
lambda_colors = ["#cc7a00", "#0b2e42", "#b31200", "#128387", "#424757", "#410f70", "#0c5a10", "#b35600", "#cc009c"]
ops = {}

def plot_data(input_path, axis = None, vanilla = False):
    global ops 
    
    # If we pass a single .txt file, then just create DataFrame from the .txt file.
    # Otherwise, merge all .txt files in the specified directory.
    if input_path.endswith(".txt"):
        df = pd.read_csv(input_path)
    else:
        print("input_path: " + input_path)
        print("joined: " + str(os.path.join(input_path, "*.txt")))
        all_files = glob.glob(os.path.join(input_path, "*.txt"))

        if vanilla:
            framework_name = "W" #"HopsFS"
            colors = vanilla_colors
            marker = "X"
            markersize = 8
        else:
            framework_name = "R" #r'$\lambda$' + "MDS"
            colors = lambda_colors
            marker = "*"
            markersize = 10
        
        next_idx = 0
        reuse_existing = False 
        idx = 0

        # Merge the .txt files into a single DataFrame.
        for i, filename in enumerate(all_files):
            print("Reading file: " + filename)
            df = pd.read_csv(filename, index_col=None, header=0)
            df.columns = COLUMNS

            # Sort the DataFrame by timestamp.
            df = df.sort_values('latency')
            df['latency'] = df['latency'].map(lambda x: x / 1e6)

            latencies = df['latency'].values.tolist()
            latencies = latencies[:-5]

            fs_operation_name = os.path.basename(filename)[:-4] # remove the ".txt" with `[:-4]`
            current_label = "%s - %s" % (framework_name, fs_operation_name)
            
            print("fs_operation_name: " + fs_operation_name)
            if fs_operation_name in ops:
              print("Found " + fs_operation_name + " in ops")
              idx = ops[fs_operation_name]
              reuse_existing = True 
            else:
              print("Did NOT find " + fs_operation_name + " in ops")
              idx = next_idx
              reuse_existing = False 
              next_idx += 1
              ops[fs_operation_name] = idx

            ys = list(range(0, len(latencies)))
            ys = [y / len(ys) for y in ys]

            #axis[idx].plot(latencies[::n] + [latencies[-1]], ys[::n] + [ys[-1]], label = current_label, linewidth = 2, markersize = markersize, marker = marker, markevery = 0.1, color = colors[idx])
            axis[idx].plot(latencies, ys, label = current_label, linewidth = 2, markersize = markersize, marker = marker, markevery = 0.1, color = colors[idx])
            axis[idx].set_yscale('linear')
            axis[idx].set_xlabel("Latency (ms)", fontsize = x_label_font_size)
            axis[idx].set_ylabel("Cumulative Probability", fontsize = y_label_font_size)
            axis[idx].tick_params(labelsize=xtick_font_size)
            axis[idx].set_title(fs_operation_name)
            #axis[idx].set_xlim(left = -0.1, right = (xlim_percent * latencies[-1]) * 1.05)
            axis[idx].set_ylim(bottom = ylim_percent, top = 1.0125)

fig, axs = plt.subplots(nrows = 1, ncols = 9, figsize=(48, 8))

plot_start = time.time()
if input_hopsfs is not None:
    plot_data(input_hopsfs, axis = axs, vanilla = True)
if input_lambdamds is not None:
    plot_data(input_lambdamds, axis = axs, vanilla = False)
print("Plotted all data points in %f seconds" % (time.time() - plot_start))

#for ax in axs:
    #ax.legend(loc = 'lower right')

#fig.legend()
plt.suptitle("Latency CDF - Spotify Workload - Log Scale x-Axis")
fig.tight_layout()
# axs.set_yscale('linear')
# axs.set_xlabel("Latency (ms)", fontsize = x_label_font_size)
# axs.set_ylabel("Cumulative Probability", fontsize = y_label_font_size)
# axs.tick_params(labelsize=xtick_font_size)
# axs.set_title("CDF - Spotify Workload - Log Scale x-Axis")

if output_path is not None:
  print("Saving plot to file '%s' now" % output_path)
  plt.savefig(output_path)
  print("Done")

if show_plot:
  print("Displaying figure now.")
  plt.show()