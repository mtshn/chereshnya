package ru.ac.phyche.chereshnya;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

import org.openscience.cdk.exception.CDKException;

import ru.ac.phyche.chereshnya.featuregenerators.CDKDescriptorsGenerator;
import ru.ac.phyche.chereshnya.featuregenerators.CDKFingerprintsGenerator;
import ru.ac.phyche.chereshnya.featuregenerators.CombinedFeaturesGenerator;
import ru.ac.phyche.chereshnya.featuregenerators.FeaturesGenerator;
import ru.ac.phyche.chereshnya.featuregenerators.FuncGroupsCDKGenerator;
import ru.ac.phyche.chereshnya.featuregenerators.LinearModelRIGenerator;
import ru.ac.phyche.chereshnya.featuregenerators.MQNDescriptorsGenerator;
import ru.ac.phyche.chereshnya.featuregenerators.PreprocessedFeaturesGenerator;
import ru.ac.phyche.chereshnya.featuregenerators.RDKitDescriptorsGenerator;
import ru.ac.phyche.chereshnya.featurepreprocessors.CombinedFeaturesPreprocessor;
import ru.ac.phyche.chereshnya.featurepreprocessors.DropConstantFeaturesPreprocessor;
import ru.ac.phyche.chereshnya.featurepreprocessors.DropFeaturesWithNaNsPreprocessor;
import ru.ac.phyche.chereshnya.featurepreprocessors.DropHighCorrPreprocessor;
import ru.ac.phyche.chereshnya.featurepreprocessors.FeaturesPreprocessor;
import ru.ac.phyche.chereshnya.featurepreprocessors.ReplaceNaNsPreprocessor;
import ru.ac.phyche.chereshnya.featurepreprocessors.Scale01FeaturesPreprocessor;
import ru.ac.phyche.chereshnya.models.LibLinearRI;
import ru.ac.phyche.chereshnya.models.ModelRI;

public class WAXRILinear {

	private static FeaturesPreprocessor preproc() {
		CombinedFeaturesPreprocessor p = new CombinedFeaturesPreprocessor();
		p.addPreprocessor(new DropFeaturesWithNaNsPreprocessor(0.001F));
		p.addPreprocessor(new ReplaceNaNsPreprocessor());
		p.addPreprocessor(new DropConstantFeaturesPreprocessor());
		// features with correlation >0.999 are always excluded
		p.addPreprocessor(new DropHighCorrPreprocessor(0.999f));
		p.addPreprocessor(new Scale01FeaturesPreprocessor());
		return p;
	}

	private static FeaturesGenerator gen() {
		ArrayList<FeaturesGenerator> lst = new ArrayList<FeaturesGenerator>();
		lst.add(new CDKDescriptorsGenerator());
		lst.add(new RDKitDescriptorsGenerator());
		lst.add(new FuncGroupsCDKGenerator());
		lst.add(new MQNDescriptorsGenerator());
		lst.add(new CDKFingerprintsGenerator(ChemUtils.FingerprintsType.KLEKOTA_ADDITIVE));
		CombinedFeaturesGenerator gen1 = new CombinedFeaturesGenerator(lst.toArray(new FeaturesGenerator[lst.size()]));
		return gen1;
	}

	public static ModelRI load(String modelDirName) throws IOException {
		FeaturesPreprocessor p2 = preproc();
		FeaturesGenerator gen2=gen();
		BufferedReader br = new BufferedReader(new FileReader(new File(modelDirName,"preproc")));
		p2.load(br);
		br.close();
		ModelRI m2 = new LibLinearRI(new PreprocessedFeaturesGenerator(gen2, p2));
		m2.load(modelDirName);
		return m2;
	}

	
	public static String[] trainValidateModel(String trainSet,String testSet, String[] excludeSetsFiles, String modelDirName, String tmpfile) throws Exception {
		ChemDataset train = ChemDataset.loadFromFile(trainSet).medianByCompounds(false);
		ChemDataset test = ChemDataset.loadFromFile(testSet);//.medianByCompounds(false);
		train.makeCanonicalAll(false);
		test.makeCanonicalAll(false);
		train.filterIdenticalByInchi(test);
		train.filterIdentical(test);
		
		for (int i=0;i<excludeSetsFiles.length;i++) {
			ChemDataset f = ChemDataset.loadFromFile(excludeSetsFiles[i]);
			f.makeCanonicalAll(false);
			train.filterIdenticalByInchi(f);
			train.filterIdentical(f);
		}
		
		FeaturesPreprocessor p = preproc();
		FeaturesGenerator gen1=gen();
		p.train(gen1, train);
		PreprocessedFeaturesGenerator g = new PreprocessedFeaturesGenerator(gen1, p);
		g.precompute(train);
		ModelRI m1 = new LibLinearRI(g, true, tmpfile, 250);
		ChemDataset val = train.compoundsBasedSplitAndShuffle(0.1f);
		m1.train(train, val);
		String s1 = m1.validate(val);
		String s2 = m1.validate(test);
		m1.save(modelDirName);
		FileWriter fw = new FileWriter(new File(modelDirName,"preproc"));
		p.save(fw);
		fw.close();
		m1=null;
		p=null;
		g=null;
		gen1=null;

		FeaturesPreprocessor p2 = preproc();
		FeaturesGenerator gen2=gen();
		BufferedReader br = new BufferedReader(new FileReader(new File(modelDirName,"preproc")));
		p2.load(br);
		br.close();
		ModelRI m2 = new LibLinearRI(new PreprocessedFeaturesGenerator(gen2, p2));
		m2.load(modelDirName);
		
		String s3 = m2.validate(test);
		System.out.println(s1);
		System.out.println(s2);
		System.out.println(s3);
		return new String[] {s1,s2,s3};
	}
	
