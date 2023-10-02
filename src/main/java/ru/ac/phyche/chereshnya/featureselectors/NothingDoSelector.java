package ru.ac.phyche.chereshnya.featureselectors;

import ru.ac.phyche.chereshnya.ChemDataset;
import ru.ac.phyche.chereshnya.featuregenerators.FeaturesGenerator;

public class NothingDoSelector extends FeatureSelector {

	@Override
	public FeaturesGenerator select(ChemDataset train, FeaturesGenerator gen, int n) {
		return gen;
	}

}
