package ru.ac.phyche.chereshnya.featureselectors;

import java.util.ArrayList;
import java.util.Arrays;

import ru.ac.phyche.chereshnya.ArUtls;
import ru.ac.phyche.chereshnya.ChemDataset;
import ru.ac.phyche.chereshnya.featuregenerators.FeaturesGenerator;
import ru.ac.phyche.chereshnya.featuregenerators.PreprocessedFeaturesGenerator;
import ru.ac.phyche.chereshnya.featurepreprocessors.Scale01FeaturesPreprocessor;
import ru.ac.phyche.chereshnya.featurepreprocessors.SelectedFeaturesPreprocessor;
import ru.ac.phyche.chereshnya.featureselectors.FeatureImportances.FeatureImportance;
import ru.ac.phyche.chereshnya.models.ModelRI;
import ru.ac.phyche.chereshnya.models.OLSRI;
import ru.ac.phyche.chereshnya.models.QSRRModelRI;

public class SeqAdditionBestF extends FeatureSelector.SeqAddition implements FeatureImportances {

	ArrayList<FeatureImportance> importancesAL = new ArrayList<FeatureImportance>();

	@Override
	public void addNext(ChemDataset train, FeaturesGenerator gen, int n) {
		float[] fscores = new float[gen.getNumFeatures()];
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
				fscores[i] = Float.MIN_VALUE;
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
					OLSRI ols = new OLSRI(p);
					ols.train(train, ChemDataset.empty());
					fscores[i] = ols.modelF();
					featuresSets[i] = newFeatures;
				} catch (Throwable e) {
					e.printStackTrace();
					fscores[i] = Float.MIN_VALUE;
					featuresSets[i] = null;
				}
			}
		});
		float bestF = Float.MIN_VALUE;
		int bestI = -1;
		for (int i = 0; i < gen.getNumFeatures(); i++) {
			if (fscores[i] > bestF) {
				bestF = fscores[i];
				bestI = i;
			}
		}
		System.out.println(bestF);
		FeatureImportance e = new FeatureImportance();
		e.name = gen.getName(bestI);
		e.value = 1;
		importancesAL.add(e);
		this.setSelectedDescritptors(featuresSets[bestI]);
	}

	@Override
	public FeatureImportance[] importances() {
		return importancesAL.toArray(new FeatureImportance[importancesAL.size()]);
	}
}
