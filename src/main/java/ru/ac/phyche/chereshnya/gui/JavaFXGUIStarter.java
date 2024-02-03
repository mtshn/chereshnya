package ru.ac.phyche.chereshnya.gui;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.concurrent.atomic.AtomicInteger;

import org.openscience.cdk.exception.CDKException;

import ru.ac.phyche.chereshnya.ArUtls;
import ru.ac.phyche.chereshnya.ChemDataset;
import ru.ac.phyche.chereshnya.ChemUtils;

public class JavaFXGUIStarter {
	public static void main(String[] args) throws IOException, CDKException {
/*
		BufferedReader br1 = new BufferedReader(new FileReader("il1.txt"));
		BufferedReader br2 = new BufferedReader(new FileReader("il2.txt"));
		BufferedReader br3 = new BufferedReader(new FileReader("il3.txt"));
		BufferedReader br4 = new BufferedReader(new FileReader("nistPolar_joined.txt"));
		BufferedReader br5 = new BufferedReader(new FileReader("nist_joined.txt"));
		String s;
		HashSet<String> sm1 = new HashSet<String>();
		s = br1.readLine();
		while (s != null) {
			if (!s.trim().equals("")) {
				sm1.add(ChemUtils.canonical(s.trim().split("\\s+")[0], false));
			}
			s = br1.readLine();
		}

		HashSet<String> sm2 = new HashSet<String>();
		s = br2.readLine();
		while (s != null) {
			if (!s.trim().equals("")) {
				sm2.add(ChemUtils.canonical(s.trim().split("\\s+")[0], false));
			}
			s = br2.readLine();
		}

		HashSet<String> sm3 = new HashSet<String>();
		s = br3.readLine();
		while (s != null) {
			if (!s.trim().equals("")) {
				sm3.add(ChemUtils.canonical(s.trim().split("\\s+")[0], false));
			}
			s = br3.readLine();
		}

		HashMap<String,String> canonicalByNonCanonical = new HashMap<String,String> ();
		
		HashSet<String> sm4a = new HashSet<String>();
		s = br4.readLine();
		int k4 = 0;
		while (s != null) {
			if (!s.trim().equals("")) {
				sm4a.add(s.trim().split("\\s+")[0]);
				k4++;
				if (k4 % 10000 == 0) {
					System.out.println(k4);
				}
			}
			s = br4.readLine();
		}
		ArrayList<String> sm4b = new ArrayList<String>();
		sm4b.addAll(sm4a);
		String[] sm4c = new String[sm4b.size()];
		AtomicInteger ai4 = new AtomicInteger(0);
		Arrays.stream(ArUtls.ints(sm4b.size())).parallel().forEach(i -> {
			int x = ai4.incrementAndGet();
			if (x % 10000 == 0) {
				System.out.println(x);
			}
			try {
				sm4c[i] = ChemUtils.canonical(sm4b.get(i), false);
			} catch (CDKException e) {
				throw new RuntimeException(e.getLocalizedMessage());
			}
		});
		HashSet<String> sm4 = new HashSet<String>();
		for (int i = 0; i < sm4c.length; i++) {
			sm4.add(sm4c[i]);
			canonicalByNonCanonical.put(sm4b.get(i), sm4c[i]);
		}

		HashSet<String> sm5a = new HashSet<String>();
		s = br5.readLine();
		int k5 = 0;
		while (s != null) {
			if (!s.trim().equals("")) {
				sm5a.add(s.trim().split("\\s+")[0]);
				k5++;
				if (k5 % 10000 == 0) {
					System.out.println(k5);
				}
			}
			s = br5.readLine();
		}
		ArrayList<String> sm5b = new ArrayList<String>();
		sm5b.addAll(sm5a);
		String[] sm5c = new String[sm5b.size()];
		AtomicInteger ai5 = new AtomicInteger(0);
		Arrays.stream(ArUtls.ints(sm5b.size())).parallel().forEach(i -> {
			int x = ai5.incrementAndGet();
			if (x % 10000 == 0) {
				System.out.println(x);
			}
			try {
				sm5c[i] = ChemUtils.canonical(sm5b.get(i), false);
			} catch (CDKException e) {
				throw new RuntimeException(e.getLocalizedMessage());
			}
		});
		HashSet<String> sm5 = new HashSet<String>();
		for (int i = 0; i < sm5c.length; i++) {
			sm5.add(sm5c[i]);
			canonicalByNonCanonical.put(sm5b.get(i), sm5c[i]);
		}
		br1.close();
		br2.close();
		br3.close();
		br4.close();
		br5.close();

		HashSet<String> total = new HashSet<String>();
		total.addAll(sm1);
		total.addAll(sm2);
		total.addAll(sm3);
		total.addAll(sm4);
		total.addAll(sm5);

		total.retainAll(sm1);
		total.retainAll(sm2);
		total.retainAll(sm3);
		total.retainAll(sm4);
		total.retainAll(sm5);

		br1 = new BufferedReader(new FileReader("il1.txt"));
		br2 = new BufferedReader(new FileReader("il2.txt"));
		br3 = new BufferedReader(new FileReader("il3.txt"));
		br4 = new BufferedReader(new FileReader("nistPolar_joined.txt"));
		br5 = new BufferedReader(new FileReader("nist_joined.txt"));
		FileWriter fw1 = new FileWriter("il1_e.txt");
		FileWriter fw2 = new FileWriter("il2_e.txt");
		FileWriter fw3 = new FileWriter("il3_e.txt");
		FileWriter fw4 = new FileWriter("wax_nist.tmp");
		FileWriter fw5 = new FileWriter("db5_nist.tmp");

		s = br1.readLine();
		while (s != null) {
			if (!s.trim().equals("")) {
				String sm = ChemUtils.canonical(s.trim().split("\\s+")[0], false);
				if (total.contains(sm)) {
					fw1.write(s + "\n");
				}
			}
			s = br1.readLine();
		}

		s = br2.readLine();
		while (s != null) {
			if (!s.trim().equals("")) {
				String sm = ChemUtils.canonical(s.trim().split("\\s+")[0], false);
				if (total.contains(sm)) {
					fw2.write(s + "\n");
				}
			}
			s = br2.readLine();
		}

		s = br3.readLine();
		while (s != null) {
			if (!s.trim().equals("")) {
				String sm = ChemUtils.canonical(s.trim().split("\\s+")[0], false);
				if (total.contains(sm)) {
					fw3.write(s + "\n");
				}
			}
			s = br3.readLine();
		}

		s = br4.readLine();
		while (s != null) {
			if (!s.trim().equals("")) {
				String sm = canonicalByNonCanonical.get(s.trim().split("\\s+")[0]);
				if (total.contains(sm)) {
					fw4.write(s + "\n");
				}
			}
			s = br4.readLine();
		}

		s = br5.readLine();
		while (s != null) {
			if (!s.trim().equals("")) {
				String sm = canonicalByNonCanonical.get(s.trim().split("\\s+")[0]);
				if (total.contains(sm)) {
					fw5.write(s + "\n");
				}
			}
			s = br5.readLine();
		}

		br1.close();
		br2.close();
		br3.close();
		br4.close();
		br5.close();
		fw1.close();
		fw2.close();
		fw3.close();
		fw4.close();
		fw5.close();

		ChemDataset p = ChemDataset.loadFromFile("wax_nist.tmp");
		p.makeCanonicalAll(false);
		p = p.medianByCompounds(false);
		p.saveToFile("wax_nist_e.txt");
		ChemDataset np = ChemDataset.loadFromFile("db5_nist.tmp");
		np.makeCanonicalAll(false);
		np = np.medianByCompounds(false);
		np.saveToFile("db5_nist_e.txt");
				
		System.exit(0);
*/
		/*
		 * BufferedReader br1 = new BufferedReader(new FileReader("il1.txt"));
		 * BufferedReader br2 = new BufferedReader(new FileReader("il2.txt"));
		 * BufferedReader br3 = new BufferedReader(new FileReader("il3.txt"));
		 * BufferedReader br4 = new BufferedReader(new FileReader("WAX.txt"));
		 * BufferedReader br5 = new BufferedReader(new FileReader("db5.txt")); String s;
		 * HashSet<String> sm1 = new HashSet<String>(); s = br1.readLine(); while
		 * (s!=null) { if (!s.trim().equals("")) {
		 * sm1.add(ChemUtils.canonical(s.trim().split("\\s+")[0], false)); } s =
		 * br1.readLine(); }
		 * 
		 * HashSet<String> sm2 = new HashSet<String>(); s = br2.readLine(); while
		 * (s!=null) { if (!s.trim().equals("")) {
		 * sm2.add(ChemUtils.canonical(s.trim().split("\\s+")[0], false)); } s =
		 * br2.readLine(); }
		 * 
		 * HashSet<String> sm3 = new HashSet<String>(); s = br3.readLine(); while
		 * (s!=null) { if (!s.trim().equals("")) {
		 * sm3.add(ChemUtils.canonical(s.trim().split("\\s+")[0], false)); } s =
		 * br3.readLine(); }
		 * 
		 * HashSet<String> sm4 = new HashSet<String>(); s = br4.readLine(); while
		 * (s!=null) { if (!s.trim().equals("")) {
		 * sm4.add(ChemUtils.canonical(s.trim().split("\\s+")[0], false)); } s =
		 * br4.readLine(); }
		 * 
		 * HashSet<String> sm5 = new HashSet<String>(); s = br5.readLine(); while
		 * (s!=null) { if (!s.trim().equals("")) {
		 * sm5.add(ChemUtils.canonical(s.trim().split("\\s+")[0], false)); } s =
		 * br5.readLine(); }
		 * 
		 * br1.close(); br2.close(); br3.close(); br4.close(); br5.close();
		 * 
		 * HashSet<String> total = new HashSet<String>(); total.addAll(sm1);
		 * total.addAll(sm2); total.addAll(sm3); total.addAll(sm4); total.addAll(sm5);
		 * 
		 * total.retainAll(sm1); total.retainAll(sm2); total.retainAll(sm3);
		 * total.retainAll(sm4); total.retainAll(sm5);
		 * 
		 * br1 = new BufferedReader(new FileReader("il1.txt")); br2 = new
		 * BufferedReader(new FileReader("il2.txt")); br3 = new BufferedReader(new
		 * FileReader("il3.txt")); br4 = new BufferedReader(new FileReader("WAX.txt"));
		 * br5 = new BufferedReader(new FileReader("db5.txt")); FileWriter fw1 = new
		 * FileWriter("il1_d.txt"); FileWriter fw2 = new FileWriter("il2_d.txt");
		 * FileWriter fw3 = new FileWriter("il3_d.txt"); FileWriter fw4 = new
		 * FileWriter("wax_d.txt"); FileWriter fw5 = new FileWriter("db5_d.txt");
		 * 
		 * s = br1.readLine(); while (s!=null) { if (!s.trim().equals("")) { String sm =
		 * ChemUtils.canonical(s.trim().split("\\s+")[0], false); if
		 * (total.contains(sm)) { fw1.write(s+"\n"); } } s = br1.readLine(); }
		 * 
		 * s = br2.readLine(); while (s!=null) { if (!s.trim().equals("")) { String sm =
		 * ChemUtils.canonical(s.trim().split("\\s+")[0], false); if
		 * (total.contains(sm)) { fw2.write(s+"\n"); } } s = br2.readLine(); }
		 * 
		 * s = br3.readLine(); while (s!=null) { if (!s.trim().equals("")) { String sm =
		 * ChemUtils.canonical(s.trim().split("\\s+")[0], false); if
		 * (total.contains(sm)) { fw3.write(s+"\n"); } } s = br3.readLine(); }
		 * 
		 * s = br4.readLine(); while (s!=null) { if (!s.trim().equals("")) { String sm =
		 * ChemUtils.canonical(s.trim().split("\\s+")[0], false); if
		 * (total.contains(sm)) { fw4.write(s+"\n"); } } s = br4.readLine(); }
		 * 
		 * s = br5.readLine(); while (s!=null) { if (!s.trim().equals("")) { String sm =
		 * ChemUtils.canonical(s.trim().split("\\s+")[0], false); if
		 * (total.contains(sm)) { fw5.write(s+"\n"); } } s = br5.readLine(); }
		 * 
		 * br1.close(); br2.close(); br3.close(); br4.close(); br5.close(); fw1.close();
		 * fw2.close(); fw3.close(); fw4.close(); fw5.close();
		 * 
		 * System.exit(0);
		 */

		JavaFXGUI.main(args);
	}
}
