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
package demo;

import org.biojava.nbio.structure.Atom;
import org.biojava.nbio.structure.StructureException;
import org.biojava.nbio.structure.align.StructureAlignment;
import org.biojava.nbio.structure.align.StructureAlignmentFactory;
import org.biojava.nbio.structure.align.ce.CeMain;
import org.biojava.nbio.structure.align.gui.jmol.StructureAlignmentJmol;
import org.biojava.nbio.structure.align.model.AFPChain;
import org.biojava.nbio.structure.align.util.AtomCache;
import org.biojava.nbio.structure.align.util.RotationAxis;
import org.biojava.nbio.structure.jama.Matrix;

import java.io.IOException;

/**
 * A demo for how to use {@link RotationAxis} to display the rotation for an
 * alignment. This is particularly useful for symmetric alignments, eg between
 * several chains of a symmetric or pseudo-symmetric complex.
 *
 * @author Spencer Bliven
 *
 */
public final class DemoRotationAxis {

	public static void main(String[] args) {

		// Compare two chains of a dimer to force CE to give a symmetric alignment.
		String name1 = "1AVD.A";
		String name2 = "1AVD.B";
		String display = "1AVD";

		//		name1 = "4HHB.A:,B:";
		//		name2 = "4HHB.C:,D:";
		//		display = "4HHB";

		AtomCache cache = new AtomCache();
		try {
			// Get the structures
			Atom[] ca1 = cache.getAtoms(name1);
			Atom[] ca2 = cache.getAtoms(name2);
			Atom[] caD = cache.getAtoms(display);

			// Perform the alignment
			StructureAlignment ce = StructureAlignmentFactory.getAlgorithm(CeMain.algorithmName);
			AFPChain afpChain = ce.align(ca1, ca2);

			// Calculate the axis of rotation
			Matrix mat = afpChain.getBlockRotationMatrix()[0];
			Atom shift = afpChain.getBlockShiftVector()[0];
			RotationAxis axis = new RotationAxis(mat,shift);

			// Print the angle of rotation
			double theta = Math.toDegrees(axis.getAngle());
			System.out.format("Angle: %f degrees%n",theta);

			// Display the alignment with Jmol
			StructureAlignmentJmol jmolPanel = new StructureAlignmentJmol();
			jmolPanel.setAtoms(caD);

			// Set some standard protein display properties
			jmolPanel.evalString("select * ; color chain;");
			jmolPanel.evalString("select nucleic; cartoon on;");
			jmolPanel.evalString("select *; spacefill off; wireframe off; cartoon on;  ");

			// draw axis
			String jmolString = axis.getJmolScript(caD);
			jmolPanel.evalString(jmolString);



			/*
			// draw intermediate vectors for debugging
			double width = .5;
			Atom s = axis.getRotationPos();
			Atom u = axis.getRotationAxis();
			jmolPanel.evalString(String.format("draw ID s VECTOR {0,0,0} {%f,%f,%f} WIDTH %f COLOR orange \">s\";",
					s.getX(),s.getY(),s.getZ(), width ));

			Atom perp = axis.getOtherTranslation();
			Atom screw = axis.getScrewTranslation();

			double uScale = 10;
			jmolPanel.evalString(String.format("draw ID u VECTOR {0,0,0} {%f,%f,%f} WIDTH %f COLOR orange \">u\";",
					uScale*u.getX(),uScale*u.getY(),uScale*u.getZ(), width ));

			jmolPanel.evalString(String.format("draw ID perp VECTOR {0,0,0} {%f,%f,%f} WIDTH %f COLOR yellow \">tPerp\";",
					perp.getX(),perp.getY(),perp.getZ(), width));
			jmolPanel.evalString(String.format("draw ID screw VECTOR {0,0,0} {%f,%f,%f} WIDTH %f COLOR yellow \">screw\";",
					screw.getX(),screw.getY(),screw.getZ(), width));

			jmolPanel.evalString(String.format("draw ID t VECTOR {0,0,0} {%f,%f,%f} WIDTH %f COLOR yellow \">t\";",
					shift.getX(),shift.getY(),shift.getZ(), width));

			// draw coordinate axes
			jmolPanel.evalString("draw ID x VECTOR {0,0,0} {5,0,0} WIDTH 0.5 COLOR red \">x\";");
			jmolPanel.evalString("draw ID y VECTOR {0,0,0} {0,5,0} WIDTH 0.5 COLOR green \">y\";");
			jmolPanel.evalString("draw ID z VECTOR {0,0,0} {0,0,5} WIDTH 0.5 COLOR blue \">z\";");
			*/

		} catch (IOException | StructureException e) {
			e.printStackTrace();
		}
    }
}
