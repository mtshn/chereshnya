package ru.ac.phyche.chereshnya.featureselectors;

import java.util.Arrays;

import ru.ac.phyche.chereshnya.ArUtls;
import ru.ac.phyche.chereshnya.ChemDataset;
import ru.ac.phyche.chereshnya.featuregenerators.FeaturesGenerator;
import ru.ac.phyche.chereshnya.featuregenerators.PreprocessedFeaturesGenerator;
import ru.ac.phyche.chereshnya.featurepreprocessors.Scale01FeaturesPreprocessor;
import ru.ac.phyche.chereshnya.featurepreprocessors.SelectedFeaturesPreprocessor;
import ru.ac.phyche.chereshnya.models.ModelRI;
import ru.ac.phyche.chereshnya.models.QSRRModelRI;

public abstract class SeqAdditionBestLoss extends FeatureSelector.SeqAddition {

	public abstract ModelRI model(PreprocessedFeaturesGenerator p);

	public abstract QSRRModelRI.AccuracyMeasure measure();

	@Override
	public void addNext(ChemDataset train, FeaturesGenerator gen, int n) {
		float[] accuracies = new float[gen.getNumFeatures()];
		int[][] featuresSets = new int[gen.getNumFeatures()][];
		Arrays.stream(ArUtls.intsrnd(gen.getNumFeatures())).parallel().forEach(i -> {
			boolean found = false;
			int[] selectedDescriptors = this.getSelectedDescritptors();
			for (int j = 0; j < selectedDescriptors.length; j++) {
				if (i == selectedDescriptors[j]) {
					found = true;
				}
			}
			if (found) {
				accuracies[i] = Float.MAX_VALUE;
				featuresSets[i] = null;
			} else {
				try {
					int[] newFeatures = new int[selectedDescriptors.length + 1];
					for (int j = 0; j < selectedDescriptors.length; j++) {
						newFeatures[j] = selectedDescriptors[j];
					}
					newFeatures[selectedDescriptors.length] = i;
					SelectedFeaturesPreprocessor s = new SelectedFeaturesPreprocessor(newFeatures);
					s.train(gen, train);
					PreprocessedFeaturesGenerator p = new PreprocessedFeaturesGenerator(gen, s);
					Scale01FeaturesPreprocessor scale = new Scale01FeaturesPreprocessor();
					scale.train(p, train);
					p = new PreprocessedFeaturesGenerator(p, scale);
					ModelRI ols = model(p);
					String ac = ols.crossValidation(train, 0, 10, null, false, false, false);
					accuracies[i] = QSRRModelRI.accuracy(measure(), ac);
					featuresSets[i] = newFeatures;
				} catch (Throwable e) {
					e.printStackTrace();
					accuracies[i] = Float.MAX_VALUE;
					featuresSets[i] = null;
				}
			}
		});
		float bestAccuracy = Float.MAX_VALUE;
		int bestI = -1;
		for (int i = 0; i < gen.getNumFeatures(); i++) {
			if (accuracies[i] < bestAccuracy) {
				bestAccuracy = accuracies[i];
				bestI = i;
			}
		}
		this.setSelectedDescritptors(featuresSets[bestI]);
	}

}
