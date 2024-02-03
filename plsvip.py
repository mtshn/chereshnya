import numpy as np
from sklearn.cross_decomposition import PLSRegression
import pandas as pd  

n_comp = 20

with open('n_comp_pls_vip.tmp') as f:
    n_comp=int(next(f))

data = pd.read_csv("./descriptorsri.tmp.csv")
y = data['RI'].values
X = data.values[:, 1:]
pls =  PLSRegression(n_comp) 
pls.fit(X,y)


t = pls.x_scores_
w = pls.x_weights_
q = pls.y_loadings_
m, p = X.shape
_, h = t.shape
vips = np.zeros((p,))
s = np.diag(t.T @ t @ q.T @ q).reshape(h, -1)
total_s = np.sum(s)
for i in range(p):
    weight = np.array([ (w[i,j] / np.linalg.norm(w[:,j]))**2 for j in range(h) ])
    vips[i] = np.sqrt(p*(s.T @ weight)/total_s)

np.savetxt('vip_score.txt',vips)

