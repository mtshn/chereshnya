package ru.ac.phyche.chereshnya;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;

import org.openscience.cdk.exception.CDKException;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.StaxDriver;

import ru.ac.phyche.chereshnya.ArUtls;
import ru.ac.phyche.chereshnya.ChemDataset;
import ru.ac.phyche.chereshnya.ChemUtils;
import ru.ac.phyche.chereshnya.DatasetEntry;
import ru.ac.phyche.chereshnya.featuregenerators.CDKDescriptorsGenerator;
import ru.ac.phyche.chereshnya.featuregenerators.CombinedFeaturesGenerator;
import ru.ac.phyche.chereshnya.featuregenerators.FeaturesGenerator;
import ru.ac.phyche.chereshnya.featuregenerators.FuncGroupsCDKGenerator;
import ru.ac.phyche.chereshnya.featuregenerators.MQNDescriptorsGenerator;
import ru.ac.phyche.chereshnya.featuregenerators.PreprocessedFeaturesGenerator;
import ru.ac.phyche.chereshnya.featuregenerators.RDKitDescriptorsGenerator;
import ru.ac.phyche.chereshnya.featurepreprocessors.CombinedFeaturesPreprocessor;
import ru.ac.phyche.chereshnya.featurepreprocessors.DropConstantFeaturesPreprocessor;
import ru.ac.phyche.chereshnya.featurepreprocessors.DropFeaturesPreprocessor;
import ru.ac.phyche.chereshnya.featurepreprocessors.DropFeaturesWithNaNsPreprocessor;
import ru.ac.phyche.chereshnya.featurepreprocessors.DropHighCorrPreprocessor;
import ru.ac.phyche.chereshnya.featurepreprocessors.ReplaceNaNsPreprocessor;
import ru.ac.phyche.chereshnya.featurepreprocessors.Scale01FeaturesPreprocessor;
import ru.ac.phyche.chereshnya.featurepreprocessors.SelectedFeaturesPreprocessor;
import ru.ac.phyche.chereshnya.models.ModelRI;
import ru.ac.phyche.chereshnya.models.OLSRI;
import ru.ac.phyche.chereshnya.models.QSRRModelRI;
import smile.data.DataFrame;
import smile.data.formula.Formula;
import smile.regression.LinearModel;
import smile.regression.OLS;

public class App {
}