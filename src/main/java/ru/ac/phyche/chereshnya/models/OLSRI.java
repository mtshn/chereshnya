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

	private LinearModel mdl = null;

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

	private LinearModel train(float[][] trainFeatures, float[] labels) throws IOException {
		for (int j = 0; j < trainFeatures[0].length; j++) {// Adding noise to constant columns!!!!
			float x = trainFeatures[0][j];
			boolean isconstant = true;
			for (int i = 0; i < trainFeatures.length; i++) {
				if (Math.abs(trainFeatures[i][j] - x) > 1E-5F) {
					isconstant = false;
				}
			}
			if (isconstant) {
				int rnd = (int) Math.round(Math.random() * trainFeatures.length);
				trainFeatures[rnd][j] += 2E-5;
			}
		}
		DataFrame dataFrame = ArUtls.toDataFrame(trainFeatures, ArUtls.mult(0.001F, labels));
		try {
			LinearModel m = OLS.fit(Formula.lhs("label"), dataFrame);
			return m;
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
			LinearModel b = this.train(trainFeatures, trainLabels);
			this.mdl = b;
		} catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException(e.getMessage());
		}
	}

	@Override
	public float[] predict(String[] smiles) {
		float[][] features = features(smiles);
		float[] predictions = ArUtls.mult(1000, ArUtls.toFloatArray(mdl.predict(ArUtls.toDataFrame(features))));
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
			throw new RuntimeException("Wrong model type");
		}
		br = new BufferedReader(new FileReader(new File(directory, "info.txt")));
		File f = new File(directory, "model.xml");
		XStream xstream = new XStream();
		xstream.allowTypes(new String[] { "smile.regression.LinearModel", "smile.data.formula.Variable",
				"smile.data.type.StructField", "smile.data.type.DoubleType" });
		mdl = (LinearModel) xstream.fromXML(f);
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

	public LinearModel getLM() {
		return mdl;
	}

	public float[] modelCoefficientsWithB() {
		double[] bw = mdl.coefficients();
		return ArUtls.mult(1000, ArUtls.toFloatArray(bw));
	}

	public float[] modelCoefficientsWithoutB() {
		double[] bw = mdl.coefficients();
		float[] result = new float[bw.length - 1];
		for (int i = 0; i < result.length; i++) {
			result[i] = (float) (1000 * bw[i + 1]);
		}
		return result;
	}

	public float modelB() {
		return (float) (1000 * mdl.coefficients()[0]);
	}

	public float modelF() {
		return (float) (mdl.ftest());
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
}

//scaled=((val-min)/(max-min))

// coeff1*scaled = (coeff1/(max-min))  * val   - (coeff1*min/(max-min))