package ru.ac.phyche.hplc_metlin_predict;

import java.io.IOException;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.HashMap;
import java.util.HashSet;

import org.openscience.cdk.DefaultChemObjectBuilder;
import org.openscience.cdk.aromaticity.Aromaticity;
import org.openscience.cdk.aromaticity.ElectronDonation;
import org.openscience.cdk.config.Isotopes;
import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.fingerprint.CircularFingerprinter;
import org.openscience.cdk.fingerprint.ICountFingerprint;
import org.openscience.cdk.fingerprint.KlekotaRothFingerprinter;
import org.openscience.cdk.fingerprint.LingoFingerprinter;
import org.openscience.cdk.fingerprint.MACCSFingerprinter;
import org.openscience.cdk.fingerprint.PubchemFingerprinter;
import org.openscience.cdk.graph.Cycles;
import org.openscience.cdk.inchi.InChIGenerator;
import org.openscience.cdk.inchi.InChIGeneratorFactory;
import org.openscience.cdk.inchi.InChIToStructure;
import org.openscience.cdk.interfaces.IAtom;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IBond;
import org.openscience.cdk.qsar.descriptors.molecular.WeightDescriptor;
import org.openscience.cdk.qsar.result.DoubleResult;
import org.openscience.cdk.smiles.SmiFlavor;
import org.openscience.cdk.smiles.SmilesGenerator;
import org.openscience.cdk.smiles.SmilesParser;
import org.openscience.cdk.tools.CDKHydrogenAdder;
import org.openscience.cdk.tools.manipulator.AtomContainerManipulator;

import io.github.dan2097.jnainchi.InchiStatus;


public class GNNChemUtils {

	/**
	 * Possible letters in SMILES string (important for 1D CNN, other symbols are
	 * not supported). Space must be first. Any change here will require
	 * (immediately) retraining of 1D-CNN model.
	 */
	public static final char[] TOKENS = { ' ', 'C', 'c', 'N', 'n', 'H', 'O', 'o', 'F', 'B', 'l', 'r', 'S', 'i', 's',
			'P', 'I', '+', '(', ')', '[', ']', '-', '=', '#', '1', '2', '3', '4', '5', '6', '7', '8', '9', '0', '%',
			'\\', '/', '@' };
	// The space always comes first! The space has index 0!

	public static final int SMILES_TOKENS = TOKENS.length;

	public static enum FingerprintsType {
		NONE, MACCS, CIRCULAR_4_1024, CIRCULAR_6_1024, LINGO, PUBCHEM, KLEKOTA, KLEKOTA_ADDITIVE, CIRCULAR_6_4096,
		ADDITIVE_CIRCULAR_4_1024_NO_SCALE, ADDITIVE_CIRCULAR_6_4096_NO_SCALE, ADDITIVE_CIRCULAR_6_1024_NO_SCALE,
		CIRCULAR_4_512;
	}

	public static final int MAXMW = 1500;
	public static final int SMILES_MAX_LENGTH = 350;

	public static class GraphMol {

		private static final int DIGITS = 4;
		public static final DecimalFormat FORMAT = getDecimalFormat();

		public static class EdgeC {
			public int node1;
			public int node2;
			public float[] features;
		}

		private int nEdgeFeatures = 0;
		private EdgeC[] edges;
		private float[][] nodeFeatures;
		private String comment;
		private int[] elementAtomicNumber;
		private int[] nHydrogens;

		public GraphMol copy() {
			GraphMol r = new GraphMol(nodeFeatures.length, nEdgeFeatures, edges.length, nEdgeFeatures);
			r.edges = this.edges.clone();
			r.nodeFeatures = this.nodeFeatures.clone();
			r.nHydrogens = this.nHydrogens.clone();
			r.elementAtomicNumber = this.elementAtomicNumber.clone();
			r.comment = this.comment;
			r.nEdgeFeatures = this.nEdgeFeatures;
			return r;
		}

		public int getNNodes() {
			return nodeFeatures.length;
		}

		public EdgeC[] getEdges() {
			return edges;
		}

		public int getNEdges() {
			return edges.length;
		}

