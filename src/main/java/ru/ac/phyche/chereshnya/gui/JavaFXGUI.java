package ru.ac.phyche.chereshnya.gui;

import java.awt.Font;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.MalformedURLException;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;

import javax.imageio.ImageIO;
import javax.swing.JPanel;

import org.apache.commons.lang3.SystemUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.knowm.xchart.CategoryChart;
import org.knowm.xchart.CategoryChartBuilder;
import org.knowm.xchart.VectorGraphicsEncoder;
import org.knowm.xchart.XChartPanel;
import org.knowm.xchart.VectorGraphicsEncoder.VectorGraphicsFormat;
import org.openscience.cdk.exception.CDKException;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.concurrent.Worker;
import javafx.embed.swing.SwingFXUtils;
import javafx.embed.swing.SwingNode;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.SnapshotParameters;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.ScatterChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Border;
import javafx.scene.layout.Pane;
import javafx.scene.layout.TilePane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.transform.Transform;
import javafx.scene.web.WebView;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Duration;
import javafx.util.converter.FloatStringConverter;
import javafx.util.converter.NumberStringConverter;
import ru.ac.phyche.chereshnya.ChemDataset;
import ru.ac.phyche.chereshnya.ChemUtils;
import ru.ac.phyche.chereshnya.PythonRunner;
import ru.ac.phyche.chereshnya.featuregenerators.CDKDescriptorsGenerator;
import ru.ac.phyche.chereshnya.featuregenerators.CDKFingerprintsGenerator;
import ru.ac.phyche.chereshnya.featuregenerators.CombinedFeaturesGenerator;
import ru.ac.phyche.chereshnya.featuregenerators.FeaturesGenerator;
import ru.ac.phyche.chereshnya.featuregenerators.FuncGroupsCDKGenerator;
import ru.ac.phyche.chereshnya.featuregenerators.LinearModelRIGenerator;
import ru.ac.phyche.chereshnya.featuregenerators.MQNDescriptorsGenerator;
import ru.ac.phyche.chereshnya.featuregenerators.PreprocessedFeaturesGenerator;
import ru.ac.phyche.chereshnya.featuregenerators.RDKitDescriptorsGenerator;
import ru.ac.phyche.chereshnya.featuregenerators.SVEKLAGeneratorRI;
import ru.ac.phyche.chereshnya.featurepreprocessors.ColumnShuffle;
import ru.ac.phyche.chereshnya.featurepreprocessors.CombinedFeaturesPreprocessor;
import ru.ac.phyche.chereshnya.featurepreprocessors.DropConstantFeaturesPreprocessor;
import ru.ac.phyche.chereshnya.featurepreprocessors.DropFeaturesWithNaNsPreprocessor;
import ru.ac.phyche.chereshnya.featurepreprocessors.DropHighCorrPreprocessor;
import ru.ac.phyche.chereshnya.featurepreprocessors.DropLowVarPreprocessor;
import ru.ac.phyche.chereshnya.featurepreprocessors.FeaturesPreprocessor;
import ru.ac.phyche.chereshnya.featurepreprocessors.ReplaceNaNsPreprocessor;
import ru.ac.phyche.chereshnya.featurepreprocessors.Scale01FeaturesPreprocessor;
import ru.ac.phyche.chereshnya.featurepreprocessors.SelectedFeaturesPreprocessor;
import ru.ac.phyche.chereshnya.featureselectors.BorutaBoosting;
import ru.ac.phyche.chereshnya.featureselectors.BorutaForest;
import ru.ac.phyche.chereshnya.featureselectors.FeatureImportances;
import ru.ac.phyche.chereshnya.featureselectors.FeatureSelector;
import ru.ac.phyche.chereshnya.featureselectors.GA;
import ru.ac.phyche.chereshnya.featureselectors.LASSONonSequential;
import ru.ac.phyche.chereshnya.featureselectors.NothingDoSelector;
import ru.ac.phyche.chereshnya.featureselectors.PLSVIP;
import ru.ac.phyche.chereshnya.featureselectors.SeqAdditionBestF;
import ru.ac.phyche.chereshnya.featureselectors.SeqAdditionBestLoss;
import ru.ac.phyche.chereshnya.featureselectors.SeqAdditionOLSMDAE;
import ru.ac.phyche.chereshnya.featureselectors.SeqAdditionOLSRMSE;
import ru.ac.phyche.chereshnya.featureselectors.SeqRemovingForest;
import ru.ac.phyche.chereshnya.featureselectors.FeatureImportances.FeatureImportance;
import ru.ac.phyche.chereshnya.models.LASSORI;
import ru.ac.phyche.chereshnya.models.LinearModelRI;
import ru.ac.phyche.chereshnya.models.ModelRI;
import ru.ac.phyche.chereshnya.models.OLSRI;
import ru.ac.phyche.chereshnya.models.QSRRModelRI;
import ru.ac.phyche.chereshnya.models.RidgeRI;
import netscape.javascript.JSObject;

public class JavaFXGUI extends Application {

	private static class Mdl {
		public ModelRI model = null;
	}

	private static class Gen {
		public CombinedFeaturesGenerator g = null;
		ArrayList<Integer> selectedDescriptors = new ArrayList<Integer>();

		public Gen() {
			ArrayList<FeaturesGenerator> lst = new ArrayList<FeaturesGenerator>();
			lst.add(new RDKitDescriptorsGenerator());
			g = new CombinedFeaturesGenerator(lst.toArray(new FeaturesGenerator[lst.size()]));
		}
	}

