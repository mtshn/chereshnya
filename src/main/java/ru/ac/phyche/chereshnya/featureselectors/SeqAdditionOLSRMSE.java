package ru.ac.phyche.chereshnya.featureselectors;

import ru.ac.phyche.chereshnya.featuregenerators.PreprocessedFeaturesGenerator;
import ru.ac.phyche.chereshnya.models.ModelRI;
import ru.ac.phyche.chereshnya.models.OLSRI;
import ru.ac.phyche.chereshnya.models.QSRRModelRI.AccuracyMeasure;

public class SeqAdditionOLSRMSE extends SeqAdditionBestLoss {

	@Override
	public ModelRI model(PreprocessedFeaturesGenerator p) {
		return new OLSRI(p);
	}

	@Override
	public AccuracyMeasure measure() {
		return AccuracyMeasure.RMSE;
	}

}