		public EdgeC getEdgen(int n) {
			return edges[n];
		}

		public int[] neighborsAtoms(int n) {
			ArrayList<Integer> l = new ArrayList<Integer>();
			for (int i = 0; i < edges.length; i++) {
				EdgeC x = edges[i];
				if (x.node1 == n) {
					l.add(x.node2);
				}
				if (x.node2 == n) {
					l.add(x.node1);
				}
			}
			int[] result = new int[l.size()];
			for (int i = 0; i < result.length; i++) {
				result[i] = l.get(i);
			}
			return result;
		}

		public int[] neighborsBonds(int n) {
			ArrayList<Integer> l = new ArrayList<Integer>();
			for (int i = 0; i < edges.length; i++) {
				EdgeC x = edges[i];
				if ((x.node1 == n) || (x.node2 == n)) {
					l.add(i);
				}
			}
			int[] result = new int[l.size()];
			for (int i = 0; i < result.length; i++) {
				result[i] = l.get(i);
			}
			return result;
		}

		public float[][] featuresOfNeighborNodes(int n) {
			int[] x = neighborsAtoms(n);
			float[][] r = new float[x.length][];
			for (int i = 0; i < x.length; i++) {
				r[i] = this.nodeFeatures[x[i]];
			}
			return r;
		}

		public float[][] featuresOfNeighborEdges(int n) {
			int[] x = neighborsBonds(n);
			float[][] r = new float[x.length][];
			for (int i = 0; i < x.length; i++) {
				r[i] = this.edges[x[i]].features;
			}
			return r;
		}

		private static DecimalFormat getDecimalFormat() {
			DecimalFormat f = new DecimalFormat();
			f.setMaximumFractionDigits(DIGITS);
			DecimalFormatSymbols s = DecimalFormatSymbols.getInstance();
			s.setDecimalSeparator('.');
			f.setDecimalFormatSymbols(s);
			f.setGroupingUsed(false);
			return f;
		}

		public GraphMol(int nNodes, int nNodeFeatures, int nEdges, int nEdgeFeatures) {
			this.edges = new EdgeC[nEdges];
			this.nEdgeFeatures = nEdgeFeatures;
			nodeFeatures = new float[nNodes][nNodeFeatures];
			elementAtomicNumber = new int[nNodes];
			nHydrogens = new int[nNodes];
		}

		public GraphMol(int nNodes, int nNodeFeatures, int nEdges, int nEdgeFeatures, String comment) {
			this.edges = new EdgeC[nEdges];
			this.nEdgeFeatures = nEdgeFeatures;
			nodeFeatures = new float[nNodes][nNodeFeatures];
			this.comment = comment;
			elementAtomicNumber = new int[nNodes];
			nHydrogens = new int[nNodes];
		}

		public void setFeaturesOfNode(int n, int elementAtomicNumber, int nHydrogens, float[] features) {
			nodeFeatures[n] = features;
			this.elementAtomicNumber[n] = elementAtomicNumber;
			this.nHydrogens[n] = nHydrogens;
		}

		public void setFeaturesOfNode(int n, float[] features) {
			nodeFeatures[n] = features;
		}

		public float[] getFeaturesOfNode(int n) {
			return nodeFeatures[n];
		}

		public float[][] getNodeFeatures() {
			return this.nodeFeatures;
		}

		public void addEdge(int node1, int node2, float[] features) {
			if (features.length != nEdgeFeatures) {
				throw new RuntimeException();
			}
			int i = 0;
			while (edges[i] != null) {
				i++;
			}
			edges[i] = new EdgeC();
			edges[i].node1 = node1;
			edges[i].node2 = node2;
			edges[i].features = features;
		}

