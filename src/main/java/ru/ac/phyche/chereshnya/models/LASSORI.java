package ru.ac.phyche.chereshnya.models;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.StaxDriver;

import ru.ac.phyche.chereshnya.ArUtls;
import ru.ac.phyche.chereshnya.ChemDataset;
import ru.ac.phyche.chereshnya.featuregenerators.FeaturesGenerator;
import smile.data.DataFrame;
import smile.data.formula.Formula;
import smile.regression.LASSO;
import smile.regression.LinearModel;

public class LASSORI extends QSRRModelRI implements LinearModelRI {
	private static final QSRRModelRI.AccuracyMeasure accuracyMeasureTuning = QSRRModelRI.AccuracyMeasure.MDAE;

	private static final float[] l1Range = new float[] { 1E-1f, 1E5f };

	private static class ModelWithPreproc {
		public LinearModel m = null;
		public boolean[] isColumnConst = null;
	}

	ModelWithPreproc mdl = null;

	private float l1 = 0.0001f;

	public LASSORI(FeaturesGenerator gen) {
		super(gen);
	}

	public LASSORI(FeaturesGenerator gen, boolean hyperparamsTune, String hyperparamsTuneFile,
			int hyperparamsTuneAttempts) {
		super(gen, hyperparamsTune, hyperparamsTuneFile, hyperparamsTuneAttempts);
	}

	public LASSORI(FeaturesGenerator gen, float l1) {
		super(gen);
		this.l1 = l1;
	}

	private static class Param {
		float l1 = 0.0001f;

		private static float logrnd(float[] minmax) {
			double log10min = Math.log10(minmax[0]);
			double log10max = Math.log10(minmax[1]);
			double r = log10min + Math.random() * (log10max - log10min);
			r = Math.pow(10, r);
			return (float) r;
		}

		public static Param rnd() {
			Param result = new Param();
			result.l1 = logrnd(l1Range);
			return result;
		}

		@Override
		public String toString() {
			String s = "";
			s = s + "l1: " + l1;
			return s;
		}
	}

	private float[][] features(ChemDataset set) {
		return features(set.allSmiles());
	}

	private float[][] features(String[] smiles) {
		getGen().precompute(smiles);
		float[][] features = getGen().features(smiles);
		return features;
	}

	private ModelWithPreproc train(Param p, float[][] trainFeatures, float[] labels) throws IOException {
		ModelWithPreproc result = new ModelWithPreproc();
		result.isColumnConst = new boolean[trainFeatures[0].length];
		for (int j = 0; j < trainFeatures[0].length; j++) {
			float x = trainFeatures[0][j];
			boolean isconstant = true;
			for (int i = 0; i < trainFeatures.length; i++) {
				if (Math.abs(trainFeatures[i][j] - x) > 1E-3F) {
					isconstant = false;
				}
			}
			if (isconstant) {
				result.isColumnConst[j] = true;
			}
		}

		DataFrame dataFrame = ArUtls.toDataFrame(trainFeatures, ArUtls.mult(0.001F, labels), result.isColumnConst);
		try {
			result.m = LASSO.fit(Formula.lhs("label"), dataFrame, p.l1);
			return result;
		} catch (Throwable e) {
			e.printStackTrace();
			return null;
		}
	}

	private String validate(ModelWithPreproc lm, float[][] features, float[] labels) {
		float[] predictions = ArUtls.mult(1000,
				ArUtls.toFloatArray(lm.m.predict(ArUtls.toDataFrame(features, lm.isColumnConst))));
		String accuracyMeasures = QSRRModelRI.accuracyMeasuresValidation(predictions, labels);
		System.out.println(accuracyMeasures);
		return accuracyMeasures;
	}

