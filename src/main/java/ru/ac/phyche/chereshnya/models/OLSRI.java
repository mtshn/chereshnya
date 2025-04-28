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
import smile.regression.LinearModel;
import smile.regression.OLS;

public class OLSRI extends QSRRModelRI implements LinearModelRI {

	private static class ModelWithPreproc {
		public LinearModel m = null;
		public boolean[] isColumnConst = null;
	}

	private ModelWithPreproc mdl = null;

	public OLSRI(FeaturesGenerator gen) {
		super(gen);
	}

	public OLSRI(FeaturesGenerator gen, boolean hyperparamsTune, String hyperparamsTuneFile,
			int hyperparamsTuneAttempts) {
		super(gen, hyperparamsTune, hyperparamsTuneFile, hyperparamsTuneAttempts);
	}

	private float[][] features(ChemDataset set) {
		return features(set.allSmiles());
	}

	private float[][] features(String[] smiles) {
		getGen().precompute(smiles);
		float[][] features = getGen().features(smiles);
		return features;
	}

	private ModelWithPreproc train(float[][] trainFeatures, float[] labels) throws IOException {
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
			result.m = OLS.fit(Formula.lhs("label"), dataFrame);
			return result;
		} catch (Throwable e) {
			e.printStackTrace();
			return null;
		}
	}

	@Override
	public void train(ChemDataset trainSet, ChemDataset validationSet) {
		try {
			float[][] trainFeatures = this.features(trainSet);
//			float[][] validationFeatures = this.features(validationSet);
			float[] trainLabels = trainSet.allRetentions();
//			float[] validationLabels = validationSet.allRetentions();
			ModelWithPreproc b = this.train(trainFeatures, trainLabels);
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
			br.close();
			throw new RuntimeException("Wrong model type");
		}
		br = new BufferedReader(new FileReader(new File(directory, "info.txt")));
		File f = new File(directory, "model.xml");
		XStream xstream = new XStream();
		xstream.allowTypes(new String[] { "ru.ac.phyche.chereshnya.models.OLSRI.ModelWithPreproc",
				"smile.regression.LinearModel", "smile.data.formula.Variable", "smile.data.type.StructField",
				"smile.data.type.DoubleType" });
		mdl = (ModelWithPreproc) xstream.fromXML(f);
		br.close();
	}

	@Override
	public String modelType() {
		return "OLS";
	}

	@Override
	public String fullModelInfo() {
		return "OLS";
	}

	@Override
	public ModelRI createSimilar() {
		OLSRI result = (OLSRI) QSRRModelRI.getModel(this.modelType(), this.getGen(), this.isHyperparamsTune(),
				this.getHyperparamsTuneFile(), this.getHyperparamsTuneAttempts());
		return result;
	}

	public float[] modelCoefficientsStdDevsWithoutB() {
		double[][] dev = mdl.m.ttest();
		float[] result = new float[mdl.isColumnConst.length];
		int k = 1;
		for (int i = 0; i < result.length; i++) {
			if (!mdl.isColumnConst[i]) {
				result[i] = (float) dev[k][1];
				k = k + 1;
			} else {
				result[i] = 0;
			}
		}
		return ArUtls.mult(1000, result);
	}

	public float modelBStd() {
		double[][] dev = mdl.m.ttest();
		return 1000 * (float) dev[0][1];
	}

	public float[] modelCoefficientsWithoutB() {
		double[] bw = mdl.m.coefficients();
		float[] result = new float[mdl.isColumnConst.length];
		int k = 1;
		for (int i = 0; i < result.length; i++) {
			if (!mdl.isColumnConst[i]) {
				result[i] = (float) bw[k];
				k = k + 1;
			} else {
				result[i] = 0;
			}
		}
		return ArUtls.mult(1000, result);
	}

	public float modelB() {
		return (float) (1000 * mdl.m.coefficients()[0]);
	}

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

	public float[] unscaledModelCoefficientsWithBStDevs(float[] minValuesDescriptors, float[] maxValuesDescriptors) {
		float[] ws = this.modelCoefficientsStdDevsWithoutB();
		float[] scales = ArUtls.mult(-1, minValuesDescriptors);
		scales = ArUtls.ewAdd(scales, maxValuesDescriptors);
		ws = ArUtls.ewDiv(ws, scales);

		float[] bws = ArUtls.mergeArrays(new float[] { modelBStd() }, ws);
		return bws;
	}

	public float getFTest() {
		return (float) this.mdl.m.ftest();
	}

	public float modelF() {
		return (float) this.mdl.m.ftest();
	}

	public float getPValue() {
		return (float) this.mdl.m.pvalue();
	}

}

//scaled=((val-min)/(max-min))

// coeff1*scaled = (coeff1/(max-min))  * val   - (coeff1*min/(max-min))