		public String graphToString() {
			String result = "";
			if (comment != null) {
				result = result + comment + " ";
			}
			result = result + nodeFeatures.length + " " + nodeFeatures[0].length + " " + edges.length + " "
					+ nEdgeFeatures;
			for (int i = 0; i < elementAtomicNumber.length; i++) {
				result = result + " " + elementAtomicNumber[i];
			}
			for (int i = 0; i < elementAtomicNumber.length; i++) {
				result = result + " " + nHydrogens[i];
			}
			for (int i = 0; i < edges.length; i++) {
				result = result + " " + edges[i].node1 + " " + edges[i].node2;
			}
			for (int i = 0; i < edges.length; i++) {
				for (int j = 0; j < nEdgeFeatures; j++) {
					result = result + " " + edges[i].features[j];
				}
			}
			for (int i = 0; i < nodeFeatures.length; i++) {
				for (int j = 0; j < nodeFeatures[0].length; j++) {
					result = result + " " + nodeFeatures[i][j];
				}
			}
			return result;
		}

		public String graphToStringMultiline() {
			return graphToStringMultiline(false);
		}

		public String graphToStringMultiline(boolean onlyAtoms) {
			String result = "";
			if (!onlyAtoms) {
				if (comment != null) {
					result = result + comment + "\n";
				}
				result = result + nodeFeatures.length + " " + nodeFeatures[0].length + " " + edges.length + " "
						+ nEdgeFeatures + "\n";
				for (int i = 0; i < edges.length; i++) {
					result = result + "edge " + i + " " + edges[i].node1 + " " + edges[i].node2 + "\n";
				}
				for (int i = 0; i < edges.length; i++) {
					result = result + "edge " + i;
					for (int j = 0; j < nEdgeFeatures; j++) {
						result = result + " " + edges[i].features[j];
					}
					result = result + "\n";
				}
			}
			for (int i = 0; i < nodeFeatures.length; i++) {
				result = result + "node " + i + " " + elementAtomicNumber[i] + " " + nHydrogens[i];
				for (int j = 0; j < nodeFeatures[0].length; j++) {
					result = result + " " + nodeFeatures[i][j];
				}
				result = result + "\n";
			}
			return result;
		}
	}

	public static String canonicalizeMol(String smiles) throws CDKException, IOException {
		IAtomContainer mol = smilesToMol(smiles);
		String inchikey14First = molToInchiKey(mol).split("\\-")[0];
		mol = preprocessMol(inchiToMol(molToInchi(preprocessMol(mol))));
		String inchikey14Second = molToInchiKey(mol).split("\\-")[0];
		String result = molToSmiles(mol);
		String inchikey14Third = molToInchiKey(smilesToMol(result)).split("\\-")[0];
		if (!inchikey14First.equals(inchikey14Second)) {
			throw (new CDKException("inchikey changed during conversion mol-inchi-mol using CDK"));
		}
		if (!inchikey14Third.equals(inchikey14Second)) {
			throw (new CDKException("inchikey changed during conversion mol-smiles-mol using CDK"));
		}
		return result;
	}

	public static String molToSmiles(IAtomContainer mol) throws CDKException {
		Aromaticity arom = new Aromaticity(ElectronDonation.cdk(), Cycles.cdkAromaticSet());
		AtomContainerManipulator.percieveAtomTypesAndConfigureAtoms(mol);
		AtomContainerManipulator.suppressHydrogens(mol);
		arom.apply(mol);
		CDKHydrogenAdder adder = CDKHydrogenAdder.getInstance(mol.getBuilder());
		adder.addImplicitHydrogens(mol);
		for (IAtom a : mol.atoms()) {
			a.setMassNumber(null);
		}
		SmilesGenerator sg;
		sg = new SmilesGenerator(SmiFlavor.Unique | SmiFlavor.UseAromaticSymbols ^ SmiFlavor.AtomicMass);
		String smiles = sg.create(mol);
		return smiles;
	}

	public static IAtomContainer inchiToMol(String inchi) throws CDKException {
		InChIGeneratorFactory factory = InChIGeneratorFactory.getInstance();
		InChIToStructure intostruct = factory.getInChIToStructure(inchi, DefaultChemObjectBuilder.getInstance());
		InchiStatus ret = intostruct.getStatus();
		if (ret.equals(InchiStatus.ERROR)) {
			throw (new CDKException("Inchi status failed!"));
		}
		IAtomContainer mol = intostruct.getAtomContainer();
		return mol;
	}

