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

public class EnsembleRI extends ModelRI {

	QSRRModelRI models[];
	LinearModel ensemble;
	FeaturesGenerator[] gen;
	boolean trainOnlySecondLevel = false;

	public EnsembleRI(FeaturesGenerator[] gen, QSRRModelRI models[]) throws IOException {
		this.gen = gen;
		this.models = models;
	}

	public EnsembleRI(FeaturesGenerator[] gen, QSRRModelRI models[], boolean trainOnlySecondLevel) throws IOException {
		this.gen = gen;
		this.models = models;
		this.trainOnlySecondLevel = trainOnlySecondLevel;
	}

	public EnsembleRI(FeaturesGenerator[] gen) throws IOException {
		this.gen = gen;
		this.models = new QSRRModelRI[gen.length];
		for (int i = 0; i < gen.length; i++) {
			models[i] = new RidgeRI(gen[i]);
		}
	}

	public EnsembleRI(String[] modelsTypes, FeaturesGenerator[] gen, int[] hyperparamsTuneAttempts,
			String hyperparamsTuneDir) throws IOException {
		this.gen = gen;
		if (hyperparamsTuneDir != null) {
			Files.createDirectories(Paths.get(hyperparamsTuneDir));
		}
		int n = modelsTypes.length;
		if ((gen.length != n) || (hyperparamsTuneAttempts.length != n)) {
			throw new RuntimeException("Wrong array length");
		}
		models = new QSRRModelRI[n];
		for (int i = 0; i < n; i++) {
			File f = null;
			if (hyperparamsTuneDir != null) {
				f = new File(hyperparamsTuneDir, "model" + i + ".txt");
			}
			models[i] = QSRRModelRI.getModel(modelsTypes[i], gen[i], hyperparamsTuneAttempts[i] > 0,
					f == null ? null : f.getAbsolutePath(), hyperparamsTuneAttempts[i]);
		}
	}

	@Override
	public float[] predict(String[] smiles) {
		float[][] predictionsT = new float[models.length][smiles.length];
		for (int i = 0; i < models.length; i++) {
			predictionsT[i] = ArUtls.mult(0.001f, models[i].predict(smiles));
		}
		float[][] predictions = ArUtls.transpose(predictionsT);
		float[] result = ArUtls.mult(1000, ArUtls.toFloatArray(ensemble.predict(ArUtls.toDataFrame(predictions))));
		return result;
	}

	@Override
	public void train(ChemDataset trainSet, ChemDataset validationSet) {
		if (!trainOnlySecondLevel) {
			for (int i = 0; i < models.length; i++) {
				models[i].train(trainSet, validationSet);
			}
		}
		float[][] predictionsT = new float[models.length][validationSet.size()];
		for (int i = 0; i < models.length; i++) {
			predictionsT[i] = ArUtls.mult(0.001f, models[i].predict(validationSet));
		}
		float[][] predictions = ArUtls.transpose(predictionsT);
		DataFrame dataFrame = ArUtls.toDataFrame(predictions, ArUtls.mult(0.001F, validationSet.allRetentions()));
		ensemble = OLS.fit(Formula.lhs("label"), dataFrame);
	}

	@Override
	public void save(String directory) throws IOException {
		for (int i = 0; i < models.length; i++) {
			File f = null;
			f = new File(directory, "model" + i);
			models[i].save(f.getAbsolutePath());
		}
		FileWriter fw = new FileWriter(new File(directory, "ModelType.txt"));
		fw.write(this.modelType());
		fw.close();
		fw = new FileWriter(new File(directory, "info.txt"));
		fw.write(models.length + "");
		fw.close();
		fw = new FileWriter(new File(directory, "info1.txt"));
		fw.write(this.fullModelInfo());
		fw.close();
		File f = new File(directory, "model.xml");
		fw = new FileWriter(f);
		XStream xstream = new XStream(new StaxDriver());
		xstream.toXML(ensemble, fw);
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
		int n = Integer.parseInt(s);
		models = new QSRRModelRI[n];
		File f = new File(directory, "model.xml");
		XStream xstream = new XStream();
		xstream.allowTypes(new String[] { "smile.regression.LinearModel", "smile.data.formula.Variable",
				"smile.data.type.StructField", "smile.data.type.DoubleType", "smile.math.matrix.Matrix" });
		ensemble = (LinearModel) xstream.fromXML(f);

		for (int i = 0; i < models.length; i++) {
			f = null;
			f = new File(directory, "model" + i);
			models[i] = QSRRModelRI.loadModel(f.getAbsolutePath(), this.gen[i]);
		}
	}

	@Override
	public String modelType() {
		return "Ensemble";
	}

	@Override
	public String fullModelInfo() {
		String s = "EnsembleRI " + models.length + " ";
		for (int i = 0; i < models.length; i++) {
			s = s + models[i].fullModelInfo() + " ";
		}
		return s;
	}

	@Override
	public ModelRI createSimilar() {
		String[] modelsTypes = new String[this.models.length];
		int[] hyperparamsTuneAttempts = new int[this.models.length];
		FeaturesGenerator[] gen = new FeaturesGenerator[this.models.length];
		for (int i = 0; i < models.length; i++) {
			modelsTypes[i] = this.models[i].modelType();
			hyperparamsTuneAttempts[i] = this.models[i].getHyperparamsTuneAttempts();
			gen[i] = this.models[i].getGen();
		}

		EnsembleRI result;
		try {
			File f = new File(this.models[0].getHyperparamsTuneFile());
			result = new EnsembleRI(modelsTypes, gen, hyperparamsTuneAttempts, f.getParent());
		} catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException(e.getMessage());
		}
		return result;
	}

	@Override
	public void setTuningOutFileOrDir(String filename) {
		if (filename != null) {
			for (int i = 0; i < models.length; i++) {
				File f = null;
				if (filename != null) {
					f = new File(filename, "model" + i + ".txt");
				}
				models[i].setHyperparamsTuneFile(f.getAbsolutePath());
			}
		} else {
			for (int i = 0; i < models.length; i++) {
				models[i].setHyperparamsTuneFile(null);
			}
		}
	}

}
