package ru.ac.phyche.chereshnya.featureselectors;

import java.util.ArrayList;

import org.openscience.cdk.exception.CDKException;

import ru.ac.phyche.chereshnya.ChemDataset;
import ru.ac.phyche.chereshnya.featuregenerators.FeaturesGenerator;
import ru.ac.phyche.chereshnya.featuregenerators.PreprocessedFeaturesGenerator;
import ru.ac.phyche.chereshnya.featurepreprocessors.FeaturesPreprocessor;
import ru.ac.phyche.chereshnya.featurepreprocessors.Scale01FeaturesPreprocessor;
import ru.ac.phyche.chereshnya.featurepreprocessors.SelectedFeaturesPreprocessor;
import ru.ac.phyche.chereshnya.featureselectors.FeatureImportances.FeatureImportance;
import ru.ac.phyche.chereshnya.models.LASSORI;

public class LASSONonSequential extends FeatureSelector implements FeatureImportances {

	private FeatureImportance[] importances;

	float l1 = 1;
	float threshold = 0.1f;

	public LASSONonSequential(float l1, float threshold) {
		this.l1 = l1;
		this.threshold = threshold;
	}

	public float getl1() {
		return l1;
	}

	@Override
	public FeaturesGenerator select(ChemDataset train, FeaturesGenerator gen, int n) {
		Scale01FeaturesPreprocessor scale = new Scale01FeaturesPreprocessor();
		scale.train(gen, train);
		PreprocessedFeaturesGenerator p = new PreprocessedFeaturesGenerator(gen, scale);
		LASSORI lasso = new LASSORI(p);
		if (l1 <= 0) {
			lasso.setHyperparamsTune(true);
			lasso.setHyperparamsTuneAttempts(10);
			ChemDataset trainClone = train.copy();
			ChemDataset val;
			try {
				val = trainClone.compoundsBasedSplitAndShuffle(0.1f);
			} catch (CDKException e) {
				throw new RuntimeException(e.getMessage());
			}
			lasso.train(trainClone, val);
			l1 = lasso.getl1();
		} else {
			lasso.setl1(this.l1);
			lasso.setHyperparamsTune(false);
			lasso.train(train, ChemDataset.empty());
		}
		float[] w = lasso.modelCoefficientsWithoutB();
		if (gen.getNumFeatures() != w.length) {
			throw new RuntimeException("LASSO feature selection error");
		}

		ArrayList<Integer> selectedDescriptors = new ArrayList<Integer>();
		ArrayList<FeatureImportance> importancesAL = new ArrayList<FeatureImportance>();
		for (int i = 0; i < w.length; i++) {
			if (w[i] > threshold) {
				FeatureImportance x = new FeatureImportance();
				x.name = gen.getName(i);
				x.value = w[i];
				importancesAL.add(x);
				selectedDescriptors.add(i);
			}
		}
		importances = importancesAL.toArray(new FeatureImportance[importancesAL.size()]);
		int[] selectedDescriptorsA = new int[selectedDescriptors.size()];
		for (int i = 0; i < selectedDescriptorsA.length; i++) {
			selectedDescriptorsA[i] = selectedDescriptors.get(i);
		}
		FeaturesPreprocessor select = new SelectedFeaturesPreprocessor(selectedDescriptorsA);
		select.train(gen, train);
		return new PreprocessedFeaturesGenerator(gen, select);
	}

	@Override
	public FeatureImportance[] importances() {
		return importances;
	}

}
