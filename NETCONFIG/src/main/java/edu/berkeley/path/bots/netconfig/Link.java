/*  Copyright ©2010. The Regents of the University of California (Regents).
 *  All Rights Reserved. Permission to use, copy, modify, and distribute this
 *  software and its documentation for educational, research, and not-for-profit
 *  purposes, without fee and without a signed licensing agreement, is hereby
 *  granted, provided that the above copyright notice, this paragraph and the
 *  following two paragraphs appear in all copies, modifications, and
 *  distributions. Contact The Office of Technology Licensing, UC Berkeley,
 *  2150 Shattuck Avenue, Suite 510, Berkeley, CA 94720-1620, (510) 643-7201,
 *  for commercial licensing opportunities.
 *
 *  IN NO EVENT SHALL REGENTS BE LIABLE TO ANY PARTY FOR DIRECT, INDIRECT,
 *  SPECIAL, INCIDENTAL, OR CONSEQUENTIAL DAMAGES, INCLUDING LOST PROFITS,
 *  ARISING OUT OF THE USE OF THIS SOFTWARE AND ITS DOCUMENTATION, EVEN IF
 *  REGENTS HAS BEEN ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 *  REGENTS SPECIFICALLY DISCLAIMS ANY WARRANTIES, INCLUDING, BUT NOT LIMITED
 *  TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 *  PURPOSE. THE SOFTWARE AND ACCOMPANYING DOCUMENTATION, IF ANY, PROVIDED
 *  HEREUNDER IS PROVIDED "AS IS". REGENTS HAS NO OBLIGATION TO PROVIDE
 *  MAINTENANCE, SUPPORT, UPDATES, ENHANCEMENTS, OR MODIFICATIONS.
 */

package edu.berkeley.path.bots.netconfig;

import java.io.Serializable;


import com.google.common.collect.ImmutableList;

import edu.berkeley.path.bots.core.GeoMultiLine;

/**
 * Abstract class (must sub-class) for a link. Implements comparable (using the
 * ID) so that spots can.
 * 
 * @author Saneesh.Apte
 * @author tjhunter
 */
public interface Link extends Serializable {

    /**
     * TODO(tjh) DOC!. The canonical precision of all measurements of lengths and
     * offsets. When performing equality or inequality checks on any length or
     * offset, all result should be interpreted within the tolerance of this
     * value.
     * 
     * Where does it come from? The distance functions have an algorithmic
     * imprecision that will cause slight variations.
     * 
     * Empirical evidence suggests this threshold could be lowered easily to
     * 1e-4.
     */
    public static final double LENGTH_PRECISION = 0.1;
    // Shift in the geometry to separate two links on different directions.
    // private static final double GEOM_SHIFT_METERS = -2.0;

    /**
     * This function returns the length of the link
     * 
     * @return the length of the link
     */
    public double length();

    /**
     * Needed for spot checking.
     * 
     * @param offset
     * @return number of lanes at offset
     * @throws NetconfigException
     *             if offset is < 0 or > length, or other error
     */
    public short numLanesAtOffset(double offset)
            throws NetconfigException;

    public Node startNode();

    public Node endNode();

    /**
     * @return a list of links that flow out of this link.
     */
    public ImmutableList<? extends Link> outLinks() throws NetconfigException;

    /**
     * @return a list of links that flow into this link.
     */
    public ImmutableList<? extends Link> inLinks() throws NetconfigException;

    /**
     * 
     * @return the geometry of this link.
     * @throws NetconfigException if not defined for this link.
     */
    public GeoMultiLine geoMultiLine() throws NetconfigException;
} // class
