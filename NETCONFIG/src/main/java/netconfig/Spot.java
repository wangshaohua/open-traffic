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
package netconfig;

import java.io.Serializable;
import core.Coordinate;

/**
 * This is the base class for encoding locations on a network link.
 * @param <LINK>
 *            A subclass of Link, used in this instance.
 * @author samitha
 * @author Ryan Herring
 * @author tjhunter
 * 
 */
public class Spot<LINK extends Link> implements Serializable {

    /** Link this spot is on. */
    private final LINK link_;
    /**
     * Offset from the start of said Link (in meters). The offset is such that:
     * 0 <= offset <= length of the link
     */
    private final double offset_;
    /**
     * Lane, from right-most (slow-lane) to the left-most, starting at 1.<br>
     * A lane value of 0 means the lane information is unknown.<br>
     * A lane value of -1 means all lanes.
     */
    private final short lane_;

    /**
     * This is the only constructor. This means that once a spot is created its
     * ID (link, offset, & lane) cannot be changed. One has to create a new spot
     * if one wants to ``move’’ it.
     * 
     * @param link
     *            where the spot is being created.
     * @param offset
     *            offset of the Spot in meters from the start of the link.
     * @param lane
     *            lane number for which the spot belongs to, see {@link #lane_}.
     * @throws NetconfigException
     *             on any error.
     * @see Link#getNumLanesAtOffset(double)
     */
    private Spot(LINK link, double offset, short lane) {
        this.link_ = link;
        this.offset_ = offset;
        this.lane_ = lane;
    }

    /*
     * Returns the coordinate corresponding to this spot on the network, or null
     * if the conversion could not be done.
     */
    public Coordinate toCoordinate() throws NetconfigException {
        return link().geoMultiLine().getCoordinate(offset());
    }

    /**
     * See the {@link Spot#compareTo(java.lang.Object) compareTo} method for a
     * description of how Spots are compared.
     * 
     * @param o
     *            The other Spot to see if it is equal to this Spot
     * @return True if the two Spots are equal.
     */
    @SuppressWarnings("rawtypes")
    @Override
    public boolean equals(Object o) {
        if ((null == o) || (this.getClass() != o.getClass())
                || (this.link().getClass() != ((Spot) o).link().getClass())) {
            return false;
        }

        @SuppressWarnings("unchecked")
        Spot<LINK> that = (Spot<LINK>) o;

        if ((this.link().equals(that.link())) && (this.offset() == that.offset())
                && (this.lane() == that.lane())) {
            return true;
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 97 * hash + (this.link() != null ? this.link().hashCode() : 0);
        // hash = 97 * hash + Double.doubleToIntBits(this.offset);
        hash = 97 * hash + this.lane();
        return hash;
    }

    /**
     * Returns a spot with the same attributes.
     * 
     * @return a spot with the same attributes
     * @throws java.lang.CloneNotSupportedException
     */
    @Override
    protected Object clone() throws CloneNotSupportedException {
        return this; // Object is immutable.
    }

    @Override
    public String toString() {
        return String.format("Spot(%s, %f, %d)", this.link().toString(),
                this.offset(), this.lane());
    }

    /** Rid us of a warning. */
    public static final long serialVersionUID = (long) 1;

    public static <LINK extends Link> Spot<LINK> from(LINK link, double offset,
            short lane) throws NetconfigException {
        // This will throw a NetconfigException if the offset is less than
        // 0 or greater than the length of the link.
        if (link == null) {
            throw new NetconfigException(null, "link is null");
        }
        if (lane < -1 || lane > link.numLanesAtOffset(offset)) {
            throw new NetconfigException(null, String.format(
                    "Lane (%d) invaid for link %s at %f meters.", lane,
                    link.toString(), offset));
        }
        return new Spot<LINK>(link, offset, lane);
    }

    public static <LINK extends Link> Spot<LINK> from(LINK link, double offset)
            throws NetconfigException {
        return from(link, offset, (short) -1);
    }

    /**
     * Convenience function for scala users.
     * 
     * @param <LINK>
     * @param link
     * @param offset
     * @param lane
     * @return
     * @throws NetconfigException
     */
    public static <LINK extends Link> Spot<LINK> apply(LINK link,
            double offset, short lane) throws NetconfigException {
        return from(link, offset, lane);
    }

    /**
     * Lane, from right-most (slow-lane) to the left-most, starting at 1.<br>
     * A lane value of 0 means the lane information is unknown.<br>
     * A lane value of -1 means all lanes.
     */
	public short lane() {
		return lane_;
	}

    /**
     * Offset from the start of said Link (in meters). The offset is such that:
     * 0 <= offset <= length of the link
     */
	public double offset() {
		return offset_;
	}

    /** Link this spot is on. */
	public LINK link() {
		return link_;
	}
}
