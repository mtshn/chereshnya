package ru.ac.phyche.chereshnya.featureselectors;

import ru.ac.phyche.chereshnya.ArUtls;
import smile.data.DataFrame;
import smile.data.formula.Formula;
import smile.regression.GradientTreeBoost;

public class BorutaBoosting extends Boruta {
	public BorutaBoosting(int nRounds) {
		super(nRounds);
	}

	@Override
	public float[] featureImportance(float[][] featuresWithShadow, float[] labels) {
		DataFrame trainSet = ArUtls.toDataFrame(featuresWithShadow, labels);
		GradientTreeBoost gb = GradientTreeBoost.fit(Formula.lhs("label"), trainSet);
		double[] impDouble = gb.importance();
		return ArUtls.toFloatArray(impDouble);
	}
}