	public static IAtomContainer smilesToMol(String smiles) throws CDKException {
		SmilesParser parser = new SmilesParser(DefaultChemObjectBuilder.getInstance());
		IAtomContainer mol = parser.parseSmiles(smiles.trim());
		return mol;
	}

	public static IAtomContainer preprocessMol(IAtomContainer mol) throws CDKException, IOException {
		Aromaticity arom = new Aromaticity(ElectronDonation.cdk(), Cycles.cdkAromaticSet());
		AtomContainerManipulator.percieveAtomTypesAndConfigureAtoms(mol);
		arom.apply(mol);
		CDKHydrogenAdder adder = CDKHydrogenAdder.getInstance(mol.getBuilder());
		adder.addImplicitHydrogens(mol);
		// AtomContainerManipulator.convertImplicitToExplicitHydrogens(mol);
		for (IAtom a : mol.atoms()) {
			a.setMassNumber(Isotopes.getInstance().getMajorIsotope(a.getAtomicNumber()).getMassNumber());
		}
		Isotopes.getInstance().configureAtoms(mol);
		// AtomContainerManipulator.percieveAtomTypesAndConfigureAtoms(mol);
		return mol;
	}

	public static IAtomContainer checkElements(String smiles) throws CDKException {
		IAtomContainer mol1 = smilesToMol(smiles);
		for (IAtom a : mol1.atoms()) {
			int n = a.getAtomicNumber();
			if (!((n == 1) || (n == 6) || (n == 7) || (n == 8) || (n == 9) || (n == 14) || (n == 15) || (n == 16)
					|| (n == 17) || (n == 35) || (n == 53))) {
				throw (new CDKException("Element type isn't supported. Only H,C,N,O,Si,S,P,F,Cl,Br,I! + Element: "
						+ a.getAtomicNumber() + a.getSymbol()));
			}
		}
		return mol1;
	}

	public static void checkConnectivityCycles(String smiles) throws CDKException {
		if (smiles.contains("0")) {
			throw (new CDKException("Not more than 9 cycles are allowed " + smiles));
		}
		if (smiles.contains(".")) {
			throw (new CDKException("Only one connectivity component is allowed " + smiles));
		}
	}

	public static float weight(IAtomContainer mol) throws CDKException {
		WeightDescriptor molarMass = new WeightDescriptor();
		return ((float) ((DoubleResult) molarMass.calculate(mol).getValue()).doubleValue());
	}

	public static void checkMW(String smiles) throws CDKException {
		IAtomContainer mol1 = smilesToMol(smiles);
		if (weight(mol1) > MAXMW) {
			throw (new CDKException("Too large MW " + weight(mol1) + "  " + smiles));
		}
	}

	public static void smilesCanCheck(String smilesCan) throws CDKException, IOException {
		if (!canonicalizeMol(smilesCan).equals(smilesCan)) {
			throw (new CDKException("Smiles changes after second canonicalization   " + smilesCan));
		}
		if (smilesCan.trim().length() > SMILES_MAX_LENGTH) {
			throw (new CDKException("Smiles too long   " + smilesCan.length() + " " + smilesCan));
		}
	}

	public static String molToInchiKey(IAtomContainer mol) throws CDKException {
		InChIGeneratorFactory factory = InChIGeneratorFactory.getInstance();
		InChIGenerator gen = factory.getInChIGenerator(mol);
		String inchi = gen.getInchiKey();
		return inchi;
	}

	public static String molToInchi(IAtomContainer mol) throws CDKException {
		InChIGeneratorFactory factory = InChIGeneratorFactory.getInstance();
		InChIGenerator gen = factory.getInChIGenerator(mol);
		String inchi = gen.getInchi();
		return inchi;
	}

	public static void checkMolIdenlity(IAtomContainer mol1, IAtomContainer mol2) throws CDKException {
		String inchikey14First = molToInchiKey(mol1).split("\\-")[0];
		String inchikey14Second = molToInchiKey(mol2).split("\\-")[0];
		if (!inchikey14First.equals(inchikey14Second)) {
			throw (new CDKException("inchikey changed"));
		}
	}

