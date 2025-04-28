package ru.ac.phyche.chereshnya.featuregenerators;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.HashMap;
import java.util.HashSet;

import ru.ac.phyche.chereshnya.ChemUtils;

public class PrecomputedFeaturesFromFileGenerator extends FeaturesGenerator {

	private HashMap<String, float[]> data0 = new HashMap<String, float[]>();
	private String[] names = null;

	public PrecomputedFeaturesFromFileGenerator(String file) {
		try {
			BufferedReader br = new BufferedReader(new FileReader(file));
			String s = br.readLine();
			String[] splt0 = s.split("\\s+");
			if (!splt0[0].equals("SMILES")) {
				br.close();
				throw new RuntimeException("Wrong file format");
			}
			int nDesc = splt0.length - 1;
			names = new String[nDesc];
			for (int i = 0; i < nDesc; i++) {
				names[i] = splt0[i + 1];
			}
			s = br.readLine();
			while (s != null) {
				String[] splt = s.split("\\s+");
				if (splt.length != nDesc + 1) {
					br.close();
					throw new RuntimeException("Wrong file format");
				}
				String smiles = ChemUtils.canonical(splt[0], false);
				smiles = ChemUtils.canonical(smiles, false);
				smiles = ChemUtils.canonical(smiles, false);
				float[] d = new float[nDesc];
				for (int i = 0; i < nDesc; i++) {
					d[i] = Float.parseFloat(splt[i + 1]);
				}
				data0.put(smiles, d);
				s = br.readLine();
			}
			br.close();
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException(e.getMessage());
		}

	}

	@Override
	public void precompute(HashSet<String> smilesStrings) {
		try {
			for (String s : smilesStrings) {
				String smiles = ChemUtils.canonical(s, false);
				smiles = ChemUtils.canonical(smiles, false);
				smiles = ChemUtils.canonical(smiles, false);
				float[] d = this.data0.get(smiles);
				if (d == null) {
					throw new RuntimeException(
							"No descriptor values in the file with precomputed decriptors for this molecule! "+s);
				}
				this.putPrecomputed(s, d);
			}
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException(e.getMessage());
		}
	}

	@Override
	public String getName(int i) {
		return this.names[i];
	}

	@Override
	public int getNumFeatures() {
		return this.names.length;
	}

}
