/*
 *                    BioJava development code
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  If you do not have a copy,
 * see:
 *
 *      http://www.gnu.org/copyleft/lesser.html
 *
 * Copyright for this code is held jointly by the individual
 * authors.  These should be listed in @author doc comments.
 *
 * For more information on the BioJava project and its aims,
 * or to join the biojava-l mailing list, visit the home page
 * at:
 *
 *      http://www.biojava.org/
 *
 * created at Mar 4, 2008
 */
package org.biojava.nbio.structure.io.mmcif.chem;

import org.biojava.nbio.structure.io.mmcif.ChemicalComponentDictionary;
import org.biojava.nbio.structure.io.mmcif.model.ChemComp;

import java.util.*;

/** Some tools for working with chemical compounds.
 *
 * @author Andreas Prlic
 * @since 1.7
 *
 */
public class ChemCompTools {

	private static final Character UNKNOWN_ONE_LETTER_CODE = 'X';
	private static final Character UNKNOWN_NUCLEOTIDE = 'N';

	/**
	 * Lookup table to convert standard amino acid's monomer ids to one-letter-codes
	 */
	private static final Map<String, Character> AMINO_ACID_LOOKUP_3TO1;

	/**
	 * Lookup table to convert standard amino acid's one-letter-codes to monomer ids
	 * TODO use an array since there are only alphabetic keys
	 */
	private static final Map<Character, String> AMINO_ACID_LOOKUP_1TO3;

	/**
	 * Lookup table to convert standard nucleic acid's monomer ids to one-letter-codes
	 */
	private static final Map<String, Character> DNA_LOOKUP_2TO1;




	/**
	 * Static block that initializes lookup maps and initializes their <tt>ResidueInfo</tt> instances
	 */
	static
	{
		Map<String, Character> foo = new HashMap<>();
		foo.put("ALA", 'A');
		foo.put("ASP", 'D');
		foo.put("ASN", 'N');
		foo.put("ASX", 'B');
		foo.put("ARG", 'R');
		foo.put("CYS", 'C');
		foo.put("GLU", 'E');
		foo.put("GLN", 'Q');
		foo.put("GLY", 'G');
		foo.put("GLX", 'Z');
		foo.put("HIS", 'H');
		foo.put("ILE", 'I');
		foo.put("LYS", 'K');
		foo.put("LEU", 'L');
		foo.put("MET", 'M');
		foo.put("PHE", 'F');
		foo.put("PRO", 'P');
		foo.put("SER", 'S');
		foo.put("THR", 'T');
		foo.put("TRP", 'W');
		foo.put("TYR", 'Y');
		foo.put("VAL", 'V');
		AMINO_ACID_LOOKUP_3TO1 = Collections.unmodifiableMap(foo);

		Map<Character, String> bar = new HashMap<>();
		bar.put('A', "ALA");
		bar.put('D', "ASP");
		bar.put('N', "ASN");
		bar.put('B', "ASX");
		bar.put('R', "ARG");
		bar.put('C', "CYS");
		bar.put('E', "GLU");
		bar.put('Q', "GLN");
		bar.put('G', "GLY");
		bar.put('Z', "GLX");
		bar.put('H', "HIS");
		bar.put('I', "ILE");
		bar.put('K', "LYS");
		bar.put('L', "LEU");
		bar.put('M', "MET");
		bar.put('F', "PHE");
		bar.put('P', "PRO");
		bar.put('S', "SER");
		bar.put('T', "THR");
		bar.put('W', "TRP");
		bar.put('Y', "TYR");
		bar.put('V', "VAL");
		AMINO_ACID_LOOKUP_1TO3 = Collections.unmodifiableMap(bar);

		foo = new HashMap<>();
		foo.put("DA",'A');
		foo.put("DC",'C');
		foo.put("DG",'G');
		foo.put("DI",'I');
		foo.put("DU",'U');
		foo.put("DT",'T');
		DNA_LOOKUP_2TO1 = Collections.unmodifiableMap(foo);



		// initialise standard chemical components
		List<String> stdMonIds = new ArrayList<>();
		stdMonIds.addAll(AMINO_ACID_LOOKUP_3TO1.keySet());
		stdMonIds.addAll(DNA_LOOKUP_2TO1.keySet());



	}

