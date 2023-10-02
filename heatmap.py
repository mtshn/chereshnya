import numpy as np                                                          
import seaborn as sns                  
import matplotlib.pyplot as plt
import pandas as pd   
df = pd.read_csv("descriptors.tmp.csv")
plt.figure(figsize=(9.5, 7))
a=False
if df.shape[1]<=11:
    a=True

sns.heatmap(abs(df.corr()),cmap="RdPu", annot=a)  
plt.subplots_adjust(bottom=0.33, top=0.97, left=0.25, right=1.04)
plt.savefig("heatmap.png")

