from skimage import data, io, segmentation, color
from skimage.future import graph
from matplotlib import pyplot as plt
import sys

img = data.coffee()
img = io.imread(sys.argv[1], plugin='matplotlib')

labels1 = segmentation.slic(img, compactness=10, n_segments=400)
out1 = color.label2rgb(labels1, img, kind='avg')

g = graph.rag_mean_color(img, labels1, mode='similarity')
labels2 = graph.cut_normalized(labels1, g, thresh=float(sys.argv[3]))
out2 = color.label2rgb(labels2, img, kind='avg')

out2.tofile('src/main/resources/script/segmented_' + sys.argv[2] + '.txt', sep=" ")
