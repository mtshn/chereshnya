package ru.ac.phyche.chereshnya.featureselectors;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;

import ru.ac.phyche.chereshnya.ChemDataset;
import ru.ac.phyche.chereshnya.PythonRunner;
import ru.ac.phyche.chereshnya.featuregenerators.FeaturesGenerator;
import ru.ac.phyche.chereshnya.featuregenerators.PreprocessedFeaturesGenerator;
import ru.ac.phyche.chereshnya.featurepreprocessors.FeaturesPreprocessor;
import ru.ac.phyche.chereshnya.featurepreprocessors.Scale01FeaturesPreprocessor;
import ru.ac.phyche.chereshnya.featurepreprocessors.SelectedFeaturesPreprocessor;

public class PLSVIP extends FeatureSelector implements FeatureImportances {
	private FeatureImportance[] importances;

	int nComp = 20;

	public PLSVIP() {

	}

	public PLSVIP(int nComp_) {
		nComp = nComp_;
	}

	@Override
	public FeaturesGenerator select(ChemDataset train, FeaturesGenerator gen, int n) {
		try {
			Scale01FeaturesPreprocessor scale = new Scale01FeaturesPreprocessor();
			scale.train(gen, train);
			FeaturesGenerator gen1 = new PreprocessedFeaturesGenerator(gen, scale);
			gen1.precompute(train);

			float[][] features = gen1.features(train);
			FileWriter fw1 = new FileWriter("n_comp_pls_vip.tmp");
			fw1.write(nComp+"\n");
			fw1.close();
			FileWriter fw2 = new FileWriter("descriptorsri.tmp.csv");

			fw2.write("RI,");
			for (int i = 0; i < features[0].length; i++) {
				fw2.write(gen.getName(i) + ((i == features[0].length - 1) ? "\n" : ","));
			}

			for (int i = 0; i < features.length; i++) {
				fw2.write(train.getRetention(i) + ",");
				for (int j = 0; j < features[0].length; j++) {
					fw2.write(features[i][j] + ((j == features[0].length - 1) ? "\n" : ","));
				}
			}
			fw2.close();
			
			PythonRunner.runPython("plsvip.py", "");
			
			float[] importances = new float[gen.getNumFeatures()];
			BufferedReader br = new BufferedReader(new FileReader("vip_score.txt"));
			String s = br.readLine();
			int c = 0;
			while (s != null) {
				if (!s.trim().equals("")) {
					importances[c] = (float) Double.parseDouble(s.trim());
					c = c + 1;
				}
				s = br.readLine();
			}
			br.close();

			FeatureImportances.FeatureImportance[] imp = new FeatureImportances.FeatureImportance[gen.getNumFeatures()];
			HashMap<String, Integer> map = new HashMap<String, Integer>();
			for (int i = 0; i < gen.getNumFeatures(); i++) {
				imp[i] = new FeatureImportances.FeatureImportance();
				imp[i].name = gen.getName(i);
				imp[i].value = importances[i];
				map.put(imp[i].name, i);
			}
			Arrays.sort(imp);
			this.importances=imp;
			int[] bestFeatures = new int[n];
			for (int i = 0; i < n; i++) {
				bestFeatures[i] = map.get(imp[imp.length - i - 1].name);
			}

			FeaturesPreprocessor select = new SelectedFeaturesPreprocessor(bestFeatures);
			select.train(gen, train);
			return new PreprocessedFeaturesGenerator(gen, select);
		} catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException(e.getMessage());
		}
	}

	@Override
	public FeatureImportance[] importances() {
		return importances;
	}

}
