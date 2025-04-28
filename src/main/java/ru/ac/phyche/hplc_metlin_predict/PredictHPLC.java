package ru.ac.phyche.hplc_metlin_predict;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;

public class PredictHPLC {
	public static float[] relu(float[] x) {
		float[] r = new float[x.length];
		for (int i = 0; i < x.length; i++) {
			if (x[i] > 0) {
				r[i] = x[i];
			} else {
				r[i] = 0f;
			}
		}
		return r;
	}

	public static float[] leakyRelu02(float[] x) {
		float[] r = new float[x.length];
		for (int i = 0; i < x.length; i++) {
			if (x[i] > 0) {
				r[i] = x[i];
			} else {
				r[i] = 0.2f * x[i];
			}
		}
		return r;
	}

	public static float[] dense(float[] input, float[][] weight, float[] bias) {
		float[] r = new float[bias.length];
		if (r.length != weight.length) {
			throw new RuntimeException("Dimensions mismatch");
		}
		if (input.length != weight[0].length) {
			throw new RuntimeException("Dimensions mismatch");
		}
		for (int i = 0; i < r.length; i++) {
			float x = bias[i];
			for (int j = 0; j < input.length; j++) {
				x = x + input[j] * weight[i][j];
			}
			r[i] = x;
		}
		return r;
	}

	public static float[] dense(float[] input, float[][] weight) {
		float[] r = new float[weight.length];
		if (input.length != weight[0].length) {
			throw new RuntimeException("Dimensions mismatch");
		}
		for (int i = 0; i < r.length; i++) {
			float x = 0;
			for (int j = 0; j < input.length; j++) {
				x = x + input[j] * weight[i][j];
			}
			r[i] = x;
		}
		return r;
	}

	public static float alpha(float[] a, float[] x) {
		if (a.length != x.length) {
			throw new RuntimeException("Dimensions mismatch");
		}
		float r = 0;
		for (int i = 0; i < a.length; i++) {
			r = r + a[i] * x[i];
		}
		return r;
	}

	public static float[][] transpose(float[][] x) {
		float[][] y = new float[x[0].length][];
		for (int i = 0; i < y.length; i++) {
			y[i] = new float[x.length];
			for (int j = 0; j < x.length; j++) {
				y[i][j] = x[j][i];
			}
		}
		return y;
	}

	public static float[] subset(float[] x, int start, int finish) {
		float[] r = new float[finish - start];
		for (int i = 0; i < r.length; i++) {
			r[i] = x[start + i];
		}
		return r;
	}

	public static float[] sumPerDimension(float[][] x) {
		float[] r = new float[x[0].length];
		for (int i = 0; i < r.length; i++) {
			for (int j = 0; j < x.length; j++) {
				r[i] = r[i] + x[j][i];
			}
		}
		return r;
	}

	public static float[] sumPerDimension(float alphas[], float[][] x) {
		if (alphas.length != x.length) {
			throw new RuntimeException("Dimensions mismatch");
		}
		float[] r = new float[x[0].length];
		for (int i = 0; i < r.length; i++) {
			for (int j = 0; j < x.length; j++) {
				r[i] = r[i] + alphas[j] * x[j][i];
			}
		}
		return r;
	}

