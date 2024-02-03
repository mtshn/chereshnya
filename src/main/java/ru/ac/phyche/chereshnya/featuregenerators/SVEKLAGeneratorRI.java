package ru.ac.phyche.chereshnya.featuregenerators;

import java.io.File;
import java.io.FileWriter;
import java.util.HashSet;
import java.util.Scanner;

import org.apache.commons.lang3.SystemUtils;

import ru.ac.phyche.chereshnya.ChemUtils;

public class SVEKLAGeneratorRI extends FeaturesGenerator {

	private String sveklaPath = null;

	public SVEKLAGeneratorRI(String sveklaPath_) {
		this.sveklaPath = sveklaPath_;
	}

	@Override
	public void precompute(HashSet<String> smilesStrings) {
		if (smilesStrings.size() != 0) {
			try {
				String[] smiles = smilesStrings.toArray(new String[smilesStrings.size()]);
				File smilesFile = new File("smiles.txt");
				FileWriter fw = new FileWriter(smilesFile);
				for (int i = 0; i < smiles.length; i++) {
					fw.write(smiles[i] + "\n");
				}
				fw.close();

				ProcessBuilder b = null;
				String cmd = "cd \"" + sveklaPath
						+ "\"\njava -cp ./svekla-0.0.2-jar-with-dependencies.jar ru.ac.phyche.gcms.svekla.App2 Predict \""
						+ smilesFile.getAbsolutePath() + "\"";
				if (SystemUtils.IS_OS_LINUX || SystemUtils.IS_OS_MAC_OSX) {
					FileWriter sh = new FileWriter("svekla.sh");
					sh.write(cmd);
					sh.close();
					b = new ProcessBuilder("sh", "svekla.sh");
				}
				if (SystemUtils.IS_OS_WINDOWS) {
					FileWriter bat = new FileWriter("svekla.bat");
					cmd = cmd.replace("java", "\"./jdk-16.0.2/bin/java.exe\"");
					bat.write(cmd);
					bat.close();
					b = new ProcessBuilder("cmd", "/c", "svekla.bat");
				}

				b.directory(new File("./"));
				Process p = b.start();
				Scanner sc = new Scanner(p.getInputStream());
				boolean t = true;

				while (sc.hasNextLine() && t) {
					String s = sc.nextLine();
					if (s.contains("SMILES Squalane ")) {
						t = false;
					}
				}
				int x = 0;
				while (sc.hasNextLine()) {
					String s = sc.nextLine();
					String[] split = s.split("\\s+");
					String smilesCan1 = ChemUtils.canonical(ChemUtils.canonical(split[0], false), false);
					String smilesCan2 = ChemUtils.canonical(ChemUtils.canonical(smiles[x], false), false);
					if (!smilesCan1.equals(smilesCan2)) {
						sc.close();
						throw new RuntimeException("RI prediction with SVEKLA failed");
					}
					float[] features = new float[5];
					features[0] = Float.parseFloat(split[1]);
					features[1] = Float.parseFloat(split[2]);
					features[2] = Float.parseFloat(split[3]);
					features[3] = Float.parseFloat(split[4]);
					features[4] = Float.parseFloat(split[5]);
					this.putPrecomputed(smiles[x], features);
					x = x + 1;
				}
				if (x != smiles.length) {
					throw new RuntimeException("RI prediction with SVEKLA failed");
				}
			} catch (Exception e) {
				e.printStackTrace();
				throw new RuntimeException("RI prediction with SVEKLA failed");
			}
		}
	}

	@Override
	public String getName(int i) {
		if (i == 0) {
			return "SVEKLA_Squalane";
		}
		if (i == 1) {
			return "SVEKLA_DB-1";
		}
		if (i == 2) {
			return "SVEKLA_DB-5";
		}
		if (i == 3) {
			return "SVEKLA_DB-624";
		}
		if (i == 4) {
			return "SVEKLA_DB-WAX";
		}

		return null;
	}

	@Override
	public int getNumFeatures() {
		return 5;
	}

}