	private static String predictedVsObservedAccuracy(ArrayList<Pair<Float, Float>> predictedVsObserved) {
		int i = 0;
		float[] predicted = new float[predictedVsObserved.size()];
		float[] observed = new float[predictedVsObserved.size()];
		for (Pair<Float, Float> p : predictedVsObserved) {
			predicted[i] = p.getLeft();
			observed[i] = p.getRight();
			i++;
		}
		return ModelRI.accuracyMeasuresValidation(predicted, observed);
	}

	private static class BarchatStyleSettings {
		boolean barChatHideCaptions;
		boolean barChatHideRdkit;
		boolean barChatHideY;
		int axisFontBarchat;
		int yTickBarchat;
		String colorBarchat;
		int xLabeslAngleBarchat;

		BarchatStyleSettings(CheckBox barChatHideCaptions, CheckBox barChatHideRdkit, CheckBox barChatHideY,
				TextField axisFontBarchat, TextField yTickBarchat, TextField colorBarchat,
				TextField xLabeslAngleBarchat) {
			this.barChatHideCaptions = barChatHideCaptions.selectedProperty().get();
			this.barChatHideRdkit = barChatHideRdkit.selectedProperty().get();
			this.barChatHideY = barChatHideY.selectedProperty().get();
			this.axisFontBarchat = Integer.parseInt(axisFontBarchat.getText());
			this.yTickBarchat = Integer.parseInt(yTickBarchat.getText());
			this.colorBarchat = colorBarchat.getText();
			this.xLabeslAngleBarchat = Integer.parseInt(xLabeslAngleBarchat.getText());
		}
	}

	private static XChartPanel importancesChart(FeatureImportances.FeatureImportance[] importances, int nTry,
			boolean showConfidenceIntervalInsteadStdev, BarchatStyleSettings style) throws IOException {
		CategoryChart chart = (new CategoryChartBuilder()).xAxisTitle("Descriptors").yAxisTitle("Importances")
				.width(800).height(600).title("Importances of descriptors").build();
		ArrayList<String> labels = new ArrayList<String>();
		ArrayList<Double> values = new ArrayList<Double>();
		ArrayList<Double> errors = new ArrayList<Double>();

		int i = 0;
		while ((i < 20) && (i < importances.length)) {
			String s = importances[importances.length - 1 - i].name;
			if (style.barChatHideRdkit) {
				s = s.replace("RDKIT_", " ").trim();
			}
			labels.add(s);
			values.add((double) importances[importances.length - 1 - i].value);
			double stdev = (double) importances[importances.length - 1 - i].stdev;
			if (!showConfidenceIntervalInsteadStdev) {
				errors.add(stdev);
			} else {
				errors.add(1.96 * stdev / (Math.sqrt(nTry)));
			}
			i++;
		}

		labels.add(" ");
		values.add(0.0);
		errors.add(0.0);

		if (nTry > 1) {
			chart.addSeries("Importances of descriptors", labels, values, errors);
		} else {
			chart.addSeries("Importances of descriptors", labels, values);
		}
		chart.getStyler().setChartBackgroundColor(java.awt.Color.WHITE);
		chart.getStyler().setPlotGridLinesVisible(false);
		chart.getStyler().setLegendVisible(false);
		chart.getStyler().setYAxisTickMarkSpacingHint(style.yTickBarchat);
		chart.getStyler().setXAxisLabelRotation(style.xLabeslAngleBarchat);
		chart.getStyler().setSeriesColors(new java.awt.Color[] { java.awt.Color.decode(style.colorBarchat) });
		if (style.barChatHideY) {
			chart.getStyler().setYAxisTicksVisible(false);
			chart.getStyler().setYAxisTitleVisible(false);
		}
		if (style.barChatHideCaptions) {
			chart.getStyler().setYAxisTitleVisible(false);
			chart.getStyler().setXAxisTitleVisible(false);
			chart.getStyler().setChartTitleVisible(false);
		}
		chart.getStyler().setAxisTickLabelsFont(new Font(null, Font.BOLD, style.axisFontBarchat));
		VectorGraphicsEncoder.saveVectorGraphic(chart, "barchart.pdf", VectorGraphicsFormat.PDF);
		return new XChartPanel(chart);
	}

	private static ScatterChart predictedVsObservedChart(ArrayList<Pair<Float, Float>> predictedVsObserved)
			throws IOException {
		NumberAxis x = new NumberAxis();
		x.setLabel("Observed");
		NumberAxis y = new NumberAxis();
		y.setLabel("Predicted");

		ScatterChart chart = new ScatterChart(x, y);
		chart.setTitle("Predicted vs Observed");

		XYChart.Series series = new XYChart.Series();
		series.setName("Predicted vs Observed");
		for (Pair<Float, Float> p : predictedVsObserved) {
			series.getData().add(new XYChart.Data(p.getRight(), p.getLeft()));
		}
		chart.setLegendVisible(false);
		chart.getData().add(series);
		chart.setHorizontalGridLinesVisible(false);
		chart.setVerticalGridLinesVisible(false);
		x.setMinorTickVisible(false);
		y.setMinorTickVisible(false);
		NumberFormat nf = NumberFormat.getNumberInstance(Locale.ENGLISH);
		nf.setGroupingUsed(false);
		x.setTickLabelFormatter(new NumberStringConverter(nf));
		y.setTickLabelFormatter(new NumberStringConverter(nf));

		Scene sc = new Scene(chart, 400, 400);
		sc.setFill(Color.WHITE);
		chart.lookup(".chart-plot-background").setStyle("-fx-background-color: white;-fx-font-size: 16px; ");
		chart.lookup(".chart-content").setStyle("-fx-background-color: white;-fx-font-size: 16px;");
		chart.lookup(".chart").setStyle("-fx-background-color: white;-fx-font-size: 16px;");
		chart.applyCss();
		x.setStyle("-fx-border-color: #000000 transparent transparent; -fx-border-width:2");
		y.setStyle("-fx-border-color: transparent #000000 transparent transparent; -fx-border-width:2");
		x.applyCss();
		y.applyCss();

		SnapshotParameters sp1 = new SnapshotParameters();
		Transform transform = Transform.scale(12, 12);
		sp1.setFill(Color.WHITE);
		sp1.setTransform(transform);
		WritableImage img = chart.snapshot(sp1, null);
		BufferedImage bi = SwingFXUtils.fromFXImage(img, null);
		ImageIO.write(bi, "png", new File("scatterchart.png"));
		return chart;
	}