	public static float[] gat(float[] features, float[][] neighFeatures, float[][] bondFeatures, int nOutNodesPerHead,
			int nHeads, float[][] weightJ, float[][] weightI, float[][] weightE, float[] biasI, float[] bias,
			float[][] a) {
		if (nOutNodesPerHead * nHeads != weightI.length) {
			throw new RuntimeException("Dimensions mismatch");
		}
		if (nOutNodesPerHead * nHeads != weightE.length) {
			throw new RuntimeException("Dimensions mismatch");
		}
		if (nOutNodesPerHead * nHeads != weightJ.length) {
			throw new RuntimeException("Dimensions mismatch");
		}
		if (neighFeatures.length != bondFeatures.length) {
			throw new RuntimeException("Dimensions mismatch");
		}
		float[] iWx = dense(features, weightI, biasI);
		float[][] jWx = new float[neighFeatures.length][];
		for (int i = 0; i < jWx.length; i++) {
			jWx[i] = dense(neighFeatures[i], weightJ);
		}
		float[][] eWx = new float[bondFeatures.length][];
		for (int i = 0; i < eWx.length; i++) {
			eWx[i] = dense(bondFeatures[i], weightE);
		}

		float[][] alphas = new float[nHeads][];
		float[] result = new float[nOutNodesPerHead * nHeads];
		for (int i = 0; i < nHeads; i++) {
			alphas[i] = new float[neighFeatures.length];
			float sum = 0;
			float[] iWx1 = subset(iWx, i * nOutNodesPerHead, nOutNodesPerHead * (i + 1));
			float[][] jWx1 = new float[neighFeatures.length][];
			for (int j = 0; j < neighFeatures.length; j++) {
				jWx1[j] = subset(jWx[j], i * nOutNodesPerHead, nOutNodesPerHead * (i + 1));
				float[] eWx1 = subset(eWx[j], i * nOutNodesPerHead, nOutNodesPerHead * (i + 1));
				float[] lr = leakyRelu02(sumPerDimension(new float[][] { iWx1, jWx1[j], eWx1 }));
				float alpha = alpha(a[i], lr);
				alphas[i][j] = (float) Math.exp(alpha);
				sum = sum + alphas[i][j];
			}
			for (int j = 0; j < neighFeatures.length; j++) {
				alphas[i][j] = alphas[i][j] / sum;
			}

			float[] r = sumPerDimension(alphas[i], jWx1);
			if (r.length != nOutNodesPerHead) {
				throw new RuntimeException("Dimensions mismatch");
			}
			for (int j = 0; j < r.length; j++) {
				result[nOutNodesPerHead * i + j] = r[j] + bias[nOutNodesPerHead * i + j];
			}
		}
		return relu(result);
	}

	public static GNNChemUtils.GraphMol gat(GNNChemUtils.GraphMol gm, int nOutNodesPerHead, int nHeads, float[][] weightJ,
			float[][] weightI, float[][] weightE, float[] biasI, float[] bias, float[][] a) {
		GNNChemUtils.GraphMol result = gm.copy();
		for (int i = 0; i < result.getNNodes(); i++) {
			float[] features = gm.getFeaturesOfNode(i);
			float[][] featuresNeighNodes = gm.featuresOfNeighborNodes(i);
			float[][] featuresEdges = gm.featuresOfNeighborEdges(i);
			float[] r = gat(features, featuresNeighNodes, featuresEdges, nOutNodesPerHead, nHeads, weightJ, weightI,
					weightE, biasI, bias, a);
			result.setFeaturesOfNode(i, r);
		}
		return result;
	}

	public static float[] loadBias(String filename) throws IOException {
		BufferedReader br = new BufferedReader(new FileReader(filename));
		ArrayList<Float> f = new ArrayList<Float>();
		String s = br.readLine();
		while (s != null) {
			if (!s.trim().equals("")) {
				f.add(Float.parseFloat(s.trim()));
			}
			s = br.readLine();
		}
		br.close();
		float[] r = new float[f.size()];
		for (int i = 0; i < r.length; i++) {
			r[i] = f.get(i);
		}
		return r;
	}

	public static float[][] loadWeight(String filename) throws IOException {
		BufferedReader br = new BufferedReader(new FileReader(filename));
		ArrayList<float[]> f = new ArrayList<float[]>();
		String s = br.readLine();
		while (s != null) {
			if (!s.trim().equals("")) {
				String[] sp = s.trim().split("\\s+");
				float[] f1 = new float[sp.length];
				for (int i = 0; i < f1.length; i++) {
					f1[i] = Float.parseFloat(sp[i]);
				}
				f.add(f1);
			}
			s = br.readLine();
		}
		br.close();
		float[][] r = new float[f.size()][];
		for (int i = 0; i < r.length; i++) {
			r[i] = f.get(i);
		}
		return r;
	}

	public static float[][] loadA(String filename) throws IOException {
		return transpose(loadWeight(filename));
	}

	public static void printFirstLast10(float[] x) {
		for (int i = 0; i < 10; i++) {
			System.out.print(x[i] + " ");
		}
		System.out.print(".......");
		for (int i = 0; i < 10; i++) {
			System.out.print(x[x.length - 10 + i] + " ");
		}
		System.out.println();
	}

