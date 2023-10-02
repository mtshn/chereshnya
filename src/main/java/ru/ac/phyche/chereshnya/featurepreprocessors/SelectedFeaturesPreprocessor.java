package ru.ac.phyche.chereshnya.featurepreprocessors;

import java.util.ArrayList;

import ru.ac.phyche.chereshnya.ChemDataset;
import ru.ac.phyche.chereshnya.featuregenerators.FeaturesGenerator;

public class SelectedFeaturesPreprocessor extends DropFeaturesPreprocessor {
	private int includedFeatures[];

	public SelectedFeaturesPreprocessor(int[] includedFeatures_) {
		this.includedFeatures = includedFeatures_;
	}

	public SelectedFeaturesPreprocessor(ArrayList<Integer> includedFeaturesList) {
		this.includedFeatures = new int[includedFeaturesList.size()];
		for (int i = 0; i < this.includedFeatures.length; i++) {
			this.includedFeatures[i] = includedFeaturesList.get(i);
		}
	}

	@Override
	public void train(FeaturesGenerator features, ChemDataset data) {
		int[] drop = new int[features.getNumFeatures() - includedFeatures.length];
		int d = 0;
		int r = 0;
		String[] namesRetain = new String[includedFeatures.length];
		for (int i = 0; i < features.getNumFeatures(); i++) {
			boolean found = false;
			for (int j = 0; j < includedFeatures.length; j++) {
				if (i == includedFeatures[j]) {
					found = true;
				}
			}
			if (!found) {
				drop[d] = i;
				d++;
			} else {
				namesRetain[r] = features.getName(i);
				r++;
			}
		}

		this.setFeaturesToDrop(drop);
		this.setNames(namesRetain);
	}

}