	private static String[] trainModelGetEquationAndAccuracyLASSO(FeaturesGenerator gen, ChemDataset ds, Mdl model,
			float l1) throws IOException, CDKException {
		gen.precompute(ds);

		Scale01FeaturesPreprocessor scale = new Scale01FeaturesPreprocessor();
		scale.train(gen, ds);
		gen = new PreprocessedFeaturesGenerator(gen, scale);

		LASSORI modelFinal = new LASSORI(gen);
		modelFinal.setl1(l1);
		modelFinal.train(ds, ChemDataset.empty());
		String accuracy = modelFinal.crossValidation(ds, 0, 10, null, false, false, false);
		float[] coef = modelFinal.unscaledModelCoefficientsWithB(scale.getMin(), scale.getMax());

		String eq = coef[0] + " + ";
		for (int i = 1; i < coef.length; i++) {
			eq = eq + gen.getName(i - 1) + " * " + coef[i];
			if (i != coef.length - 1) {
				eq = eq + " + ";
			}
		}
		eq = eq + " (l1 = " + l1 + " )";
		model.model = modelFinal;
		return new String[] { eq, accuracy };
	}

	private static void computeHeatmap() {
		PythonRunner.runPython("./heatmap.py", "");
	}

	private static String minMaxAverageVariationInfo(FeaturesGenerator gen, ChemDataset ds) {
		gen.precompute(ds);
		String s = "";
		Scale01FeaturesPreprocessor scale = new Scale01FeaturesPreprocessor();
		scale.train(gen, ds);
		float[] min = scale.getMin();
		float[] max = scale.getMax();
		for (int i = 0; i < gen.getNumFeatures(); i++) {
			float summ = 0;
			for (int j = 0; j < ds.size(); j++) {
				summ += gen.featuresForMol(ds.getSmiles(j))[i];
			}
			float var = 0;
			float average = summ / ds.size();
			for (int j = 0; j < ds.size(); j++) {
				float d = gen.featuresForMol(ds.getSmiles(j))[i] - average;
				var = var + d * d;
			}
			var = (float) Math.sqrt(var / ds.size());
			float varMinMax = var / (max[i] - min[i]);
			s = s + gen.getName(i) + " MIN: " + min[i] + " MIN: " + max[i] + " AVERAGE: " + average + " STDEV: " + var
					+ "STDEV_SCALED: " + varMinMax + "\n";
		}
		return s;
	}

	private static float computeDescriptor(String smiles, String descriptorName) throws CDKException {
		String smilesCan = ChemUtils.canonical(smiles, false);
		smilesCan = ChemUtils.canonical(smilesCan, false);
		if (descriptorName.contains("CDK_")) {
			float[] d = ChemUtils.descriptors(smilesCan, new String[] { descriptorName.split("CDK\\_")[1] });
			return (d[0]);
		} else {
			ArrayList<FeaturesGenerator> lst = new ArrayList<FeaturesGenerator>();
			lst.add(new CDKDescriptorsGenerator());
			lst.add(new RDKitDescriptorsGenerator());
			lst.add(new FuncGroupsCDKGenerator());
			lst.add(new MQNDescriptorsGenerator());
			lst.add((new CDKFingerprintsGenerator(ChemUtils.FingerprintsType.KLEKOTA_ADDITIVE)));
			lst.add(new LinearModelRIGenerator());

			CombinedFeaturesGenerator gen1 = new CombinedFeaturesGenerator(
					lst.toArray(new FeaturesGenerator[lst.size()]));
			float[] d = gen1.featuresForMolNoPrecompute(smilesCan);
			String[] names = gen1.getNames();
			for (int i = 0; i < names.length; i++) {
				if (names[i].equals(descriptorName)) {
					return (d[i]);
				}
			}
		}
		return 0;
	}

	private static void showMessageWithError(Exception e) {
		StringWriter sw = new StringWriter();
		e.printStackTrace(new PrintWriter(sw));
		showMessageWithError(sw.toString());
	}

	private static void showMessageWithError(String message) {
		try {
			Stage resultsStage = new Stage();
			FXMLLoader loader2 = new FXMLLoader();
			loader2.setLocation(new File("./exception.fxml").toURI().toURL());
			VBox vBox2 = loader2.<VBox>load();
			Scene scene2 = new Scene(vBox2, 590, 500);
			resultsStage.setScene(scene2);
			resultsStage.show();
			resultsStage.getIcons().add(new Image((new File("./chereshnya1.png")).toURI().toURL().toString()));
			TextArea errorArea = (TextArea) vBox2.lookup("#exceptionfield");
			errorArea.setText(message);
		} catch (Exception e) {
			throw new RuntimeException(e.getMessage());
		}
	}

