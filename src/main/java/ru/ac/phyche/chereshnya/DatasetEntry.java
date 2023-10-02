package ru.ac.phyche.chereshnya;

import org.openscience.cdk.exception.CDKException;

/**
 * An entry of a ChemDataset. It stores information about property value and
 * SMILES representation of structure of the molecule.
 *
 */
public class DatasetEntry {
	private String smiles;
	private float retention;

	/**
	 * 
	 * @return SMILES string
	 */
	public String getSmiles() {
		return smiles;
	}

	/**
	 * It is slow. It converts SMILES to InChI internally.
	 * 
	 * @return InChI string (structure of molecule)
	 * @throws CDKException internal CDK error
	 */
	public String getInchi() throws CDKException {
		return ChemUtils.smilesToInchi(smiles);
	}

	/**
	 * It is slow. It converts SMILES to InChI-key.
	 * 
	 * @return InChI-key
	 * @throws CDKException internal CDK error
	 */
	public String getInchikey() throws CDKException {
		return ChemUtils.smilesToInchiKey(smiles);
	}

	/**
	 * 
	 * @param smiles SMILES string
	 */
	public void setSmiles(String smiles) {
		this.smiles = smiles.trim();
	}

	/**
	 * 
	 * @return value of a given property
	 */
	public float getRetention() {
		return retention;
	}

	/**
	 * 
	 * @param value of the molecule property
	 */
	public void setRetention(float retention) {
		this.retention = retention;
	}

	/**
	 * Create new instance with converting SMILES string to canonical form.
	 * 
	 * @param smiles_         SMILES string of the molecule
	 * @param property_       value of property (such as boiling point)
	 * @param stereochemistry if true - symbols to denote cis/trans and optical
	 *                        isomers will be used
	 * @return newly created entry
	 * @throws CDKException internal CDK error
	 */
	public static DatasetEntry instanceCanonical(String smiles_, float retention_, boolean stereochemistry)
			throws CDKException {
		DatasetEntry result = new DatasetEntry();
		result.smiles = ChemUtils.canonical(smiles_, stereochemistry).trim();
		result.retention = retention_;
		return result;
	}

	/**
	 * Create new instance with converting SMILES string to canonical form from
	 * space separated or comma-separated string.
	 * 
	 * @param s               space separated or comma-separated string that
	 *                        contains SMILES and numerical value, e.g., "CCC 3.3"
	 * @param stereochemistry if true - symbols to denote cis/trans and optical
	 *                        isomers will be used
	 * @return newly created entry
	 * @throws CDKException internal CDK error
	 */
	public static DatasetEntry fromString(String s, boolean stereochemistry) throws CDKException {
		String smiles_ = "Q";
		String val_ = "";

		String[] split = null;
		if (s.contains(",")) {
			split = s.split("\\,");
		} else {
			split = s.trim().split("\\s+");
		}
		smiles_ = split[0].trim();
		val_ = split[1].trim();
		DatasetEntry result = new DatasetEntry();
		result.smiles = ChemUtils.canonical(smiles_, stereochemistry).trim();
		result.retention = Float.parseFloat(val_);
		return result;
	}

	@Override
	public String toString() {
		String result = (smiles + " " + retention);
		return result;
	}

	/**
	 * 
	 * @return comma-separated string, e.g., "CCC,3.3"
	 */
	public String toCommaSeparatedString() {
		String result = (smiles + "," + retention);
		return result;
	}

	/**
	 * Create new instance without (!) converting SMILES string to canonical form.
	 * 
	 * @param smiles_   SMILES string of the molecule
	 * @param property_ value of property (such as boiling point)
	 * @return newly created entry
	 */
	public static DatasetEntry instance(String smiles_, float retention_) {
		DatasetEntry result = new DatasetEntry();
		result.smiles = new String(smiles_.trim());
		result.retention = retention_;
		return result;
	}

	@Override
	public DatasetEntry clone() {
		return DatasetEntry.instance(smiles, retention);
	}

	/**
	 * 
	 * @return this.clone();
	 */
	public DatasetEntry deepclone() {
		return this.clone();
	}

	/**
	 * 
	 * @param e other RetentionsEntry
	 * @return true if this equals e
	 */
	public boolean equals(DatasetEntry e) {
		return ((e.smiles.trim().equals(this.smiles.trim())) && (this.retention == e.retention));
	}

	@Override
	public boolean equals(Object o) {
		if (o.getClass().equals(this.getClass())) {
			DatasetEntry e = (DatasetEntry) o;
			return ((e.smiles.trim().equals(this.smiles.trim())) && (this.retention == e.retention));
		} else {
			return false;
		}
	}
}
