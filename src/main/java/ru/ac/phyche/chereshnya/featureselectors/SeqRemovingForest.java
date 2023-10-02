package ru.ac.phyche.chereshnya.featureselectors;

import ru.ac.phyche.chereshnya.ArUtls;
import smile.data.DataFrame;
import smile.data.formula.Formula;
import smile.regression.RandomForest;

public class SeqRemovingForest extends SeqRemovingLessImportant {

	public SeqRemovingForest(int removeDescriptorsAtOnce) {
		super(removeDescriptorsAtOnce);
	}

	@Override
	public float[] featureImportance(float[][] featuresWithShadow, float[] labels) {
		DataFrame trainSet = ArUtls.toDataFrame(featuresWithShadow, labels);
		RandomForest rf = RandomForest.fit(Formula.lhs("label"), trainSet);
		double[] impDouble = rf.importance();
		return ArUtls.toFloatArray(impDouble);
	}


}
