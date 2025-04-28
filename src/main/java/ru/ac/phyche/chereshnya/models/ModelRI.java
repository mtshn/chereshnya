package ru.ac.phyche.chereshnya.models;

import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.apache.commons.lang3.tuple.Pair;
import org.openscience.cdk.exception.CDKException;

import ru.ac.phyche.chereshnya.ArUtls;
import ru.ac.phyche.chereshnya.ChemDataset;
import ru.ac.phyche.chereshnya.models.QSRRModelRI.AccuracyMeasure;

public abstract class ModelRI {
	public abstract float[] predict(String[] smiles);

	public abstract void train(ChemDataset trainSet, ChemDataset validationSet);

	public abstract void save(String directory) throws IOException;

	public abstract void load(String directory) throws IOException;

	public abstract String modelType();

	public abstract String fullModelInfo();

	public abstract ModelRI createSimilar();

	public abstract void setTuningOutFileOrDir(String filename);

	public float[] predict(ChemDataset data) {
		return predict(ChemDataset.smilesFromChemDataset(data));
	}

	public float predict(String smiles) {
		return predict(new String[] { smiles })[0];
	}

	public static String accuracyMeasuresValidation(float[] predictions, float[] labels) {
		try {
			return accuracyMeasuresValidation(new String[predictions.length], predictions, labels, false, null);
		} catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException(e.getLocalizedMessage());
		}
	}

	public static String accuracyMeasuresValidation(String[] smiles, float[] predictions, float[] labels,
			boolean moreMeasures, FileWriter writePredictions) throws IOException {
		if (labels.length != predictions.length) {
			throw (new RuntimeException("Wrong length of arrays"));
		}
		float[] deltas = ArUtls.absDelta(labels, predictions);
		float[] sqDeltas = ArUtls.ewMult(deltas, deltas);
		float[] relDeltas = ArUtls.ewDiv(deltas, labels);
		float mdae = ArUtls.median(deltas);
		float mae = ArUtls.mean(deltas);
		float rmse = ((float) Math.sqrt(ArUtls.mean(sqDeltas)));
		float mdpe = ArUtls.median(relDeltas);
		float mpe = ArUtls.mean(relDeltas);
		String result = "RMSE: " + rmse + " MAE: " + mae + " MdAE: " + mdae + " MPE: " + 100f * mpe + " MdPE: "
				+ 100f * mdpe;
		if (moreMeasures) {
			float r2 = ArUtls.r2(labels, predictions);
			float r = ArUtls.corr(labels, predictions);
			float d80abs = ArUtls.delta80(deltas);
			float d90abs = ArUtls.delta90(deltas);
			float d95abs = ArUtls.delta95(deltas);
			float d80rel = ArUtls.delta80(relDeltas);
			float d90rel = ArUtls.delta90(relDeltas);
			float d95rel = ArUtls.delta95(relDeltas);
			float x = ArUtls.percentMoreThan70(deltas);

			result += "\nR2: " + r2 + " percentMoreThan70 " + x + " r: " + r + "\nd80abs " + d80abs + " d90abs "
					+ d90abs + " d95abs " + d95abs + "\nd80rel " + 100f * d80rel + " d90rel " + 100f * d90rel
					+ " d95rel " + 100f * d95rel;
		}
		if (writePredictions != null) {
			ArUtls.savePredResults(writePredictions, smiles, labels, predictions);
		}
		return result;
	}

	public String validate(String[] smiles, float[] labels, boolean moreMeasures, FileWriter writePredictions)
			throws IOException {
		float[] predictions = predict(smiles);
		return accuracyMeasuresValidation(smiles, predictions, labels, moreMeasures, writePredictions);
	}

	public String validate(String[] smiles, float[] labels, boolean moreMeasures) throws IOException {
		FileWriter fw = null;
		String s = validate(smiles, labels, moreMeasures, fw);
		return s;
	}

	public String validate(String[] smiles, float[] labels) throws IOException {
		String s = validate(smiles, labels, true);
		return s;
	}

	public String validate(ChemDataset data, boolean moreMeasures, FileWriter writePredictions) throws IOException {
		return validate(ChemDataset.smilesFromChemDataset(data), ChemDataset.retentionsFromChemDataset(data),
				moreMeasures, writePredictions);
	}

	public String validate(ChemDataset data, boolean moreMeasures) throws IOException {
		return validate(ChemDataset.smilesFromChemDataset(data), ChemDataset.retentionsFromChemDataset(data),
				moreMeasures);
	}

	public String validate(ChemDataset data) throws IOException {
		return validate(ChemDataset.smilesFromChemDataset(data), ChemDataset.retentionsFromChemDataset(data));
	}

	public String crossValidation(ChemDataset[] trainArray, ChemDataset[] testArray, int toValidationSet,
			FileWriter log, boolean allMeasures) throws IOException, CDKException {
		if (trainArray.length != testArray.length) {
			throw (new RuntimeException("Wrong length of arrays"));
		}
		int nFold = trainArray.length;
		String[] results = new String[nFold];
		float[] predictionsAll = new float[] {};
		float[] labelsAll = new float[] {};
		String[] smilesAll = new String[] {};
		if (log != null) {
			DateTimeFormatter t = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");
			LocalDateTime ti = LocalDateTime.now();
			log.write("Cross-validation started. " + nFold + " -fold " + t.format(ti));
			log.write(fullModelInfo() + "\n");
		}
		for (int i = 0; i < nFold; i++) {
			ChemDataset train = trainArray[i];
			ChemDataset test = testArray[i];

			if (log != null) {
				log.write("Split_" + i);
				log.write("_ _ _ Train-validation_set: " + train.size() + " ( " + train.compounds().size()
						+ " compounds)\n");
				log.write(train.compounds() + "\n");
				log.flush();
			}
			ChemDataset validation = train.compoundsBasedSplitAndShuffle(toValidationSet);
			this.train(train, validation);
			float[] predictions = predict(test);
			float[] labels = ChemDataset.retentionsFromChemDataset(test);
			String[] smiles = ChemDataset.smilesFromChemDataset(test);
			results[i] = accuracyMeasuresValidation(smiles, predictions, labels, allMeasures, log);
			predictionsAll = ArUtls.mergeArrays(predictionsAll, predictions);
			labelsAll = ArUtls.mergeArrays(labelsAll, labels);
			smilesAll = ArUtls.mergeArrays(smilesAll, smiles);
		}
		String result = accuracyMeasuresValidation(smilesAll, predictionsAll, labelsAll, allMeasures, null);
		if (log != null) {
			for (int i = 0; i < nFold; i++) {
				log.write("Subset " + i + " " + results[i] + "\n");
			}
			log.write("\n\n\n");
			log.write("CV results\n");
			log.write(result);
			DateTimeFormatter t = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");
			LocalDateTime ti = LocalDateTime.now();
			log.write("\nCross-validation finished. " + nFold + " -fold " + t.format(ti) + "\n");
			log.flush();
		}
		return accuracyMeasuresValidation(smilesAll, predictionsAll, labelsAll, allMeasures, log);
	}

	public static Pair<ChemDataset[], ChemDataset[]> trainSetssAndSetsArraysForCV(ChemDataset data, int nFold,
			FileWriter log, boolean stereoisomers, boolean makeCanonicalAll) throws CDKException, IOException {
		if (log != null) {
			DateTimeFormatter t = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");
			LocalDateTime ti = LocalDateTime.now();
			log.write("Preparing for cross-validation. " + t.format(ti) + "\n");
			log.flush();
		}
		ChemDataset[] trainArray = new ChemDataset[nFold];
		ChemDataset[] testArray = new ChemDataset[nFold];
		ChemDataset[] split = new ChemDataset[nFold];
		ChemDataset clone = data.copy();
		if (makeCanonicalAll) {
			clone.makeCanonicalAll(stereoisomers);
		}
		ChemDataset clone1 = clone.copy();
		int sizeI = data.compounds().size() / nFold;
		clone = clone.shuffle();
		for (int i = 0; i < nFold - 1; i++) {
			split[i] = clone.compoundsBasedSplitAndShuffle(sizeI);
		}
		split[nFold - 1] = clone;
		for (int i = 0; i < nFold; i++) {
			ChemDataset train = clone1.copy();
			ChemDataset test = split[i];
			if (makeCanonicalAll) {
				train.filterIdentical(test);
				train.filterIdenticalByInchi(test);
				if ((train.countIdenticalByInchi(test) != 0) || (test.countIdenticalByInchi(train) != 0)) {
					throw (new RuntimeException("Overlap between train and test is non-zero"));
				}
			} else {
				train.filterIdenticalFastNoCanonical(test);
			}
			train = train.shuffle();
			trainArray[i] = train;
			testArray[i] = test;
		}
		return Pair.of(trainArray, testArray);
	}

	public String crossValidation(ChemDataset data, int toValidationSet, int nFold, FileWriter log,
			boolean stereoisomers, boolean allMeasures, boolean makeCanonicalAll) throws IOException, CDKException {
		Pair<ChemDataset[], ChemDataset[]> pr = trainSetssAndSetsArraysForCV(data, nFold, log, stereoisomers,
				makeCanonicalAll);
		return crossValidation(pr.getLeft(), pr.getRight(), toValidationSet, log, allMeasures);
	}

	public String crossValidation(ChemDataset data, float fractionValidationSet, int nFold, FileWriter log,
			boolean stereoisomers, boolean allMeasures, boolean makeCanonicalAll) throws IOException, CDKException {
		float n = ((float) data.compounds().size()) * ((float) nFold - 1 / (float) nFold);
		n = n * fractionValidationSet;
		return crossValidation(data, Math.round(n), nFold, log, stereoisomers, allMeasures, makeCanonicalAll);
	}

	public String crossValidation(ChemDataset data, float fractionValidationSet, int nFold, FileWriter log)
			throws IOException, CDKException {
		return crossValidation(data, fractionValidationSet, nFold, log, true, true, false);
	}

	/**
	 * 
	 * @param validationString output string of validation(...) method
	 * @return MAE value
	 * @throws CDKException CDK
	 * @throws IOException  IO
	 */
	public static float mae(String validationString) {
		return Float.parseFloat(validationString.split("\\s+")[3]);
	}

	/**
	 * 
	 * @param validationString output string of validation(...) method
	 * @return MdAE value
	 * @throws CDKException CDK
	 * @throws IOException  IO
	 */
	public static float mdae(String validationString) {
		return Float.parseFloat(validationString.split("\\s+")[5]);
	}

	/**
	 * 
	 * @param validationString output string of validation(...) method
	 * @return RMSE value
	 * @throws CDKException CDK
	 * @throws IOException  IO
	 */
	public static float rmse(String validationString) {
		return Float.parseFloat(validationString.split("\\s+")[1]);
	}

	/**
	 * 
	 * @param validationString output string of validation(...) method
	 * @return MPE value
	 * @throws CDKException CDK
	 * @throws IOException  IO
	 */
	public static float mpe(String validationString) {
		return Float.parseFloat(validationString.split("\\s+")[7]);
	}

	/**
	 * 
	 * @param validationString output string of validation(...) method
	 * @return MdPE value
	 * @throws CDKException CDK
	 * @throws IOException  IO
	 */
	public static float mdpe(String validationString) {
		return Float.parseFloat(validationString.split("\\s+")[9]);
	}

	public static float accuracy(AccuracyMeasure measure, String validationString) {
		float f = Float.NaN;
		switch (measure) {
		case RMSE:
			f = rmse(validationString);
			break;
		case MAE:
			f = mae(validationString);
			break;
		case MPE:
			f = mpe(validationString);
			break;
		case MDPE:
			f = mdpe(validationString);
			break;
		case MDAE:
			f = mdae(validationString);
			break;
		}
		return f;
	}
}
