package ru.ac.phyche.chereshnya.featurepreprocessors;

import ru.ac.phyche.chereshnya.ChemDataset;
import ru.ac.phyche.chereshnya.featuregenerators.FeaturesGenerator;

/**
 * 
 * The subclass of the abstract DropFeaturesPreprocessor class. Training is
 * disabled.
 *
 */
public class DropFeaturesPreprocessorNoTrain extends DropFeaturesPreprocessor {

	@Override
	public void train(FeaturesGenerator features, ChemDataset data) {
		throw (new RuntimeException("Instance of DropFeaturesPreprocessorNoTrain cannot be trained!"
				+ " Use one of other sub-classes of DropFeaturesPreprocessor instead!"));
	}

}
