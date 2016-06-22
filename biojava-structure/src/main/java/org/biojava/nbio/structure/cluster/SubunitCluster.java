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
 */
package org.biojava.nbio.structure.cluster;

import org.apache.commons.lang.StringUtils;
import org.biojava.nbio.alignment.Alignments;
import org.biojava.nbio.alignment.Alignments.PairwiseSequenceAlignerType;
import org.biojava.nbio.alignment.SimpleGapPenalty;
import org.biojava.nbio.alignment.template.PairwiseSequenceAligner;
import org.biojava.nbio.core.alignment.matrices.SubstitutionMatrixHelper;
import org.biojava.nbio.core.exceptions.CompoundNotFoundException;
import org.biojava.nbio.core.sequence.ProteinSequence;
import org.biojava.nbio.core.sequence.compound.AminoAcidCompound;
import org.biojava.nbio.structure.StructureException;
import org.biojava.nbio.structure.align.StructureAlignment;
import org.biojava.nbio.structure.align.StructureAlignmentFactory;
import org.biojava.nbio.structure.align.fatcat.FatCatRigid;
import org.biojava.nbio.structure.align.model.AFPChain;
import org.biojava.nbio.structure.align.multiple.MultipleAlignment;
import org.biojava.nbio.structure.align.multiple.MultipleAlignmentEnsembleImpl;
import org.biojava.nbio.structure.symmetry.core.Subunits;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * A SubunitCluster contains a set of equivalent {@link Subunits}, the set of
 * equivalent residues (EQR) between {@link Subunit} and a {@link Subunit}
 * representative. It also stores the method used for clustering.
 * <p>
 * This class allows the comparison and merging of SubunitClusters.
 * 
 * @author Aleix Lafita
 * 
 */
public class SubunitCluster {

	private static final Logger logger = LoggerFactory
			.getLogger(SubunitCluster.class);

	private List<Subunit> subunits = new ArrayList<Subunit>();
	private List<List<Integer>> subunitEQR = new ArrayList<List<Integer>>();
	private int representative = -1;
	private SubunitClustererMethod method = SubunitClustererMethod.IDENTITY;

	/**
	 * A SubunitCluster is always initialized with a single Subunit.
	 * 
	 * @param subunit
	 *            initial Subunit
	 */
	public SubunitCluster(Subunit subunit) {

		subunits.add(subunit);

		List<Integer> identity = new ArrayList<Integer>();
		for (int i = 0; i < subunit.size(); i++)
			identity.add(i);
		subunitEQR.add(identity);

		representative = 0;
	}

	/**
	 * Tells whether the other SubunitCluster contains exactly the same Subunit.
	 * This is checked by String equality of their residue one-letter sequences.
	 * 
	 * @param other
	 *            SubunitCluster
	 * @return true if the SubunitClusters are identical, false otherwise
	 */
	public boolean isIdenticalTo(SubunitCluster other) {
		String thisSequence = this.subunits.get(this.representative)
				.getProteinSequenceString();
		String otherSequence = other.subunits.get(other.representative)
				.getProteinSequenceString();
		return thisSequence.equals(otherSequence);
	}

	/**
	 * Merges the other SubunitCluster into this one if it contains exactly the
	 * same Subunit. This is checked by {@link #isIdenticalTo(SubunitCluster)}.
	 * 
	 * @param other
	 *            SubunitCluster
	 * @return true if the SubunitClusters were merged, false otherwise
	 */
	public boolean mergeIdentical(SubunitCluster other) {

		if (!isIdenticalTo(other))
			return false;

		logger.info("SubunitClusters are identical");

		this.subunits.addAll(other.subunits);
		this.subunitEQR.addAll(other.subunitEQR);

		return true;
	}

