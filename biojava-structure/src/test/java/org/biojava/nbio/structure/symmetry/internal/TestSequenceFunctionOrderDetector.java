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
package org.biojava.nbio.structure.symmetry.internal;

import org.biojava.nbio.structure.Atom;
import org.biojava.nbio.structure.StructureException;
import org.biojava.nbio.structure.align.model.AFPChain;
import org.biojava.nbio.structure.align.util.AtomCache;
import org.biojava.nbio.structure.symmetry.internal.CESymmParameters.RefineMethod;
import org.junit.Test;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;

/**
 * Originally part of {@link CeSymmTest}.
 * @author Spencer Bliven
 */
public class TestSequenceFunctionOrderDetector {

	@Test
	public void testGetSymmetryOrder() throws IOException, StructureException, RefinerFailedException {
		// List of alignments to try, along with proper symmetry
		Map<String,Integer> orderMap = new HashMap<>();
		orderMap.put("1itb.A",3); // b-trefoil, C3
		orderMap.put("1tim.A",2); // tim-barrel, C8
		//orderMap.put("d1p9ha_",-1); // not rotational symmetry
		orderMap.put("3HKE.A",2); // very questionable alignment
		orderMap.put("d1jlya1",3); // a very nice trefoil

		AtomCache cache = new AtomCache();

		for(Map.Entry<String, Integer> entry : orderMap.entrySet()) {
            String name = entry.getKey();
            CESymmParameters params = new CESymmParameters();
			params.setRefineMethod(RefineMethod.NOT_REFINED);
			Atom[] ca1 = cache.getAtoms(name);

			CeSymmResult result = CeSymm.analyzeLevel(ca1, params);
			AFPChain afpChain = result.getSelfAlignment();

			int order = new SequenceFunctionOrderDetector().calculateOrder(afpChain, ca1);

			assertEquals("Wrong order for "+name, entry.getValue().intValue(), order);
		}
	}

}
