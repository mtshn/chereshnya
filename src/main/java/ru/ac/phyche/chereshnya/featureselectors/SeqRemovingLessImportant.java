package ru.ac.phyche.chereshnya.featureselectors;

import java.util.Arrays;

import ru.ac.phyche.chereshnya.ArUtls;
import ru.ac.phyche.chereshnya.ChemDataset;
import ru.ac.phyche.chereshnya.featuregenerators.FeaturesGenerator;
import ru.ac.phyche.chereshnya.featuregenerators.PreprocessedFeaturesGenerator;
import ru.ac.phyche.chereshnya.featurepreprocessors.Scale01FeaturesPreprocessor;
import ru.ac.phyche.chereshnya.featurepreprocessors.SelectedFeaturesPreprocessor;
import ru.ac.phyche.chereshnya.featureselectors.FeatureImportances.FeatureImportance;
import ru.ac.phyche.chereshnya.featureselectors.FeatureSelector.SeqRemoving;

public abstract class SeqRemovingLessImportant extends SeqRemoving implements FeatureImportances {

	public abstract float[] featureImportance(float[][] featuresWithShadow, float[] labels);

	private FeatureImportance[] importances;

	@Override
	public FeatureImportance[] importances() {
		return importances;
	}

	public int removeDescriptorsAtOnce = 10;

	public SeqRemovingLessImportant(int removeDescriptorsAtOnce_) {
		this.removeDescriptorsAtOnce = removeDescriptorsAtOnce_;
	}

	public SeqRemovingLessImportant() {

	}

	private static class ComparablePair implements Comparable<ComparablePair> {
		float value;
		int index;

		@Override
		public int compareTo(ComparablePair o) {
			return ((Float) value).compareTo(o.value);
		}
	}

	@Override
	public void removeNext(ChemDataset train, FeaturesGenerator gen, int n) {
		SelectedFeaturesPreprocessor s = new SelectedFeaturesPreprocessor(this.getSelectedDescritptors());
		s.train(gen, train);
		PreprocessedFeaturesGenerator p = new PreprocessedFeaturesGenerator(gen, s);
		Scale01FeaturesPreprocessor scale = new Scale01FeaturesPreprocessor();
		scale.train(p, train);
		PreprocessedFeaturesGenerator gen1 = new PreprocessedFeaturesGenerator(p, scale);

		gen1.precompute(train);
		ComparablePair[] finalImportances = new ComparablePair[gen.getNumFeatures()];

		float[][] features = gen1.features(train);
		float[] labels = train.allRetentions();
		labels = ArUtls.mult(0.001f, labels);

		float[] imp = featureImportance(features, labels);
		for (int i = 0; i < finalImportances.length; i++) {
			boolean selected = false;
			int newNum = -1;
			for (int j = 0; j < this.getSelectedDescritptors().length; j++) {
				if (this.getSelectedDescritptors()[j] == i) {
					selected = true;
					newNum = j;
				}
			}
			if (selected) {
				finalImportances[i] = new ComparablePair();
				finalImportances[i].index = i;
				finalImportances[i].value = imp[newNum];
			} else {
				finalImportances[i] = new ComparablePair();
				finalImportances[i].index = i;
				finalImportances[i].value = Float.NEGATIVE_INFINITY;
			}
		}
		if (this.getSelectedDescritptors().length == gen.getNumFeatures()) { // First call of method. Nothing was
																				// excluded at this place.
			importances = new FeatureImportance[gen.getNumFeatures()];
			for (int i = 0; i < importances.length; i++) {
				importances[i] = new FeatureImportance();
				importances[i].name = gen.getName(i);
				importances[i].value = finalImportances[i].value;
			}
		}

		Arrays.sort(finalImportances);
		int retain = Math.max(n, this.getSelectedDescritptors().length - this.removeDescriptorsAtOnce);
		int[] bestFeatures = new int[retain];
		for (int i = 0; i < retain; i++) {
			bestFeatures[i] = finalImportances[finalImportances.length - 1 - i].index;
		}
		Arrays.sort(bestFeatures);
		this.setSelectedDescritptors(bestFeatures);
	}

}
