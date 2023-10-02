package ru.ac.phyche.chereshnya.featureselectors;

import java.util.Arrays;
import java.util.Random;

import ru.ac.phyche.chereshnya.ArUtls;
import ru.ac.phyche.chereshnya.ChemDataset;
import ru.ac.phyche.chereshnya.featuregenerators.FeaturesGenerator;
import ru.ac.phyche.chereshnya.featuregenerators.PreprocessedFeaturesGenerator;
import ru.ac.phyche.chereshnya.featurepreprocessors.FeaturesPreprocessor;
import ru.ac.phyche.chereshnya.featurepreprocessors.Scale01FeaturesPreprocessor;
import ru.ac.phyche.chereshnya.featurepreprocessors.SelectedFeaturesPreprocessor;

public abstract class Boruta extends FeatureSelector implements FeatureImportances {

	private int rounds = 10;
	private FeatureImportance[] importances;

	@Override
	public FeatureImportance[] importances() {
		return importances;
	}

	public Boruta() {

	}

	public Boruta(int nRounds) {
		rounds = nRounds;
	}

	public abstract float[] featureImportance(float[][] featuresWithShadow, float[] labels);

	private static class ComparablePair implements Comparable<ComparablePair> {
		float value;
		int index;

		@Override
		public int compareTo(ComparablePair o) {
			return ((Float) value).compareTo(o.value);
		}
	}

	@Override
	public FeaturesGenerator select(ChemDataset train0, FeaturesGenerator gen, int n) {
		Scale01FeaturesPreprocessor scale = new Scale01FeaturesPreprocessor();
		scale.train(gen, train0);
		FeaturesGenerator gen1 = new PreprocessedFeaturesGenerator(gen, scale);

		Random rnd = new Random();
		gen1.precompute(train0);

		ComparablePair[] finalImportances = new ComparablePair[gen.getNumFeatures()];

		for (int i = 0; i < gen.getNumFeatures(); i++) {
			finalImportances[i] = new ComparablePair();
			finalImportances[i].index = i;
			finalImportances[i].value = 0;
		}

		for (int count = 0; count < rounds; count++) {
			ChemDataset train = train0.copy();
			train.compoundsBasedSplitAndShuffle(1);
			float[][] features = gen1.features(train);
			float[][] shadowFeatures = ArUtls.cloneFloatArray2d(features);
			for (int i = 0; i < shadowFeatures.length; i++) {
				for (int x = 0; x < 5; x++) {
					for (int j = 0; j < shadowFeatures[i].length; j++) {
						int l = shadowFeatures[i].length;
						int k = rnd.nextInt(l);
						float a = shadowFeatures[i][j];
						float b = shadowFeatures[i][k];
						shadowFeatures[i][j] = b;
						shadowFeatures[i][k] = a;
					}
				}
			}
			float[][] fullfeatures = new float[features.length][];
			for (int i = 0; i < features.length; i++) {
				fullfeatures[i] = ArUtls.mergeArrays(features[i], shadowFeatures[i]);
			}
			float[] labels = train.allRetentions();
			labels = ArUtls.mult(0.001f, labels);

			float[] imp = featureImportance(fullfeatures, labels);

			float largestShadow = Float.NEGATIVE_INFINITY;
			for (int i = 0; i < features[0].length; i++) {
				int i1 = features[0].length + i;
				if (imp[i1] > largestShadow) {
					largestShadow = imp[i1];
				}
			}

			for (int i = 0; i < features[0].length; i++) {
				finalImportances[i].value = imp[i] > largestShadow ? finalImportances[i].value + 1
						: finalImportances[i].value;
			}
		}

		importances = new FeatureImportance[gen.getNumFeatures()];
		for (int i = 0; i < importances.length; i++) {
			importances[i] = new FeatureImportance();
			importances[i].name = gen.getName(i);
			importances[i].value = finalImportances[i].value;
		}

		Arrays.sort(finalImportances);
		int[] bestFeatures = new int[n];
		for (int i = 0; i < n; i++) {
			bestFeatures[i] = finalImportances[finalImportances.length - 1 - i].index;
		}

		FeaturesPreprocessor select = new SelectedFeaturesPreprocessor(bestFeatures);
		select.train(gen, train0);
		return new PreprocessedFeaturesGenerator(gen, select);
	}

}
