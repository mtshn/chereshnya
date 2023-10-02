package ru.ac.phyche.chereshnya.featureselectors;

import ru.ac.phyche.chereshnya.ArUtls;
import ru.ac.phyche.chereshnya.ChemDataset;
import ru.ac.phyche.chereshnya.featuregenerators.FeaturesGenerator;
import ru.ac.phyche.chereshnya.featuregenerators.PreprocessedFeaturesGenerator;
import ru.ac.phyche.chereshnya.featurepreprocessors.FeaturesPreprocessor;
import ru.ac.phyche.chereshnya.featurepreprocessors.SelectedFeaturesPreprocessor;

public abstract class FeatureSelector {
	public abstract FeaturesGenerator select(ChemDataset train, FeaturesGenerator gen, int n);

	public static abstract class SeqAddition extends FeatureSelector {
		private int[] selectedDescriptors = new int[] {};

		public int[] getSelectedDescritptors() {
			return selectedDescriptors;
		}

		public void setSelectedDescritptors(int[] selectedDescriptors) {
			this.selectedDescriptors = selectedDescriptors;
		}

		public abstract void addNext(ChemDataset train, FeaturesGenerator gen, int n);

		public FeaturesGenerator select(ChemDataset train, FeaturesGenerator gen, int n) {
			while (selectedDescriptors.length < n) {
				addNext(train, gen, n);
			}
			FeaturesPreprocessor select = new SelectedFeaturesPreprocessor(selectedDescriptors);
			select.train(gen, train);
			return new PreprocessedFeaturesGenerator(gen, select);
		}
	}

	public static abstract class SeqRemoving extends FeatureSelector {
		private int[] selectedDescriptors = null;

		public int[] getSelectedDescritptors() {
			return selectedDescriptors;
		}

		public void setSelectedDescritptors(int[] selectedDescriptors) {
			this.selectedDescriptors = selectedDescriptors;
		}

		public abstract void removeNext(ChemDataset train, FeaturesGenerator gen, int n);

		public FeaturesGenerator select(ChemDataset train, FeaturesGenerator gen, int n) {
			selectedDescriptors = ArUtls.ints(gen.getNumFeatures());

			while (selectedDescriptors.length > n) {
				removeNext(train, gen, n);
			}
			FeaturesPreprocessor select = new SelectedFeaturesPreprocessor(selectedDescriptors);
			select.train(gen, train);
			return new PreprocessedFeaturesGenerator(gen, select);
		}
	}

}