	public static float weight(String smiles) throws CDKException {
		WeightDescriptor molarMass = new WeightDescriptor();
		return ((float) ((DoubleResult) molarMass.calculate(smilesToMol(smiles)).getValue()).doubleValue());
	}

	/**
	 * Converts SMILES string into integers for 1D CNN
	 * 
	 * @param s SMILES string for a molecule
	 * @return int[SMILES_MAX_LENGTH] - integer array, where each symbol of SMILES
	 *         string is represented as integer according to index of this symbol in
	 *         TOKENS array.
	 * @throws CDKException This method throws an exception if SMILES string of
	 *                      molecule is longer than SMILES_LEN or if the string
	 *                      contains incompatible symbols (i.e. symbols which are
	 *                      not contained in TOKENS array).
	 */
	public static int[] tokenize(String s) throws CDKException {
		int[] result = new int[SMILES_MAX_LENGTH];
		String s1 = s.trim();
		if (s1.length() >= SMILES_MAX_LENGTH) {
			throw (new CDKException(" Too long SMILES string " + s1.length()));
		}
		for (int i = 0; i < s1.length(); i++) {
			boolean found = false;
			for (int j = 0; j < TOKENS.length; j++) {
				if (TOKENS[j] == s1.charAt(i)) {
					result[i] = j;
					found = true;
				}
			}
			if (!found) {
				throw (new CDKException(" Unknown symbol " + s1.charAt(i)));
			}
		}
		return result;
	}

	/**
	 * Backward transform comparing with tokenize().
	 * intsToSmiles(tokenize(s)).trim().equals(s) is true
	 * 
	 * @param q int[SMILES_LEN] array which represents SMILES string. Symbols
	 *          encoded as integers according to TOKENS array.
	 * @return SMILES string (with spaces at end). Use "trim()" always with this
	 *         method.
	 */
	public static String intsToSmiles(int[] q) {
		String s = "";
		for (int i = 0; i < q.length; i++) {
			s = s + TOKENS[q[i]];
		}
		return s;
	}

	/**
	 * Molecular fingerprints for a molecule.
	 * 
	 * @param smiles molecule (SMILES string)
	 * @param type   types of fingerprints
	 * @return float array. Typically zeros (0.0F) or ones (1.0F) or values which
	 *         are identical to integers.
	 * @throws CDKException CDK internal errors.
	 * @throws IOException
	 */
	public static float[] fingerprints(String smiles, FingerprintsType type) throws CDKException, IOException {
		if (type == FingerprintsType.NONE) {
			return new float[] {};
		}
		IAtomContainer mol = preprocessMol(smilesToMol(smiles.trim()));
		BitSet fp = null;
		if (type == FingerprintsType.ADDITIVE_CIRCULAR_6_4096_NO_SCALE) {
			return (circularAdditiveFingerPrints(mol, CircularFingerprinter.CLASS_ECFP4, 4096, false));
		}
		if (type == FingerprintsType.ADDITIVE_CIRCULAR_4_1024_NO_SCALE) {
			return (circularAdditiveFingerPrints(mol, CircularFingerprinter.CLASS_ECFP4, 1024, false));
		}
		if (type == FingerprintsType.ADDITIVE_CIRCULAR_6_1024_NO_SCALE) {
			return (circularAdditiveFingerPrints(mol, CircularFingerprinter.CLASS_ECFP6, 1024, false));
		}
		if (type == FingerprintsType.MACCS) {
			MACCSFingerprinter fpGen = new MACCSFingerprinter();
			fp = fpGen.getBitFingerprint(mol).asBitSet();
		}
		if (type == FingerprintsType.CIRCULAR_4_1024) {
			CircularFingerprinter fpGen = new CircularFingerprinter(CircularFingerprinter.CLASS_ECFP4, 1024);
			fpGen.calculate(mol);
			fp = fpGen.getBitFingerprint(mol).asBitSet();
		}
		if (type == FingerprintsType.CIRCULAR_4_512) {
			CircularFingerprinter fpGen = new CircularFingerprinter(CircularFingerprinter.CLASS_ECFP4, 512);
			fpGen.calculate(mol);
			fp = fpGen.getBitFingerprint(mol).asBitSet();
		}
		if (type == FingerprintsType.CIRCULAR_6_1024) {
			CircularFingerprinter fpGen = new CircularFingerprinter(CircularFingerprinter.CLASS_ECFP6, 1024);
			fpGen.calculate(mol);
			fp = fpGen.getBitFingerprint(mol).asBitSet();
		}
		if (type == FingerprintsType.CIRCULAR_6_4096) {
			CircularFingerprinter fpGen = new CircularFingerprinter(CircularFingerprinter.CLASS_ECFP6, 4096);
			fpGen.calculate(mol);
			fp = fpGen.getBitFingerprint(mol).asBitSet();
		}
		if (type == FingerprintsType.LINGO) {
			LingoFingerprinter fpGen = new LingoFingerprinter(6);
			fp = fpGen.getBitFingerprint(mol).asBitSet();
		}

		if (type == FingerprintsType.PUBCHEM) {
			PubchemFingerprinter fpGen = new PubchemFingerprinter(mol.getBuilder());
			fp = fpGen.getBitFingerprint(mol).asBitSet();
		}

		if (type == FingerprintsType.KLEKOTA) {
			KlekotaRothFingerprinter fpGen = new KlekotaRothFingerprinter();
			fp = fpGen.getBitFingerprint(mol).asBitSet();
		}

		if (type == FingerprintsType.KLEKOTA_ADDITIVE) {
			KlekotaRothFingerprinter fpGen = new KlekotaRothFingerprinter();
			ICountFingerprint klekota = fpGen.getCountFingerprint(mol);
			int n = (int) klekota.size();
			float[] result = new float[n];
			for (int i = 0; i < n; i++) {
				result[i] = klekota.getCount(i);
			}
			return result;
		}

		float[] result = new float[fp.size()];
		for (int i = 0; i < fp.size(); i++) {
			if (fp.get(i)) {
				result[i] = 1;
			} else {
				result[i] = 0;
			}
		}
		return result;
	}