	/**
	 * Merges the other SubunitCluster into this one if their representatives
	 * sequences are similar (higher sequence identity and coverage than the
	 * thresholds).
	 * <p>
	 * The sequence alignment is performed using Smith Waterman, default linear
	 * {@link SimpleGapPenalty} and BLOSUM62 as scoring matrix.
	 * 
	 * @param other
	 *            SubunitCluster
	 * @param minSeqid
	 *            sequence identity threshold. Value in [0,1]. Values lower than
	 *            0.7 are not recommended. Use {@link #mergeStructure} for lower
	 *            values.
	 * @param minCoverage
	 *            coverage (alignment fraction) threshold. Value in [0,1].
	 * @return true if the SubunitClusters were merged, false otherwise
	 * @throws CompoundNotFoundException
	 */
	public boolean mergeSequence(SubunitCluster other, double minSeqid,
			double minCoverage) throws CompoundNotFoundException {

		// Extract the protein sequences as BioJava alignment objects
		ProteinSequence thisSequence = this.subunits.get(this.representative)
				.getProteinSequence();
		ProteinSequence otherSequence = other.subunits
				.get(other.representative).getProteinSequence();

		// Perform a Smith-Waterman alignment with BLOSUM62
		PairwiseSequenceAligner<ProteinSequence, AminoAcidCompound> aligner = Alignments
				.getPairwiseAligner(thisSequence, otherSequence,
						PairwiseSequenceAlignerType.LOCAL,
						new SimpleGapPenalty(),
						SubstitutionMatrixHelper.getBlosum62());

		// TODO provisional coverage function
		double gaps1 = StringUtils.countMatches(aligner.getPair()
				.getAlignedSequence(1).getSequenceAsString(), "-");
		double gaps2 = StringUtils.countMatches(aligner.getPair()
				.getAlignedSequence(2).getSequenceAsString(), "-");
		double lengthAlignment = aligner.getPair().getLength();
		double lengthThis = aligner.getQuery().getLength();
		double lengthOther = aligner.getTarget().getLength();
		double coverage = Math.min((lengthAlignment - gaps1 - gaps2)
				/ lengthThis, (lengthAlignment - gaps1 - gaps2) / lengthOther);

		if (coverage < minCoverage)
			return false;

		double seqid = aligner.getPair().getNumIdenticals();
		seqid /= aligner.getPair().getLength();

		if (seqid < minSeqid)
			return false;

		logger.info("SubunitClusters are similar in sequence with {} sequence "
				+ "identity and {} coverage", seqid, coverage);

		// If coverage and sequence identity sufficient, merge other and this
		List<Integer> thisAligned = new ArrayList<Integer>();
		List<Integer> otherAligned = new ArrayList<Integer>();

		// Extract the aligned residues of both Subunit
		for (int p = 1; p < aligner.getPair().getLength() + 1; p++) {

			// Skip gaps in any of the two sequences
			if (aligner.getPair().getAlignedSequence(1).isGap(p))
				continue;
			if (aligner.getPair().getAlignedSequence(2).isGap(p))
				continue;

			int thisIndex = aligner.getPair().getIndexInQueryAt(p) - 1;
			int otherIndex = aligner.getPair().getIndexInTargetAt(p) - 1;

			// Only consider residues that are part of the SubunitCluster
			if (this.subunitEQR.get(this.representative).contains(thisIndex)
					&& other.subunitEQR.get(other.representative).contains(
							otherIndex)) {
				thisAligned.add(thisIndex);
				otherAligned.add(otherIndex);
			}
		}

		// Do a List intersection to find out which EQR columns to remove
		List<Integer> thisRemove = new ArrayList<Integer>();
		List<Integer> otherRemove = new ArrayList<Integer>();

		for (int t = 0; t < this.subunitEQR.get(this.representative).size(); t++) {
			// If the index is aligned do nothing, otherwise mark as removing
			if (!thisAligned.contains(this.subunitEQR.get(this.representative)
					.get(t)))
				thisRemove.add(t);
		}

		for (int t = 0; t < other.subunitEQR.get(other.representative).size(); t++) {
			// If the index is aligned do nothing, otherwise mark as removing
			if (!otherAligned.contains(other.subunitEQR.get(
					other.representative).get(t)))
				otherRemove.add(t);
		}

		// Now remove unaligned columns, from end to start
		Collections.sort(thisRemove);
		Collections.reverse(thisRemove);
		Collections.sort(otherRemove);
		Collections.reverse(otherRemove);

		for (int t = 0; t < thisRemove.size(); t++) {
			for (List<Integer> eqr : this.subunitEQR) {
				int column = thisRemove.get(t);
				eqr.remove(column);
			}
		}

		for (int t = 0; t < otherRemove.size(); t++) {
			for (List<Integer> eqr : other.subunitEQR) {
				int column = otherRemove.get(t);
				eqr.remove(column);
			}
		}

		// The representative is the longest sequence
		if (this.subunits.get(this.representative).size() < other.subunits.get(
				other.representative).size())
			this.representative = other.representative + subunits.size();

		this.subunits.addAll(other.subunits);
		this.subunitEQR.addAll(other.subunitEQR);

		this.method = SubunitClustererMethod.SEQUENCE;

		return true;
	}