	public static char getAminoOneLetter(String chemCompId){
		return  AMINO_ACID_LOOKUP_3TO1.getOrDefault(chemCompId, (char)0);
	}


	public static char getDNAOneLetter(String chemCompId){
		return DNA_LOOKUP_2TO1.getOrDefault(chemCompId, (char)0) ;
	}

	public static String getAminoThreeLetter(Character c){
		return AMINO_ACID_LOOKUP_1TO3.get(c);
	}

	/**
	 * Lookup table to convert standard nucleic acid's one-letter-codes to monomer ids
	 * TODO use an array since there are only alphabetic keys
	 */
	public static String getDNATwoLetter(char c){
		switch (c) {
			case 'A': return "DA";
			case 'C': return "DC";
			case 'G': return "DG";
			case 'I': return "DI";
			case 'U': return "DU";
			case 'T': return "DT";
			default: return null;
		}
	}

	public static boolean isStandardChemComp(ChemComp cc){

		String pid = cc.getMon_nstd_parent_comp_id();
		String one = cc.getOne_letter_code();

		PolymerType polymerType = cc.getPolymerType();

		// standard residues have no parent
		if ((pid == null) || (pid.equals("?"))){

			// and they have a one letter code
			if ( ( one != null) && ( !one.equals("?") )){

				// peptides and dpeptides must not have X
				if ( (polymerType == PolymerType.peptide) ||
						( polymerType == PolymerType.dpeptide)) {
					return performPeptideCheck(cc, one);

				}
				if (polymerType == PolymerType.rna){
					return performRNACheck(cc);
				}
				if (polymerType == PolymerType.dna) {
					return performDNACheck(cc);
				}

				//System.err.println("Non standard chem comp: " + cc);
				return false;
			}
		}
		return false;
	}


	private static boolean performRNACheck(ChemComp cc) {
		return cc.getId().length() == 1;
	}


	private static boolean performDNACheck(ChemComp cc) {
		if ( cc.getId().equals(UNKNOWN_NUCLEOTIDE.toString()))
			return false;

		char c = getDNAOneLetter(cc.getId());
		// we did not find it in the list of standard nucleotides?
		return c != 0;
	}


	private static boolean performPeptideCheck(ChemComp cc, String one) {
		if (one.equals(UNKNOWN_ONE_LETTER_CODE.toString()))
			return false;

		char c =  getAminoOneLetter(cc.getId());
		// we did not find it in the list of standard aminos?
		return c != 0;
	}


	// TODO: component 175 has 3 chars as a one letter code...
	// Figure out what to do with it...
	// so does: 4F3,5ZA and others
	public static Character getOneLetterCode(ChemComp cc, ChemicalComponentDictionary dictionary){
		if ( cc.getResidueType() == ResidueType.nonPolymer )
			return null;

		if ( cc.isStandard())
			return cc.getOne_letter_code().charAt(0);

		ChemComp parent = dictionary.getParent(cc);
		if ( parent == null){
			//System.err.println("parent is null " + cc);
			return cc.getOne_letter_code().charAt(0);
		}
		PolymerType poly = cc.getPolymerType();
		if (( poly == PolymerType.peptide) || ( poly == PolymerType.dpeptide)){
			char c = getAminoOneLetter(parent.getId());
			return c == 0 ? UNKNOWN_ONE_LETTER_CODE : Character.valueOf(c);
		}
		if ( poly == PolymerType.dna){
			char c = getDNAOneLetter(parent.getId());
			return c == 0 ? UNKNOWN_NUCLEOTIDE : Character.valueOf(c);

		}
		return cc.getMon_nstd_parent_comp_id().charAt(0);
	}
}