	public static GraphMol molToMolGraph(String smiles, String comment, boolean nonBondedEdges)
			throws CDKException, IllegalArgumentException, IOException {

		int heavyAtoms = 0;

		IAtomContainer mol = preprocessMol(smilesToMol(smiles));

		HashMap<Integer, Integer> graphNumberByCDKNumber = new HashMap<Integer, Integer>();
		ArrayList<float[]> atomsFeatures = new ArrayList<float[]>();
		ArrayList<Integer> atomsElementAtomicNumber = new ArrayList<Integer>();
		ArrayList<Integer> atomsNHydrogensNeigh = new ArrayList<Integer>();
		ArrayList<float[]> bondsFeatures = new ArrayList<float[]>();
		ArrayList<Integer> firstNodesOfBonds = new ArrayList<Integer>();
		ArrayList<Integer> secondNodesOfBonds = new ArrayList<Integer>();

		for (IAtom a : mol.atoms()) {
			if (!a.getAtomicNumber().equals(1)) {
				heavyAtoms++;
				int n = a.getIndex();
				graphNumberByCDKNumber.put(n, atomsFeatures.size());
				float[] features = new float[30];
				int[] coordinatesAndType = atomType(a);
				features[coordinatesAndType[0]] = 1;
				features[coordinatesAndType[1] + 25] = 1;

				for (int i = 0; i < features.length; i++) {
					if (Float.isNaN(features[i]) || Float.isInfinite(features[i])) {
						features[i] = 0;
					}
				}
				atomsFeatures.add(features);
				atomsElementAtomicNumber.add(a.getAtomicNumber());
				atomsNHydrogensNeigh.add(nHydrogensNeigh(a));
			}
		}
		if (heavyAtoms != atomsFeatures.size()) {
			throw new RuntimeException("Unknown error");
		}

		for (IBond b : mol.bonds()) {
			boolean nonH = true;
			for (IAtom a : b.atoms()) {
				if (a.getAtomicNumber().equals(1)) {
					nonH = false;
				}
			}
			if (nonH) {
				bondsFeatures.add(bondFeatures(b, nonBondedEdges));
				int a1 = graphNumberByCDKNumber.get(b.getBegin().getIndex());
				int a2 = graphNumberByCDKNumber.get(b.getEnd().getIndex());
				int a11 = a1 < a2 ? a1 : a2;
				int a21 = a1 < a2 ? a2 : a1;
				if (a1 == a2) {
					throw new RuntimeException("Unknown error");
				}
				firstNodesOfBonds.add(a11);
				secondNodesOfBonds.add(a21);
			}
		}

		if (nonBondedEdges) {
			for (IAtom at1 : mol.atoms()) {
				if (!at1.getAtomicNumber().equals(1)) {
					for (IAtom at2 : mol.atoms()) {
						if (!at2.getAtomicNumber().equals(1)) {
							int a1 = at1.getIndex();
							int a2 = at2.getIndex();
							if (a2 > a1) {
								if (!at1.equals(at2)) {
									if (!atomsLinked(at1, at2)) {
										a1 = graphNumberByCDKNumber.get(a1);
										a2 = graphNumberByCDKNumber.get(a2);
										int a11 = a1 < a2 ? a1 : a2;
										int a21 = a1 < a2 ? a2 : a1;
										if (a1 == a2) {
											throw new RuntimeException("Unknown error");
										}
										float[] apf = atompairFeatures(at1, at2);
										if ((apf[7] != 0) || (apf[8] != 0)) {
											bondsFeatures.add(apf);
											firstNodesOfBonds.add(a11);
											secondNodesOfBonds.add(a21);
										}
									}
								}
							}
						}
					}
				}
			}
		}

		int nAtomFeatures = atomsFeatures.get(0).length;
		int nBondFeatures = bondsFeatures.get(0).length;
		GraphMol result = new GraphMol(heavyAtoms, nAtomFeatures, bondsFeatures.size(), nBondFeatures, comment);
		for (int i = 0; i < atomsFeatures.size(); i++) {
			result.setFeaturesOfNode(i, atomsElementAtomicNumber.get(i), atomsNHydrogensNeigh.get(i),
					atomsFeatures.get(i));
		}
		for (int i = 0; i < bondsFeatures.size(); i++) {
			result.addEdge(firstNodesOfBonds.get(i), secondNodesOfBonds.get(i), bondsFeatures.get(i));
		}

		return result;
	}