	public static class GATLayer {
		float[][] weightE;
		float[][] weightI;
		float[][] weightJ;
		float[] biasI;
		float[] bias;
		float[][] a;
		int nOutPerHead = 100;
		int nHeads = 4;

		public static GATLayer load(String prefix, int nOutPerHead, int nHeads) throws IOException {
			GATLayer r = new GATLayer();
			r.weightE = loadWeight(prefix + "denseE_weight.txt");
			r.weightI = loadWeight(prefix + "denseI_weight.txt");
			r.weightJ = loadWeight(prefix + "denseJ_weight.txt");
			r.biasI = loadBias(prefix + "denseI_bias.txt");
			r.bias = loadBias(prefix + "bias.txt");
			r.a = loadA(prefix + "a.txt");
			r.nOutPerHead = nOutPerHead;
			r.nHeads = nHeads;
			return r;
		}

		public GNNChemUtils.GraphMol forward(GNNChemUtils.GraphMol gm) {
			return gat(gm, nOutPerHead, nHeads, weightJ, weightI, weightE, biasI, bias, a);
		}
	}

	public static class ModelGat3Dense2_400 {
		GATLayer[] layers = new GATLayer[3];
		float[][] dense1W;
		float[][] dense2W;
		float[] dense1Bias;
		float[] dense2Bias;

		public static ModelGat3Dense2_400 load(String prefix) throws IOException {
			ModelGat3Dense2_400 r = new ModelGat3Dense2_400();
			for (int i = 0; i < 3; i++) {
				r.layers[i] = GATLayer.load(prefix + "gat" + (i + 1) + "_", 100, 4);
			}
			r.dense1W = loadWeight(prefix + "dense1_weight.txt");
			r.dense2W = loadWeight(prefix + "dense2_weight.txt");
			r.dense1Bias = loadBias(prefix + "dense1_bias.txt");
			r.dense2Bias = loadBias(prefix + "dense2_bias.txt");
			return r;
		}

		public float forward(GNNChemUtils.GraphMol gm) {
			GNNChemUtils.GraphMol gm2 = gm.copy();
			for (int i = 0; i < 3; i++) {
				gm2 = layers[i].forward(gm2);
			}
			float[][] x = gm2.getNodeFeatures();
			float[] x1 = sumPerDimension(x);
			x1 = dense(x1, dense1W, dense1Bias);
			x1 = relu(x1);
			x1 = dense(x1, dense2W, dense2Bias);
			return 1000 * x1[0];
		}
	}

	public static class Ensemble {
		ModelGat3Dense2_400[] models = new ModelGat3Dense2_400[5];

		public static Ensemble load(String prefix) throws IOException {
			Ensemble r = new Ensemble();
			for (int i = 0; i < 5; i++) {
				r.models[i] = ModelGat3Dense2_400.load(prefix + "model" + (i + 1) + "_");
			}
			return r;
		}

		public float[] forward(GNNChemUtils.GraphMol gm) {
			float[] result = new float[5];
			for (int i = 0; i < result.length; i++) {
				result[i] = models[i].forward(gm);
			}
			return result;
		}

		public float predictEnsemble(String smiles) {
			GNNChemUtils.GraphMol gm = null;
			try {
				gm = GNNChemUtils.molToMolGraph(smiles, "", true);
			} catch (Throwable e) {
				return Float.NaN;
			}
			float[] ensemblePredict = forward(gm);
			float result = 0;
			for (int i = 0; i < ensemblePredict.length; i++) {
				result = result + ensemblePredict[i] / ensemblePredict.length;
			}
			return result;
		}

		public float[] predictMany(String[] smiles) {
			float[] results = new float[smiles.length];
			Arrays.stream(intsrnd(smiles.length)).parallel().forEach(i -> {
				results[i] = this.predictEnsemble(smiles[i]);
			});
			return results;
		}

	}

	public static int[] intsrnd(int n) {
		int[] a = new int[n];
		for (int i = 0; i < n; i++) {
			a[i] = i;
		}
		Random rnd = new Random();
		for (int i = 0; i < n; i++) {
			int x = a[i];
			int j = rnd.nextInt(n);
			int y = a[j];
			a[i] = y;
			a[j] = x;
		}
		return a;
	}
}