	@Override
	public void train(ChemDataset trainSet, ChemDataset validationSet) {
		try {
			float[][] trainFeatures = this.features(trainSet);
			float[][] validationFeatures = this.features(validationSet);
			float[] trainLabels = trainSet.allRetentions();
			float[] validationLabels = validationSet.allRetentions();
			if (isHyperparamsTune()) {
				float bestAccuracy = Float.MAX_VALUE;
				Param bestParams = null;
				FileWriter fw = null;
				if (getHyperparamsTuneFile() != null) {
					fw = new FileWriter(getHyperparamsTuneFile());
				}
				for (int n = 0; n < getHyperparamsTuneAttempts(); n++) {
					Param p = Param.rnd();
					ModelWithPreproc b = this.train(p, trainFeatures, trainLabels);
					if (b != null) {
						String accuracyMeasures = this.validate(b, validationFeatures, validationLabels);
						float accuracy = QSRRModelRI.accuracy(accuracyMeasureTuning, accuracyMeasures);
						if (accuracy < bestAccuracy) {
							bestAccuracy = accuracy;
							bestParams = p;
						}
						if (fw != null) {
							fw.write(modelType() + " " + p.toString() + " " + accuracyMeasures + "\n");
							fw.flush();
						}
					} else {
						fw.write(modelType() + " " + p.toString() + " Training Failed\n");
					}
				}

				if (fw != null) {
					fw.close();
				}
				this.l1 = bestParams.l1;
			}
			Param p = new Param();
			p.l1 = this.l1;
			ModelWithPreproc b = this.train(p, trainFeatures, trainLabels);
			this.mdl = b;
		} catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException(e.getMessage());
		}
	}

	@Override
	public float[] predict(String[] smiles) {
		float[][] features = features(smiles);
		float[] predictions = ArUtls.mult(1000,
				ArUtls.toFloatArray(mdl.m.predict(ArUtls.toDataFrame(features, mdl.isColumnConst))));
		return predictions;
	}

	@Override
	public void save(String directory) throws IOException {
		Files.createDirectories(Paths.get(directory));
		FileWriter fw = new FileWriter(new File(directory, "ModelType.txt"));
		fw.write(this.modelType());
		fw.close();
		fw = new FileWriter(new File(directory, "info.txt"));
		fw.write(l1 + "");
		fw.close();
		fw = new FileWriter(new File(directory, "info1.txt"));
		fw.write(this.fullModelInfo());
		fw.close();
		File f = new File(directory, "model.xml");
		fw = new FileWriter(f);
		XStream xstream = new XStream(new StaxDriver());
		xstream.toXML(mdl, fw);
	}

	@Override
	public void load(String directory) throws IOException {
		BufferedReader br = new BufferedReader(new FileReader(new File(directory, "ModelType.txt")));
		String s = br.readLine();
		while (s.trim().equals("")) {
			s = br.readLine();
		}
		br.close();
		if (!s.trim().equals(this.modelType())) {
			throw new RuntimeException("Wrong model type");
		}
		br = new BufferedReader(new FileReader(new File(directory, "info.txt")));
		s = br.readLine();
		while (s.trim().equals("")) {
			s = br.readLine();
		}
		br.close();
		this.l1 = Float.parseFloat(s);

		File f = new File(directory, "model.xml");
		XStream xstream = new XStream();
		xstream.allowTypes(new String[] { "ru.ac.phyche.chereshnya.models.LASSORI.ModelWithPreproc",
				"smile.regression.LinearModel", "smile.data.formula.Variable", "smile.data.type.StructField",
				"smile.data.type.DoubleType" });
		mdl = (ModelWithPreproc) xstream.fromXML(f);
	}

	@Override
	public String modelType() {
		return "LASSO";
	}

	@Override
	public String fullModelInfo() {
		return "LASSO " + " l1 " + l1;
	}

	@Override
	public ModelRI createSimilar() {
		LASSORI result = (LASSORI) QSRRModelRI.getModel(this.modelType(), this.getGen(), this.isHyperparamsTune(),
				this.getHyperparamsTuneFile(), this.getHyperparamsTuneAttempts());
		result.l1 = this.l1;
		return result;
	}

	public float[] modelCoefficientsWithoutB() {
		double[] w = mdl.m.coefficients();
		float[] result = new float[mdl.isColumnConst.length];
		int k = 0;
		for (int i = 0; i < result.length; i++) {
			if (mdl.isColumnConst[i]) {
				result[i] = 0;
			} else {
				result[i] = (float) w[k];
				k = k + 1;
			}
		}
		return ArUtls.mult(1000, result);
	}

	public float modelB() {
		return (float) (1000 * mdl.m.intercept());
	}

	public void setl1(float l1) {
		this.l1 = l1;
	}

	public float getl1() {
		return l1;
	}

	@Override
	public float[] unscaledModelCoefficientsWithB(float[] minValuesDescriptors, float[] maxValuesDescriptors) {
		float b = 0;
		float[] w = this.modelCoefficientsWithoutB();
		float[] scales = ArUtls.mult(-1, minValuesDescriptors);
		scales = ArUtls.ewAdd(scales, maxValuesDescriptors);
		w = ArUtls.ewDiv(w, scales);
		b = ArUtls.sum(ArUtls.ewMult(ArUtls.mult(-1, minValuesDescriptors), w));

		float[] bw = ArUtls.mergeArrays(new float[] { modelB() + b }, w);
		return bw;
	}

}