	public static void main(String[] args) throws Exception {
		ModelRI m2 = WAXRILinear.load("ri_linear_model_polar");
		//String s= m2.validate(ChemDataset.loadFromFile("./nistPolar_split0.ri"));
		System.out.println();
		//System.out.println(s);
		FeaturesGenerator gen = new LinearModelRIGenerator();
		gen.precompute(new String[] {"CCCCCO"});
		System.out.println(m2.predict("CCCCCO"));
		System.out.println(gen.features(new String[] {"CCCCCO"})[0][1]);
		System.exit(0);
		
		String[] a = trainValidateModel("nistPolar.ri","nistPolar_split0.ri",new String[]{"il1.txt","il2.txt","il3.txt"},"ri_linear_model_polar","tune_polar.tmp");
//		String[] b = trainValidateModel("nist.ri","nist_CV_split0.ri",new String[]{"il1.txt","il2.txt","il3.txt"},"ri_linear_model_nonpolar","tune_nonpolar.tmp");
		System.out.println("\n\n\nWAX\n");
		System.out.println(a[0]);
		System.out.println(a[1]);
		System.out.println(a[2]);
//		System.out.println("\n\n\nNon-polar\n");
//		System.out.println(b[0]);
//		System.out.println(b[1]);
//		System.out.println(b[2]);
	}
}
/**  New accuracy, trained here
 * 
 * 
WAX

RMSE: 160.27686 MAE: 80.103294 MdAE: 35.739136 MPE: 5.1960297 MdPE: 2.1060014
R2: 0.903116 r: 0.9503242
d80abs 112.03296 d90abs 207.02588 d95abs 299.39697
d80rel 6.4535275 d90rel 10.70957 d95rel 15.08292
RMSE: 98.41147 MAE: 53.26173 MdAE: 31.270996 MPE: 3.728054 MdPE: 2.0799837
R2: 0.95299196 r: 0.9762131
d80abs 74.44446 d90abs 113.79468 d95abs 201.82898
d80rel 5.4774857 d90rel 8.322674 d95rel 10.858587
RMSE: 98.41337 MAE: 53.26202 MdAE: 31.270996 MPE: 3.72803 MdPE: 2.0799837
R2: 0.95298946 r: 0.9762118
d80abs 74.44446 d90abs 113.79468 d95abs 201.82898
d80rel 5.4774857 d90rel 8.322674 d95rel 10.858587


Non-polar

RMSE: 116.42104 MAE: 56.173527 MdAE: 28.075684 MPE: 2.965321 MdPE: 1.4290766
R2: 0.97144437 r: 0.98561877
d80abs 73.0061 d90abs 125.0979 d95abs 196.42944
d80rel 3.9256997 d90rel 6.6789865 d95rel 10.268302
RMSE: 71.14678 MAE: 39.78646 MdAE: 25.737915 MPE: 2.7898648 MdPE: 1.8773689
R2: 0.9869931 r: 0.99347526
d80abs 59.37671 d90abs 81.772644 d95abs 116.890625
d80rel 4.1882114 d90rel 6.535453 d95rel 7.970962
RMSE: 71.0497 MAE: 39.63522 MdAE: 25.365356 MPE: 2.774463 MdPE: 1.8588305
R2: 0.9870324 r: 0.99349505
d80abs 59.44043 d90abs 81.772644 d95abs 116.84082
d80rel 4.159489 d90rel 6.535453 d95rel 7.955617
 * 
 */





/* Accuracy polar (wax)!
 * RMSE: 100.43359 MAE: 53.63143 MdAE: 31.357788 MPE: 3.6810193 MdPE: 2.0619125
R2: 0.95075214 r: 0.9750652
d80abs 72.68164 d90abs 106.565674 d95abs 201.21509
d80rel 5.381315 d90rel 7.939996 d95rel 11.617182
 * */