	private static String[] trainModelGetEquationAndAccuracy(FeaturesGenerator gen, ChemDataset ds, float l2, Mdl model)
			throws IOException, CDKException {
		gen.precompute(ds);

		Scale01FeaturesPreprocessor scale = new Scale01FeaturesPreprocessor();
		scale.train(gen, ds);
		gen = new PreprocessedFeaturesGenerator(gen, scale);

		ModelRI modelFinal = new OLSRI(gen);

		if (l2 > 0) {
			modelFinal = new RidgeRI(gen, l2);
		}

		FileWriter cv = new FileWriter("cv_log.tmp");
		System.out.print("n_features " + gen.getNumFeatures());
		String accuracy = modelFinal.crossValidation(ds, 0, 10, cv, false, false, false);
		System.out.println(";");
		cv.close();
		float[] coef = ((LinearModelRI) modelFinal).unscaledModelCoefficientsWithB(scale.getMin(), scale.getMax());

		String eq = coef[0] + " + ";
		for (int i = 1; i < coef.length; i++) {
			eq = eq + gen.getName(i - 1) + " * " + coef[i];
			if (i != coef.length - 1) {
				eq = eq + " + ";
			}
		}

		model.model = modelFinal;
		return new String[] { eq, accuracy };
	}

	private static void computeDescriptorsForDataSet(String datasetfilename, String outputfilename,
			FeaturesGenerator gen) throws IOException {
		BufferedReader br = new BufferedReader(new FileReader(datasetfilename));
		String s = br.readLine();
		ArrayList<String> smiles = new ArrayList<String>();
		while (s != null) {
			if (!s.trim().equals("")) {
				try {
					String smiles1 = s.trim().split("\\s+")[0];
					smiles.add(ChemUtils.canonical(smiles1, false));
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			s = br.readLine();
		}
		br.close();

		float features[][] = gen.featuresNoPrecompute(smiles.toArray(new String[smiles.size()]));

		FileWriter fw1 = new FileWriter(outputfilename);
		FileWriter fw2 = new FileWriter("descriptors.tmp.csv");

		fw1.write("SMILES ");
		for (int i = 0; i < features[0].length; i++) {
			fw1.write(gen.getName(i) + ((i == features[0].length - 1) ? "\n" : " "));
			fw2.write(gen.getName(i) + ((i == features[0].length - 1) ? "\n" : ","));
		}

		for (int i = 0; i < features.length; i++) {
			fw1.write(smiles.get(i) + " ");
			for (int j = 0; j < features[0].length; j++) {
				fw1.write(features[i][j] + ((j == features[0].length - 1) ? "\n" : " "));
				fw2.write(features[i][j] + ((j == features[0].length - 1) ? "\n" : ","));
			}
		}
		fw1.close();
		fw2.close();
	}

	@Override
	public void start(Stage primaryStage) throws Exception {

		Locale.setDefault(Locale.US);

		primaryStage.setTitle("CHERESHNYA: A GUI for quantitative structure-retention relationships");
		FXMLLoader loader = new FXMLLoader();
		loader.setLocation(new File("./chereshnya.fxml").toURI().toURL());
		VBox vBox = loader.<VBox>load();
		Scene scene = new Scene(vBox, 1000, 720);
		primaryStage.setScene(scene);
		primaryStage.getIcons().add(new Image((new File("./chereshnya1.png")).toURI().toURL().toString()));
		primaryStage.show();
		primaryStage.setOnCloseRequest(event -> {
			System.exit(0);
		});

		Stage resultsStage = new Stage();
		FXMLLoader loader2 = new FXMLLoader();
		loader2.setLocation(new File("./results.fxml").toURI().toURL());
		VBox vBox2 = loader2.<VBox>load();
		Scene scene2 = new Scene(vBox2, 1000, 720);
		resultsStage.setScene(scene2);
		resultsStage.show();
		resultsStage.getIcons().add(new Image((new File("./chereshnya1.png")).toURI().toURL().toString()));
		resultsStage.hide();

		TextField excludernd = (TextField) vBox.lookup("#excludernd");
		TextField highcorr = (TextField) vBox.lookup("#highcorr");
		TextField lowvar = (TextField) vBox.lookup("#lowvar");
		TextField datafile = (TextField) vBox.lookup("#datafile");
		TextField datafile2 = (TextField) vBox.lookup("#datafile2");

		TextField descriptorname = (TextField) vBox.lookup("#descriptorname");
		TextField descriptorvalue = (TextField) vBox.lookup("#descriptorvalue");
		TextField rivalue = (TextField) vBox.lookup("#rivalue");
		TextField sveklaPath = (TextField) vBox.lookup("#svekla_path");
		TextField borutoRounds = (TextField) vBox.lookup("#borutorounds");
		TextField outFileDescriptors = (TextField) vBox.lookup("#outfiledescriptors");
		TextField nRepeats = (TextField) vBox.lookup("#nrepeats");
		TextField nCompPLS = (TextField) vBox.lookup("#n_comp_pls");
		TextField nGenGAP = (TextField) vBox.lookup("#n_gen_ga");

		TextArea equations = (TextArea) vBox.lookup("#equations");
		TextArea accuracy = (TextArea) vBox.lookup("#accuracy");
		TextField ndesc = (TextField) vBox.lookup("#ndesc");
		TextField lassoL1 = (TextField) vBox.lookup("#lassol1");
		TextField lassoThreshold = (TextField) vBox.lookup("#lassothreshold1");
		TextField l2field = (TextField) vBox.lookup("#l2field");
		ComboBox<String> combobox = (ComboBox) vBox.lookup("#selectionmethod");
		CheckBox errorBarsType = (CheckBox) vBox.lookup("#errorbarstype");
		CheckBox shuffleDescriptors = (CheckBox) vBox.lookup("#shuffledescriptors");
		CheckBox seqaddeq = (CheckBox) vBox.lookup("#seqaddeq");
		combobox.getItems().add("Sequental OLS RMSE");
		combobox.getItems().add("Sequental OLS MDAE");
		combobox.getItems().add("Sequental OLS, max F-score goodness-of-fit");
		combobox.getItems().add("PLS VIP");
		combobox.getItems().add("Boruta forest");
		combobox.getItems().add("Boruta gradient boosting");
		combobox.getItems().add("Seq removing forest");
		combobox.getItems().add("LASSO non sequental (required number of descriptors is ignored)");
		combobox.getItems().add("Genetic algorithm");
		combobox.getItems().add("No descriptor selection");

		// -fx-text-box-border: #8710e2;
		vBox.setStyle("-fx-focus-color: #8710e2;");
		vBox.applyCss();

		WebView webView = (WebView) vBox.lookup("#webview");
		Button button1 = (Button) vBox.lookup("#button1");
		Button buttonDescriptor = (Button) vBox.lookup("#descriptorbutton");
		Button buttonPredict = (Button) vBox.lookup("#predictbutton");
		Button buttonPredictDataset = (Button) vBox.lookup("#predictfordatasetbutton");
		Button buttonAllDescriptors = (Button) vBox.lookup("#alldescriptorsbutton");
		Button buttonAllDescriptorsDataset = (Button) vBox.lookup("#alldescriptorsdatasetbutton");
		Button buttonSelectDescriptors = (Button) vBox.lookup("#selectdescriptorsbutton");
		Button buttonChangeDescriptors = (Button) vBox.lookup("#changedescriptorsbutton");
		Button openButton = (Button) vBox.lookup("#openbutton");

		TextArea outArea = (TextArea) vBox2.lookup("#outfield");
		outArea.setEditable(false);
		AnchorPane tabWithCharts = (AnchorPane) vBox2.lookup("#tabwithcharts");
		TabPane resultsTabPane = (TabPane) vBox2.lookup("#resultstabpane");

		ImageView img = new ImageView(new Image((new File("./chereshnya2.png")).toURI().toString()));
		img.setFitHeight(60);
		img.setPreserveRatio(true);
		button1.setGraphic(img);

		CheckBox barChatHideCaptions = (CheckBox) vBox.lookup("#barchat_hide_captions");
		CheckBox barChatHideRdkit = (CheckBox) vBox.lookup("#barchat_hide_rdkit");
		CheckBox barChatHideY = (CheckBox) vBox.lookup("#barchat_hide_y");
		TextField axisFontBarchat = (TextField) vBox.lookup("#barchat_axisfont");
		TextField yTickBarchat = (TextField) vBox.lookup("#barchat_y_tick_spacing");
		TextField colorBarchat = (TextField) vBox.lookup("#barchat_color");
		TextField xLabeslAngleBarchat = (TextField) vBox.lookup("#barchat_x_labels_angle");

		String path = new java.io.File(".").getCanonicalPath();
		webView.getEngine().load("file://" + path + "/moledit.html");
		final JSJavaCall jsJavaCall = new JSJavaCall();
		webView.getEngine().getLoadWorker().stateProperty().addListener(new ChangeListener<Worker.State>() {
			@Override
			public void changed(@SuppressWarnings("rawtypes") ObservableValue observable, Worker.State oldValue,
					Worker.State newValue) {
				if (newValue != Worker.State.SUCCEEDED) {
					return;
				}
				JSObject window = (JSObject) webView.getEngine().executeScript("window");
				window.setMember("o", jsJavaCall);
			}
		});

		Mdl model = new Mdl();
		EventHandler<ActionEvent> runDescriptor = (new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent actionEvent) {
				try {
					String smiles = ChemUtils.canonical(jsJavaCall.smiles, false);
					String descriptorName = descriptorname.getText();
					descriptorvalue.setText(computeDescriptor(smiles, descriptorName) + "");
				} catch (Exception e) {
					e.printStackTrace();
					showMessageWithError(e);
				}
			}
		});

		EventHandler<ActionEvent> runAllDescriptors = (new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent actionEvent) {
				try {
					String smiles = ChemUtils.canonical(jsJavaCall.smiles, false);
					String s2 = "";
					QSRRModelRI m = (QSRRModelRI) model.model;
					String[] descriptorNames = m.getGen().getNames();
					for (int i = 0; i < descriptorNames.length; i++) {
						float d = computeDescriptor(smiles, descriptorNames[i]);
						s2 = s2 + descriptorNames[i] + " " + d + "\n";
					}
					outArea.setText(s2);
					resultsStage.show();
					resultsTabPane.getSelectionModel().select(0);
				} catch (Exception e) {
					e.printStackTrace();
					showMessageWithError(e);
				}
			}
		});

		EventHandler<ActionEvent> runPredict = (new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent actionEvent) {
				try {
					float ri = model.model.predict(jsJavaCall.smiles);
					rivalue.setText(ri + "");
				} catch (Exception e) {
					e.printStackTrace();
					showMessageWithError(e);
				}
			}
		});

		Gen gen1 = new Gen();
		FXMLLoader loader1 = new FXMLLoader();
		loader1.setLocation(new File("./selectdescriptors.fxml").toURI().toURL());
		VBox vBox1 = loader1.<VBox>load();
		Scene scene1 = new Scene(vBox1, 600, 200);
		Stage st1 = new Stage();
		st1.setTitle("Select descriptors set");
		st1.setScene(scene1);

		EventHandler<ActionEvent> runOK = (new EventHandler<ActionEvent>() {
			public void handle(ActionEvent actionEvent) {
				try {
					if (st1.isShowing()) {
						CheckBox cdkD = (CheckBox) vBox1.lookup("#cdk_d");
						CheckBox rdkitD = (CheckBox) vBox1.lookup("#rdkit_d");
						CheckBox sveklaD = (CheckBox) vBox1.lookup("#svekla_d");
						CheckBox klekotaD = (CheckBox) vBox1.lookup("#klekota_d");
						CheckBox linearriD = (CheckBox) vBox1.lookup("#linear_ri_d");
						CheckBox funcgroupsD = (CheckBox) vBox1.lookup("#funcgroups_d");

						ArrayList<FeaturesGenerator> lst = new ArrayList<FeaturesGenerator>();
						if (rdkitD.selectedProperty().get()) {
							lst.add(new RDKitDescriptorsGenerator());
						}
						if (cdkD.selectedProperty().get()) {
							lst.add(new CDKDescriptorsGenerator());
						}
						if (sveklaD.selectedProperty().get()) {
							lst.add(new SVEKLAGeneratorRI(sveklaPath.getText()));
						}
						if (klekotaD.selectedProperty().get()) {
							lst.add(new CDKFingerprintsGenerator(ChemUtils.FingerprintsType.KLEKOTA_ADDITIVE));
						}
						if (linearriD.selectedProperty().get()) {
							lst.add(new LinearModelRIGenerator());
						}
						if (funcgroupsD.selectedProperty().get()) {
							lst.add(new FuncGroupsCDKGenerator());
						}
						gen1.g = new CombinedFeaturesGenerator(lst.toArray(new FeaturesGenerator[lst.size()]));
						gen1.selectedDescriptors = new ArrayList<Integer>();
						st1.close();
					}
				} catch (Exception e) {
					e.printStackTrace();
					showMessageWithError(e);
				}
			}
		});

		EventHandler<ActionEvent> runSelect = (new EventHandler<ActionEvent>() {
			public void handle(ActionEvent actionEvent) {
				try {
					runOK.handle(actionEvent);
					FXMLLoader loader2 = new FXMLLoader();
					loader2.setLocation(new File("./empty.fxml").toURI().toURL());
					VBox vBox2 = loader2.<VBox>load();
					Scene scene2 = new Scene(vBox2, 1000, 750);
					Stage st2 = new Stage();
					st2.setTitle("Select specific descriptors");
					st2.setScene(scene2);
					st2.show();
					st2.getIcons().add(new Image((new File("./chereshnya1.png")).toURI().toURL().toString()));

					ScrollPane sp = (ScrollPane) vBox2.lookup("#scrollpane");
					TilePane pane = new TilePane();

					ArrayList<CheckBox> list = new ArrayList<CheckBox>();

					Button okButton = new Button();
					okButton.setText("OK");
					pane.getChildren().add(okButton);

					Button selectallButton = new Button();
					selectallButton.setText("Select all");
					selectallButton.setText("Select all");

					Button unselectallButton = new Button();
					unselectallButton.setText("Unselect all");
					unselectallButton.setText("Unselect all");

					pane.getChildren().add(selectallButton);
					pane.getChildren().add(unselectallButton);

					EventHandler<ActionEvent> runOK = (new EventHandler<ActionEvent>() {
						public void handle(ActionEvent actionEvent) {
							try {
								ArrayList<Integer> selectedDescriptors = new ArrayList<Integer>();
								for (int i = 0; i < list.size(); i++) {
									if (list.get(i).selectedProperty().getValue()) {
										selectedDescriptors.add(i);
									}
								}

								gen1.selectedDescriptors = selectedDescriptors;
								st2.close();
							} catch (Exception e) {
								e.printStackTrace();
								showMessageWithError(e);
							}
						}
					});

					EventHandler<ActionEvent> runSelectAll = (new EventHandler<ActionEvent>() {
						public void handle(ActionEvent actionEvent) {
							try {
								for (int i = 0; i < list.size(); i++) {
									list.get(i).setSelected(true);
								}
							} catch (Exception e) {
								e.printStackTrace();
								showMessageWithError(e);
							}
						}
					});

					EventHandler<ActionEvent> runUnSelectAll = (new EventHandler<ActionEvent>() {
						public void handle(ActionEvent actionEvent) {
							try {
								for (int i = 0; i < list.size(); i++) {
									list.get(i).setSelected(false);
								}
							} catch (Exception e) {
								e.printStackTrace();
								showMessageWithError(e);
							}
						}
					});

					for (int i = 0; i < gen1.g.getNumFeatures(); i++) {
						CheckBox checkbox = new CheckBox();
						checkbox.setText(gen1.g.getName(i));
						pane.getChildren().add(checkbox);
						list.add(checkbox);
						HashSet<Integer> hs = new HashSet<Integer>();
						hs.addAll(gen1.selectedDescriptors);
						if (hs.contains(i) || (hs.size() == 0)) {
							checkbox.setSelected(true);
						}
					}
					sp.setContent(pane);
					okButton.setOnAction(runOK);
					selectallButton.setOnAction(runSelectAll);
					unselectallButton.setOnAction(runUnSelectAll);

				} catch (Exception e) {
					e.printStackTrace();
					showMessageWithError(e);
				}
			}
		});

		EventHandler<ActionEvent> runSelectDescriptors = (new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent actionEvent) {
				try {
					st1.show();
					st1.getIcons().add(new Image((new File("./chereshnya1.png")).toURI().toURL().toString()));
					Button okButton = (Button) vBox1.lookup("#okbutton");
					Button specButton = (Button) vBox1.lookup("#selectbutton");
					okButton.setOnAction(runOK);
					specButton.setOnAction(runSelect);
				} catch (Exception e) {
					e.printStackTrace();
					showMessageWithError(e);
				}
			}
		});

		EventHandler<ActionEvent> runDescriptorsAllDataset = (new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent actionEvent) {
				try {
					QSRRModelRI m = (QSRRModelRI) model.model;
					FeaturesGenerator g = m.getGen();
					computeDescriptorsForDataSet(datafile2.getText(), outFileDescriptors.getText(), g);
					computeHeatmap();
					InputStream stream = new FileInputStream("./heatmap.png");
					Image image = new Image(stream);
					ImageView imageView = new ImageView();
					imageView.setImage(image);
					tabWithCharts.getChildren().clear();
					tabWithCharts.getChildren().add(imageView);
					resultsStage.show();
					resultsTabPane.getSelectionModel().select(1);
				} catch (Exception e) {
					e.printStackTrace();
					showMessageWithError(e);
				}
			}
		});

		EventHandler<ActionEvent> runPredictDataset = (new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent actionEvent) {
				try {
					String filename = datafile2.getText();
					BufferedReader br = new BufferedReader(new FileReader(filename));
					String s = br.readLine();
					String s2 = "";
					ArrayList<Pair<Float, Float>> predictedVsObserved = new ArrayList<Pair<Float, Float>>();
					while (s != null) {
						if (!s.trim().equals("")) {
							try {
								String smiles = s.trim().split("\\s+")[0];
								float ri = model.model.predict(smiles);
								s2 = s2 + s.trim() + " " + ri + "\n";
								try {
									float riObserved = Float.parseFloat(s.trim().split("\\s+")[1]);
									if (riObserved > 0) {
										Pair<Float, Float> p = Pair.of(ri, riObserved);
										predictedVsObserved.add(p);
									}
								} catch (Exception e) {
								}
							} catch (Exception e) {
								s2 = s2 + s.trim() + " PREDICTION_FAILED" + "\n";
								e.printStackTrace();
							}

						}
						s = br.readLine();
					}
					br.close();
					if (predictedVsObserved.size() > 1) {
						tabWithCharts.getChildren().clear();
						tabWithCharts.getChildren().add(predictedVsObservedChart(predictedVsObserved));
						s2 = predictedVsObservedAccuracy(predictedVsObserved) + "\n\n" + s2;
					}
					outArea.setText(s2);
					resultsStage.show();
				} catch (Exception e) {
					e.printStackTrace();
					showMessageWithError(e);
				}
			}
		});

		EventHandler<ActionEvent> runQSRR = (new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent actionEvent) {
				try {
					equations.setText("");
					accuracy.setText("");
					ArrayList<FeatureImportance[]> fiList = new ArrayList<FeatureImportance[]>();
					FeatureSelector fs = null;
					ChemDataset ds = null;
					FeaturesGenerator genFinal = null;

					int nTry = Integer.parseInt(nRepeats.getText());
					for (int cc = 0; cc < nTry; cc++) {
						ds = ChemDataset.loadFromFile(datafile.getText());
						ds.makeCanonicalAll(false);
						ds.makeCanonicalAll(false);
						String info = "N_molecules_init = " + ds.size() + "; ";

						int randomRemove = Integer.parseInt(excludernd.getText());
						ds.shuffle();
						ds.compoundsBasedSplitAndShuffle(randomRemove);
						info = info + "N_molecules = " + ds.size() + "; ";

						info = info + "N_descriptors_available = " + gen1.g.getNumFeatures() + "; ";
						if (gen1.selectedDescriptors.size() != 0) {
							info = info + "N_descriptors_init = " + gen1.selectedDescriptors.size() + "; ";
						}

						CombinedFeaturesPreprocessor p = new CombinedFeaturesPreprocessor();
						if (gen1.selectedDescriptors.size() != 0) {
							p.addPreprocessor(new SelectedFeaturesPreprocessor(gen1.selectedDescriptors));
						}
						if (shuffleDescriptors.selectedProperty().getValue()) {
							p.addPreprocessor(new ColumnShuffle());
						}
						p.addPreprocessor(new DropFeaturesWithNaNsPreprocessor(0.001F));
						p.addPreprocessor(new ReplaceNaNsPreprocessor());
						p.addPreprocessor(new DropConstantFeaturesPreprocessor());
						if (!highcorr.getText().trim().equals("")) {
							p.addPreprocessor(new DropHighCorrPreprocessor(Float.parseFloat(highcorr.getText())));
						}
						if (!lowvar.getText().trim().equals("")) {
							p.addPreprocessor(new DropLowVarPreprocessor(Float.parseFloat(lowvar.getText())));
						}

						p.train(gen1.g, ds);
						PreprocessedFeaturesGenerator g = new PreprocessedFeaturesGenerator(gen1.g, p);
						g.precompute(ds);
						info = info + "N_descriptors_preselected = " + g.getNumFeatures() + ";\n";
						equations.setText(equations.getText() + info);

						int n = Integer.parseInt(ndesc.getText());
						if (combobox.getSelectionModel().getSelectedIndex() == 0) {
							fs = new SeqAdditionOLSRMSE();
						}
						if (combobox.getSelectionModel().getSelectedIndex() == 1) {
							fs = new SeqAdditionOLSMDAE();
						}
						if (combobox.getSelectionModel().getSelectedIndex() == 2) {
							fs = new SeqAdditionBestF();
						}
						if (combobox.getSelectionModel().getSelectedIndex() == 3) {
							fs = new PLSVIP(Integer.parseInt(nCompPLS.getText()));
						}
						if (combobox.getSelectionModel().getSelectedIndex() == 4) {
							fs = new BorutaForest(Integer.parseInt(borutoRounds.getText()));
						}
						if (combobox.getSelectionModel().getSelectedIndex() == 5) {
							fs = new BorutaBoosting(Integer.parseInt(borutoRounds.getText()));
						}
						if (combobox.getSelectionModel().getSelectedIndex() == 6) {
							fs = new SeqRemovingForest(10);
						}
						if (combobox.getSelectionModel().getSelectedIndex() == 7) {
							fs = new LASSONonSequential(Float.parseFloat(lassoL1.getText()),
									Float.parseFloat(lassoThreshold.getText()));
						}
						if (combobox.getSelectionModel().getSelectedIndex() == 8) {
							fs = new GA(Integer.parseInt(nGenGAP.getText()));
						}
						if (combobox.getSelectionModel().getSelectedIndex() == 9) {
							fs = new NothingDoSelector();
						}

						if (FeatureSelector.SeqAddition.class.isInstance(fs)
								&& seqaddeq.selectedProperty().getValue()) {
							FeatureSelector.SeqAddition fs1 = (FeatureSelector.SeqAddition) fs;
							accuracy.setText(accuracy.getText() + "");
							while (fs1.getSelectedDescritptors().length < n) {
								fs1.addNext(ds, g, n);
								FeaturesPreprocessor select = new SelectedFeaturesPreprocessor(
										fs1.getSelectedDescritptors());
								select.train(g, ds);
								genFinal = new PreprocessedFeaturesGenerator(g, select);
								String[] eqAndAcc = trainModelGetEquationAndAccuracy(genFinal, ds,
										Float.parseFloat(l2field.getText()), model);
								equations.setText(equations.getText() + "\n" + eqAndAcc[0]);
								accuracy.setText(accuracy.getText() + "\n" + eqAndAcc[1]);
							}
							accuracy.setText(accuracy.getText() + "\n");
						} else {
							genFinal = fs.select(ds, g, n);
							String[] eqAndAc = null;
							if (LASSONonSequential.class.isInstance(fs)) {
								eqAndAc = trainModelGetEquationAndAccuracyLASSO(genFinal, ds, model,
										((LASSONonSequential) fs).getl1());
							} else {
								eqAndAc = trainModelGetEquationAndAccuracy(genFinal, ds,
										Float.parseFloat(l2field.getText()), model);
							}
							equations.setText(equations.getText() + eqAndAc[0] + "\n");
							accuracy.setText(accuracy.getText() + eqAndAc[1] + "\n");
						}
						String s2 = "";
						if (FeatureImportances.class.isInstance(fs)) {
							FeatureImportances fs1 = (FeatureImportances) fs;
							FeatureImportances.FeatureImportance[] imp = fs1.importances();
							fiList.add(imp);
						}
						s2 = s2 + minMaxAverageVariationInfo(genFinal, ds);
						outArea.setText(s2);
						resultsStage.show();
					}
					String s2 = "";
					if (FeatureImportances.class.isInstance(fs)) {
						FeatureImportances.FeatureImportance[] imp = FeatureImportances.averageImportances(fiList);
						System.out.println(imp.length + "x");
						Arrays.sort(imp);
						tabWithCharts.getChildren().clear();
						BarchatStyleSettings style = new BarchatStyleSettings(barChatHideCaptions, barChatHideRdkit,
								barChatHideY, axisFontBarchat, yTickBarchat, colorBarchat, xLabeslAngleBarchat);
						JPanel barchart = importancesChart(imp, nTry, errorBarsType.selectedProperty().get(), style);
						SwingNode sn = new SwingNode();
						sn.setContent(barchart);
						tabWithCharts.getChildren().add(sn);
						sn.maxWidth(900);
						sn.prefWidth(900);
						sn.maxHeight(700);
						sn.prefHeight(700);

						for (int i = 0; i < imp.length; i++) {
							s2 = s2 + imp[imp.length - 1 - i].name + " " + imp[imp.length - 1 - i].value;
							if (nTry > 1) {
								s2 = s2 + " (stdev: " + imp[imp.length - 1 - i].stdev + ";";
								s2 = s2 + " error range with p=95% : "
										+ 1.96 * imp[imp.length - 1 - i].stdev / ((float) Math.sqrt(nTry)) + ")";
							}
							s2 = s2 + "\n";
						}
					}
					s2 = s2 + minMaxAverageVariationInfo(genFinal, ds);
					outArea.setText(s2);
					resultsStage.show();
				} catch (Exception e) {
					e.printStackTrace();
					showMessageWithError(e);
				}
			}
		});

		EventHandler<ActionEvent> runOpenButton = (new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent actionEvent) {
				FileChooser fileChooser = new FileChooser();
				File file = fileChooser.showOpenDialog(primaryStage);
				if (file != null) {
					datafile.setText(file.getAbsolutePath());
				}
			}
		});

		button1.setOnAction(runQSRR);
		buttonDescriptor.setOnAction(runDescriptor);
		buttonAllDescriptors.setOnAction(runAllDescriptors);
		buttonPredict.setOnAction(runPredict);
		buttonPredictDataset.setOnAction(runPredictDataset);
		buttonSelectDescriptors.setOnAction(runSelectDescriptors);
		buttonChangeDescriptors.setOnAction(runSelect);
		buttonAllDescriptorsDataset.setOnAction(runDescriptorsAllDataset);
		openButton.setOnAction(runOpenButton);
	}

	public static void main(String[] args) {
		Application.launch(args);
	}
}
