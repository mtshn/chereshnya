package ru.ac.phyche.chereshnya.featurepreprocessors;

import java.util.ArrayList;

import ru.ac.phyche.chereshnya.ChemDataset;
import ru.ac.phyche.chereshnya.featuregenerators.FeaturesGenerator;

public class DropLowVarPreprocessor extends DropFeaturesPreprocessor {

	private float var = 0;

	@Override
	public void train(FeaturesGenerator features, ChemDataset data) {
		features.precompute(data);
		float[][] featuresFloat = features.features(data);
		ArrayList<Integer> drop = new ArrayList<Integer>();
		ArrayList<String> namesRetain = new ArrayList<String>();
		if (featuresFloat.length * features.getNumFeatures() != 0) {
			for (int i = 0; i < features.getNumFeatures(); i++) {
				float min = Float.POSITIVE_INFINITY;
				float max = Float.NEGATIVE_INFINITY;
				float summ = 0;
				for (int j = 0; j < featuresFloat.length; j++) {
					if (featuresFloat[j][i] < min) {
						min = featuresFloat[j][i];
					}
					if (featuresFloat[j][i] > max) {
						max = featuresFloat[j][i];
					}
					summ += featuresFloat[j][i];
				}
				float average = summ / featuresFloat.length;
				float summDev = 0;
				if (max - min > 1E-6) {
					for (int j = 0; j < featuresFloat.length; j++) {
						float d = featuresFloat[j][i] - average;
						d = d / (max - min);
						summDev = summDev + d * d;
					}
				}
				float dev = summDev / featuresFloat.length;
				if (Math.sqrt(dev) <= var) {
					drop.add(i);
				} else {
					namesRetain.add(features.getName(i));
				}
			}
			int[] result = new int[drop.size()];
			for (int i = 0; i < result.length; i++) {
				result[i] = drop.get(i);
			}
			setFeaturesToDrop(result);
			setNames(namesRetain.toArray(new String[namesRetain.size()]));
		} else {
			setNames(features.getNames());
			setFeaturesToDrop(new int[] {});
		}
	}

	/**
	 * 
	 * @param var minimum allowed value of square root of variance (for scaled to
	 *            0...1 range values)
	 */
	public DropLowVarPreprocessor(float var) {
		this.var = var;
	}

}
