import numpy as np
import pandas as pd
from genetic_selection import GeneticSelectionCV
from sklearn import datasets, linear_model


if __name__ == '__main__':  
    n_desc = 20
    n_gen = 80

    with open('n_desc_ga.tmp') as f:
        n_desc=int(next(f))

    with open('generations_ga.tmp') as f:
        n_gen=int(next(f))

    data = pd.read_csv("./descriptorsri.tmp.csv")
    y = data['RI'].values
    X = data.values[:, 1:]

    estimators = linear_model.LinearRegression()
    selectors = GeneticSelectionCV(estimators,
                                      cv=5,
                                      verbose=5,
                                      scoring="r2",
                                      max_features=n_desc,
                                      n_generations=n_gen,
                                      n_jobs=4
                                      )
    selectors.fit(X,y)
    np.savetxt('ga_desc.txt',selectors.support_) 

