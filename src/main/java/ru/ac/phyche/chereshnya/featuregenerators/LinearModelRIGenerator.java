package ru.ac.phyche.chereshnya.featuregenerators;

import java.io.IOException;
import java.util.HashSet;

import ru.ac.phyche.chereshnya.WAXRILinear;
import ru.ac.phyche.chereshnya.models.ModelRI;

public class LinearModelRIGenerator extends FeaturesGenerator {

	ModelRI m1 = null;
	ModelRI m2 = null;

	private void loadModelIfNull() {
		try {
			if (m1 == null) {
				m1 = WAXRILinear.load("ri_linear_model_nonpolar");
			}
			if (m2 == null) {
				m2 = WAXRILinear.load("ri_linear_model_polar");
			}
		} catch (IOException e) {
			e.printStackTrace();
			new RuntimeException(e.getMessage());
		}

	}

	@Override
	public void precompute(HashSet<String> smilesStrings) {
		loadModelIfNull();
		String[] s = smilesStrings.toArray(new String[smilesStrings.size()]);
		float[] a = m1.predict(s);
		float[] b = m2.predict(s);
		for (int i = 0; i < s.length; i++) {
			this.putPrecomputed(s[i], new float[] { a[i], b[i] });
		}
	}

	@Override
	public String getName(int i) {
		if (i == 0) {
			return "RI_Siloxane_LM";
		}
		if (i == 1) {
			return "RI_PEG_LM";
		}
		return null;
	}

	@Override
	public int getNumFeatures() {
		return 2;
	}

}