	public static String checkMolAndCanonicalize(String smiles) throws CDKException, IOException {
		checkElements(smiles);
		checkConnectivityCycles(smiles);
		checkMW(smiles);
		String smilesCan = canonicalizeMol(canonicalizeMol(smiles));
		smilesCanCheck(smilesCan);
		checkConnectivityCycles(smilesCan);
		checkMW(smilesCan);
		checkMolIdenlity(smilesToMol(smiles), smilesToMol(smilesCan));
		checkMolIdenlity(smilesToMol(canonicalizeMol(smilesCan)), smilesToMol(smilesCan));
		checkMolIdenlity(preprocessMol(smilesToMol(smilesCan)), smilesToMol(smilesCan));

		tokenize(smilesCan);
		molToMolGraph(smilesCan, "", true);
		fingerprints(smilesCan, FingerprintsType.ADDITIVE_CIRCULAR_6_1024_NO_SCALE);
		return smilesCan;
	}

	// private methods

	private static float[] circularAdditiveFingerPrints(IAtomContainer mol, int type, int len, boolean scale)
			throws CDKException {
		CircularFingerprinter cf = new CircularFingerprinter(type, len);
		cf.calculate(mol);
		float[] result = new float[len];
		for (int n = 0; n < len; n++) {
			result[n] = 0;
		}
		for (int n = 0; n < cf.getFPCount(); n++) {
			int i = cf.getFP(n).hashCode;
			long b = i >= 0 ? i : ((i & 0x7FFFFFFF) | (1L << 31));
			result[(int) (b % len)] += 1.0;
		}
		if (scale) {
			float max = -1;
			for (int n = 0; n < len; n++) {
				if (result[n] > max) {
					max = result[n];
				}
			}
			if (max != 0) {
				for (int n = 0; n < len; n++) {
					result[n] = result[n] / max;
				}
			} else
				System.out.println("Warning! All fingerprints are zero... " + molToSmiles(mol));
		}
		return result;
	}

