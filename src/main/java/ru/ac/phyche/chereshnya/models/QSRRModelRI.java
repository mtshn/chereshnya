package ru.ac.phyche.chereshnya.models;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import ru.ac.phyche.chereshnya.featuregenerators.FeaturesGenerator;

public abstract class QSRRModelRI extends ModelRI {
	public static enum AccuracyMeasure {
		MAE, MDAE, RMSE, MDPE, MPE
	};

	private FeaturesGenerator gen = null;
	private boolean hyperparamsTune = false;
	private String hyperparamsTuneFile = null;
	private int hyperparamsTuneAttempts = 0;

	@Override
	public void setTuningOutFileOrDir(String filename) {
		this.setHyperparamsTuneFile(filename);
	}

	public FeaturesGenerator getGen() {
		return gen;
	}

	public String[] getDescriptorNames() {
		return gen.getNames();
	}

	
	public void setGen(FeaturesGenerator gen) {
		this.gen = gen;
	}

	public boolean isHyperparamsTune() {
		return hyperparamsTune;
	}

	public void setHyperparamsTune(boolean hyperparamsTune) {
		this.hyperparamsTune = hyperparamsTune;
	}

	public String getHyperparamsTuneFile() {
		return hyperparamsTuneFile;
	}

	public void setHyperparamsTuneFile(String hyperparamsTuneFile) {
		this.hyperparamsTuneFile = hyperparamsTuneFile;
	}

	public int getHyperparamsTuneAttempts() {
		return hyperparamsTuneAttempts;
	}

	public void setHyperparamsTuneAttempts(int hyperparamsTuneAttempts) {
		this.hyperparamsTuneAttempts = hyperparamsTuneAttempts;
	}

	public QSRRModelRI(FeaturesGenerator gen) {
		this.gen = gen;
	}

	public QSRRModelRI(FeaturesGenerator gen, boolean hyperparamsTune, String hyperparamsTuneFile,
			int hyperparamsTuneAttempts) {
		this.gen = gen;
		this.hyperparamsTune = hyperparamsTune;
		this.hyperparamsTuneFile = hyperparamsTuneFile;
		this.hyperparamsTuneAttempts = hyperparamsTuneAttempts;
	}

	private static QSRRModelRI getModel(String modelType) {
		if (modelType.equals((new LibLinearRI(null)).modelType())) {
			return new LibLinearRI(null);
		}
		if (modelType.equals((new LibSVMRI(null)).modelType())) {
			return new LibSVMRI(null);
		}
		if (modelType.equals((new RidgeRI(null)).modelType())) {
			return new RidgeRI(null);
		}
		if (modelType.equals((new LASSORI(null)).modelType())) {
			return new LASSORI(null);
		}
		if (modelType.equals((new OLSRI(null)).modelType())) {
			return new OLSRI(null);
		}
		if (modelType.equals((new XgbRI(null)).modelType())) {
			return new XgbRI(null);
		}
		throw new RuntimeException("Unknown model type " + modelType);
	}

	public static QSRRModelRI getModel(String modelType, FeaturesGenerator gen) {
		QSRRModelRI result = getModel(modelType);
		result.gen = gen;
		return result;
	}

	public static QSRRModelRI getModel(String modelType, FeaturesGenerator gen, boolean hyperparamsTune,
			String hyperparamsTuneFile, int hyperparamsTuneAttempts) {
		QSRRModelRI result = getModel(modelType);
		result.gen = gen;
		result.hyperparamsTune = hyperparamsTune;
		result.hyperparamsTuneFile = hyperparamsTuneFile;
		result.hyperparamsTuneAttempts = hyperparamsTuneAttempts;
		return result;
	}

	public static QSRRModelRI loadModel(String directoryName, FeaturesGenerator gen) throws IOException {
		BufferedReader br = new BufferedReader(new FileReader(new File(directoryName, "ModelType.txt")));
		String s = br.readLine();
		while (s.trim().equals("")) {
			s = br.readLine();
		}
		br.close();
		QSRRModelRI result = getModel(s.trim(), gen);
		result.load(directoryName);
		return result;
	}
}
