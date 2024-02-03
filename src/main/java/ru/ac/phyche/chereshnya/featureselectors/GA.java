package ru.ac.phyche.chereshnya.featureselectors;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

import ru.ac.phyche.chereshnya.ChemDataset;
import ru.ac.phyche.chereshnya.PythonRunner;
import ru.ac.phyche.chereshnya.featuregenerators.FeaturesGenerator;
import ru.ac.phyche.chereshnya.featuregenerators.PreprocessedFeaturesGenerator;
import ru.ac.phyche.chereshnya.featurepreprocessors.FeaturesPreprocessor;
import ru.ac.phyche.chereshnya.featurepreprocessors.Scale01FeaturesPreprocessor;
import ru.ac.phyche.chereshnya.featurepreprocessors.SelectedFeaturesPreprocessor;
import ru.ac.phyche.chereshnya.featureselectors.FeatureImportances.FeatureImportance;

public class GA extends FeatureSelector implements FeatureImportances {
	ArrayList<FeatureImportance> importancesAL = new ArrayList<FeatureImportance>();

	int nGenerations = 80;

	public GA(int nGen) {
		nGenerations = nGen;
	}

	@Override
	public FeaturesGenerator select(ChemDataset train, FeaturesGenerator gen, int n) {
		try {
			Scale01FeaturesPreprocessor scale = new Scale01FeaturesPreprocessor();
			scale.train(gen, train);
			FeaturesGenerator gen1 = new PreprocessedFeaturesGenerator(gen, scale);
			gen1.precompute(train);

			float[][] features = gen1.features(train);
			FileWriter fw1 = new FileWriter("n_desc_ga.tmp");
			fw1.write(n + "\n");
			fw1.close();
			fw1 = new FileWriter("generations_ga.tmp");
			fw1.write(nGenerations + "\n");
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

			PythonRunner.runPython("ga.py", "");

			BufferedReader br = new BufferedReader(new FileReader("ga_desc.txt"));
			String s = br.readLine();
			ArrayList<Integer> bestFeatures = new ArrayList<Integer>();
			int c1 = 0;
			while (s != null) {
				if (!s.trim().equals("")) {
					float t = (float) Double.parseDouble(s.trim());
					if (t > 0.5f) {
						bestFeatures.add(c1);
					}
					c1 = c1 + 1;
				}
				s = br.readLine();
			}
			br.close();
			
			for (Integer i : bestFeatures) {
				FeatureImportance e = new FeatureImportance();
				e.name = gen.getName(i);
				e.value = 1;
				importancesAL.add(e);
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
		return importancesAL.toArray(new FeatureImportance[importancesAL.size()]);
	}

}
