package ru.ac.phyche.chereshnya.featurepreprocessors;

import java.io.BufferedReader;
import java.io.FileWriter;
import java.io.IOException;

import ru.ac.phyche.chereshnya.ArUtls;
import ru.ac.phyche.chereshnya.ChemDataset;
import ru.ac.phyche.chereshnya.featuregenerators.FeaturesGenerator;

public class ColumnShuffle extends FeaturesPreprocessor {

	private int[] order;
	private String[] names = new String[] {};

	@Override
	public void train(FeaturesGenerator features, ChemDataset data) {
		this.order = ArUtls.intsrnd(features.getNumFeatures());
		names = new String[order.length];
		for (int i = 0; i < order.length; i++) {
			names[i] = features.getName(order[i]);
		}
	}

	@Override
	public void save(FileWriter filewriter) throws IOException {
		throw new RuntimeException("Not implemented yet");
	}

	@Override
	public void load(BufferedReader filereader) throws IOException {
		throw new RuntimeException("Not implemented yet");
	}

	@Override
	public float[] preprocess(float[] input) {
		if (input.length != order.length) {
			throw new RuntimeException("Wrong array length! Error!");
		}
		float[] result = new float[input.length];
		for (int i = 0; i < input.length; i++) {
			result[i] = input[order[i]];
		}
		return result;
	}

	@Override
	public String[] featureNames() {
		return names;
	}

}
