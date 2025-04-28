package ru.ac.phyche.chereshnya.gui;

import org.openscience.cdk.exception.CDKException;

import ru.ac.phyche.chereshnya.ChemUtils;

public class JSJavaCall {
	public String smiles = "X";
	public String oldSmiles = "X";

	public void jsjavacall(String smiles) {
		//System.out.println(smiles);
		String canonicalSmiles;
		try {
			canonicalSmiles = ChemUtils.canonical(smiles, false);
			if (ChemUtils.weight(canonicalSmiles) < 60) {
				throw new CDKException("");
			}
			if (canonicalSmiles.length() < 5) {
				throw new CDKException("");
			}
			this.smiles = smiles;
		} catch (CDKException e) {
			e.printStackTrace();
		}

	}
}