	private static String atomTypeToString(IAtom at) {
		HashSet<String> mostCommonAtomTypes = new HashSet<String>();
		mostCommonAtomTypes.addAll(Arrays.asList(new String[] { "C.sp3", "C.sp2", "O.sp3", "O.sp2", "N.sp2", "N.amide",
				"N.sp3", "C.sp", "S.3", "O.planar3", "N.nitro", "N.planar3", "S.planar3" }));
		String atomType = at.getAtomTypeName();
		if (mostCommonAtomTypes.contains(atomType.trim())) {
			return atomType.trim();
		} else {
			return atomType.split("\\.")[0];
		}
	}

	private static int[] atomType(IAtom at) throws CDKException {
		int[] result = new int[2];
		String[] atomTypes = new String[] { "B", "C", "F", "Cl", "I", "S.planar3", "C.sp2", "C.sp3", "N", "O", "P",
				"N.nitro", "S", "X", "S.3", "O.planar3", "N.planar3", "Br", "N.amide", "N.sp2", "Si", "N.sp3", "C.sp",
				"O.sp3", "O.sp2" };
		String atomType = atomTypeToString(at);
		result[0] = -1;
		for (int i = 0; i < atomTypes.length; i++) {
			if (atomTypes[i].equals(atomType)) {
				result[0] = i;
			}
		}
		if (result[0] == -1) {
			throw new CDKException("Unexpected atom type " + at.getAtomTypeName());
		}
		result[1] = nHydrogensNeigh(at);
		return result;
	}

	private static int nHydrogensNeigh(IAtom a) throws CDKException {
		int count = 0;
		for (IBond b : a.bonds()) {
			if (b.getOther(a).getAtomicNumber().equals(1)) {
				count = count + 1;
			}
		}
		count = count + a.getImplicitHydrogenCount();
		return count;
	}

	private static float[] bondFeatures(IBond b, boolean fullAtomPairsFeatures) {
		float[] result = new float[fullAtomPairsFeatures ? 9 : 6];
		result[0] = b.isAromatic() ? 1.0f : 0.0f;
		result[1] = b.isInRing() ? 1.0f : 0.0f;
		result[2] = b.getOrder().numeric();
		result[3] = result[2] == 1 ? 1.0f : 0.0f;
		result[4] = result[2] == 2 ? 1.0f : 0.0f;
		result[5] = result[2] == 3 ? 1.0f : 0.0f;
		return result;
	}

	private static float[] atompairFeatures(IAtom a1, IAtom a2) {
		float[] result = new float[9];
		result[6] = 1;
		result[7] = atoms13(a1, a2) ? 1.0f : 0.0f;
		result[8] = atoms14(a1, a2) ? 1.0f : 0.0f;
		return result;
	}

	private static boolean atoms13(IAtom a1, IAtom a2) {
		for (IBond b1 : a1.bonds()) {
			for (IBond b2 : a2.bonds()) {
				if (!a1.equals(a2)) {
					if (b1.getOther(a1).equals(b2.getOther(a2))) {
						return true;
					}
				}
			}
		}
		return false;
	}

	private static boolean atoms14(IAtom a1, IAtom a2) {
		ArrayList<IAtom[]> result = new ArrayList<IAtom[]>();
		if (!a1.equals(a2)) {
			for (IBond b1 : a1.bonds()) {
				for (IBond b2 : a2.bonds()) {
					IAtom a3 = b1.getOther(a1);
					IAtom a4 = b2.getOther(a2);
					if ((!a3.equals(a4)) && (!a3.equals(a2)) && (!a4.equals(a1))) {
						if (atomsLinked(a3, a4)) {
							result.add(new IAtom[] { a1, a3, a4, a2 });
						}
					}
				}
			}
		}
		return result.size() != 0;
	}

	private static boolean atomsLinked(IAtom a1, IAtom a2) {
		for (IBond b : a1.bonds()) {
			if (b.getOther(a1).equals(a2)) {
				return true;
			}
		}
		return false;
	}

}