	/**
	 * Merges the other SubunitCluster into this one if their representative
	 * Atoms are structurally similar (lower RMSD and higher coverage than the
	 * thresholds).
	 * <p>
	 * The structure alignment is performed using FatCatRigid, with default
	 * parameters.
	 * 
	 * @param other
	 *            SubunitCluster
	 * @param maxRmsd
	 *            RMSD threshold.
	 * @param minCoverage
	 *            coverage (alignment fraction) threshold. Value in [0,1].
	 * @return true if the SubunitClusters were merged, false otherwise
	 * @throws StructureException
	 */
	public boolean mergeStructure(SubunitCluster other, double maxRmsd,
			double minCoverage) throws StructureException {

		// Perform a FatCat alignment with default parameters
		StructureAlignment algorithm = StructureAlignmentFactory
				.getAlgorithm(FatCatRigid.algorithmName);

		AFPChain afp = algorithm.align(this.subunits.get(this.representative)
				.getRepresentativeAtoms(),
				other.subunits.get(other.representative)
						.getRepresentativeAtoms());

		// Convert AFPChain to MultipleAlignment for convinience
		MultipleAlignment msa = new MultipleAlignmentEnsembleImpl(
				afp,
				this.subunits.get(this.representative).getRepresentativeAtoms(),
				other.subunits.get(other.representative)
						.getRepresentativeAtoms(), false)
				.getMultipleAlignment(0);

		double coverage = Math.min(msa.getCoverages().get(0), msa
				.getCoverages().get(1));
		if (coverage < minCoverage)
			return false;

		double rmsd = afp.getTotalRmsdOpt();
		if (rmsd > maxRmsd)
			return false;

		logger.info("SubunitClusters are structurally similar with {} RMSD "
				+ "and {} coverage", rmsd, coverage);

		// If RMSD is low and coverage sufficient merge clusters
		List<List<Integer>> alignedRes = msa.getBlock(0).getAlignRes();
		List<Integer> thisAligned = new ArrayList<Integer>();
		List<Integer> otherAligned = new ArrayList<Integer>();

		// Extract the aligned residues of both Subunit
		for (int p = 0; p < msa.length(); p++) {

			// Skip gaps in any of the two sequences
			if (alignedRes.get(0).get(p) == null)
				continue;
			if (alignedRes.get(1).get(p) == null)
				continue;

			int thisIndex = alignedRes.get(0).get(p);
			int otherIndex = alignedRes.get(1).get(p);

			// Only consider residues that are part of the SubunitCluster
			if (this.subunitEQR.get(this.representative).contains(thisIndex)
					&& other.subunitEQR.get(other.representative).contains(
							otherIndex)) {
				thisAligned.add(thisIndex);
				otherAligned.add(otherIndex);
			}
		}

		// Do a List intersection to find out which EQR columns to remove
		List<Integer> thisRemove = new ArrayList<Integer>();
		List<Integer> otherRemove = new ArrayList<Integer>();

		for (int t = 0; t < this.subunitEQR.get(this.representative).size(); t++) {
			// If the index is aligned do nothing, otherwise mark as removing
			if (!thisAligned.contains(this.subunitEQR.get(this.representative)
					.get(t)))
				thisRemove.add(t);
		}

		for (int t = 0; t < other.subunitEQR.get(other.representative).size(); t++) {
			// If the index is aligned do nothing, otherwise mark as removing
			if (!otherAligned.contains(other.subunitEQR.get(
					other.representative).get(t)))
				otherRemove.add(t);
		}

		// Now remove unaligned columns, from end to start
		Collections.sort(thisRemove);
		Collections.reverse(thisRemove);
		Collections.sort(otherRemove);
		Collections.reverse(otherRemove);

		for (int t = 0; t < thisRemove.size(); t++) {
			for (List<Integer> eqr : this.subunitEQR) {
				int column = thisRemove.get(t);
				eqr.remove(column);
			}
		}

		for (int t = 0; t < otherRemove.size(); t++) {
			for (List<Integer> eqr : other.subunitEQR) {
				int column = otherRemove.get(t);
				eqr.remove(column);
			}
		}

		// The representative is the longest sequence
		if (this.subunits.get(this.representative).size() < other.subunits.get(
				other.representative).size())
			this.representative = other.representative + subunits.size();

		this.subunits.addAll(other.subunits);
		this.subunitEQR.addAll(other.subunitEQR);

		this.method = SubunitClustererMethod.STRUCTURE;

		return true;
	}

	/**
	 * Analyze the internal symmetry of the SubunitCluster and divide its
	 * {@link Subunit} into the internal repeats (domains) if they are
	 * internally symmetric.
	 * 
	 * @return true if the cluster was internally symmetric, false otherwise
	 */
	public boolean divideInternally() {
		return false;
	}

	/**
	 * @return the number of Subunits in the cluster
	 */
	public int size() {
		return subunits.size();
	}

	/**
	 * @return the number of aligned residues between Subunits of the cluster
	 */
	public int length() {
		return subunitEQR.get(representative).size();
	}

	public SubunitClustererMethod getClustererMethod() {
		return method;
	}

	@Override
	public String toString() {
		return "SubunitCluster [Size=" + size() + ", Length=" + length()
				+ ", Representative=" + representative + ", Method=" + method
				+ "]";
	}

}
