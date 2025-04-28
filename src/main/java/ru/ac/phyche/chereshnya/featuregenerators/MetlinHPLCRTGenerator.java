package ru.ac.phyche.chereshnya.featuregenerators;

import java.util.HashSet;

import ru.ac.phyche.hplc_metlin_predict.PredictHPLC.Ensemble;

public class MetlinHPLCRTGenerator extends FeaturesGenerator {

	String[] names = new String[] { "RT_METLIN", "RT2_METLIN", "RT3_METLIN", "RT0.5_METLIN" };
	Ensemble modelHPLC = null;

	private void loadModelIfNull() {
		try {
			if (modelHPLC == null) {
				modelHPLC = Ensemble.load("./model_hplc/");
			}
		} catch (Exception e) {
			e.printStackTrace();
			new RuntimeException(e.getMessage());
		}
	}

	@Override
	public void precompute(HashSet<String> smilesStrings) {
		loadModelIfNull();
		String[] s = smilesStrings.toArray(new String[smilesStrings.size()]);
		float[] rt = modelHPLC.predictMany(s);
		for (int i = 0; i < s.length; i++) {
			float x = rt[i] / 60f;
			this.putPrecomputed(s[i], new float[] { x, x * x, x * x * x, (float) Math.sqrt(x) });
		}
	}

	@Override
	public String getName(int i) {
		return names[i];
	}

	@Override
	public int getNumFeatures() {
		return names.length;
	}

}
