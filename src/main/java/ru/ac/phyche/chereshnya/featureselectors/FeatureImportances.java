package ru.ac.phyche.chereshnya.featureselectors;

import java.util.ArrayList;
import java.util.HashMap;

import ru.ac.phyche.chereshnya.ArUtls;

public interface FeatureImportances {

	public static class FeatureImportance implements Comparable<FeatureImportance> {
		public String name;
		public float value;
		public float stdev = 0;

		@Override
		public int compareTo(FeatureImportance o) {
			return ((Float) value).compareTo(o.value);
		}
	}

	public abstract FeatureImportance[] importances();

	public static FeatureImportance[] averageImportances(ArrayList<FeatureImportance[]> x) {
		HashMap<String, ArrayList<Float>> imp = new HashMap<String, ArrayList<Float>>();

		for (FeatureImportance[] x1 : x) {
			for (int i = 0; i < x1.length; i++) {
				ArrayList<Float> l = imp.get(x1[i].name);
				if (l == null) {
					l = new ArrayList<Float>();
				}
				l.add(x1[i].value);
				imp.put(x1[i].name, l);
			}
		}
		FeatureImportance[] result = new FeatureImportance[imp.size()];
		int i = 0;
		for (String name : imp.keySet()) {
			result[i] = new FeatureImportance();
			result[i].name = name;
			float[] a0 = ArUtls.toFloatArray(imp.get(name));
			float[] a1 = new float[x.size()];
			for (int j = 0; j < a0.length; j++) {
				a1[j] = a0[j];
			}
			result[i].value = ArUtls.mean(a1);
			result[i].stdev = ArUtls.stdev(a1);
			i++;
		}
		return result;
	}

}
