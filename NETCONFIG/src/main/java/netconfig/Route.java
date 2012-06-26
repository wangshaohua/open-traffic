/*  Copyright 2010. The Regents of the University of California (Regents).
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
//import java.util.ArrayList;
//import java.util.Collection;
//import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
//import java.util.Map.Entry;
//import java.util.TreeMap;

import core.GeoMultiLine;
import core.Monitor;
import com.google.common.collect.ImmutableList;

//import com.google.common.collect.ImmutableCollection;

/**
 * This class represents a route object. A route is defined a possible way of
 * traversing a network including lane information.
 * 
 * @param <LINK>
 *            Subclass of Link that this route is on.
 * @author samitha, Ryan Herring
 * @author tjhunter
 */
public class Route<LINK extends Link> implements Serializable {

    public static final long serialVersionUID = 2L;
    /** The list of spots that defines this route. */
    public final ImmutableList<Spot<LINK>> spots;
    /** The list of links that defines this route. */
    public final ImmutableList<LINK> links;

    // /**
    // * Provides a mapping between each link and the route offset of the
    // * beginning of that link
    // */
    // private final HashMap<LINK, Float> offsetMap = new HashMap<LINK,
    // Float>();
    // /**
    // * Provides an inverse mapping between a route offset and the link that
    // * begins at that offset
    // */
    // private final TreeMap<Float, LINK> reverseOffsetMap = new TreeMap<Float,
    // LINK>();

    private Route(ImmutableList<Spot<LINK>> spots, ImmutableList<LINK> links) {
        this.spots = spots;
        this.links = links;
    }

    /**
     * returns the offset of the first spot (equivalently link) of the route
     * 
     * @return the offset of the first spot (equivalently link) of the route
     */
    public double startOffset() {
        return spots.get(0).offset;
    }

    /**
     * returns the offset of the last spot (equivalently link) of the route
     * 
     * @return the offset of the last spot (equivalently link) of the route
     */
    public double endOffset() {
        return spots.get(this.spots.size() - 1).offset;
    }

    /**
     * returns the length of the route (meters)
     * 
     * @return returns the length of the route (meters)
     */
    public double length() {
        if (this.links.size() == 1) {
            return this.endOffset() - this.startOffset();
        }
        double res = this.links.get(0).length() - this.startOffset();
        for (int i = 1; i < (this.links.size() - 1); i++) {
            res += this.links.get(i).length();
        }
        res += this.endOffset();
        return res;
    }

    public Spot<LINK> startSpot() {
        return spots.get(0);
    }

    public Spot<LINK> endSpot() {
        return spots.get(spots.size() - 1);
    }

    @Override
    protected Object clone() throws CloneNotSupportedException {
        throw new CloneNotSupportedException("No route cloneing!");
    }

    @Override
    public String toString() {
        StringBuilder ret = new StringBuilder();
        ret.append("Route with ");
        ret.append(this.spots.size());
        ret.append(" spots:");
        for (Spot<LINK> spot : this.spots) {
            ret.append("\t");
            ret.append(spot.toString());
        }
        return ret.toString();
    }

    private boolean all_from_same_link() {
        for (int i = 1; i < this.spots.size(); ++i) {
            if (spots.get(i).link != spots.get(0).link) {
                return false;
            }
        }
        return true;
    }

    /**
     * 
     * @return the geometry of a route.
     */
    public GeoMultiLine geoMultiLine() throws NetconfigException {
        // Grrr where is my for all?
        if (all_from_same_link()) {
            return startSpot().link.geoMultiLine().getPartialGeometry(startOffset(),
                    endOffset());
        }
        GeoMultiLine res = null;
        final double first_offset_diff = startSpot().link.length()
                - startOffset();
        if (first_offset_diff > 0) {
            res = startSpot().link.geoMultiLine().getPartialGeometry(startOffset(),
                    startSpot().link.length());
        }

        for (int i = 1; i < links.size() - 1; ++i) {
            final LINK l = links.get(i);
            res = GeoMultiLine.greedyConcatenation(res, l.geoMultiLine(), 2.0);
        }

        final double last_offset_dff = endOffset();
        if (last_offset_dff > 0) {
            res = GeoMultiLine.greedyConcatenation(res,
                    endSpot().link.geoMultiLine().getPartialGeometry(0, endOffset()), 2.0);
        }
        return res;
    }

    // // returns turn by turn directions with one direction per each element in
    // // this array
    // public String[] getDirections() {
    // return null;
    // }

    // /**
    // * Gets a spot along the route given the specified route offset. The
    // * returned spot will have no lane information (i.e. lane = 0).
    // *
    // * @param offset
    // * The offset along the route
    // * @return A spot along the route
    // * @throws NetconfigException
    // * if there's an error creating the spot
    // */
    // public Spot<LINK> getSpotFromRouteOffset(float offset)
    // throws NetconfigException {
    // if (offset < -1e-9 || offset > this.getRouteLength()) {
    // throw new IllegalArgumentException(
    // "offset must be greater than 0 and less than the length of the route");
    // }
    //
    // Entry<Float, LINK> e = this.reverseOffsetMap.floorEntry(offset);
    //
    // return new Spot<LINK>(e.getValue(), offset - e.getKey(), (short) 0);
    // }
    // /**
    // * Gets the offset along the route of the given spot
    // *
    // * @param spot
    // * A spot to determine its offset along the route
    // * @return The offset along the route of the given spot
    // * @throws NetconfigException
    // * If the spot is not on the route
    // */
    // public float offsetOnRoute(Spot<LINK> spot) throws NetconfigException {
    // if (!this.offsetMap.containsKey(spot.link) || !this.isSpotOnRoute(spot))
    // {
    // throw new NetconfigException(null, "Spot is not on route.");
    // }
    // if (spot.link.equals(this.getFirstSpot().link)) {
    // return spot.offset - this.getFirstSpot().offset;
    // }
    // return spot.offset + this.offsetMap.get(spot.link);
    // }
    // /**
    // * Fills a hashmap ({@link #offsetMap}) with the beginning offset for each
    // * link on the route. This allows for quick access to the offset instead
    // of
    // * having to search for it everytime its needed
    // */
    // private void createOffsetMap() {
    // this.offsetMap.put(this.links[0], 0.0f);
    // this.reverseOffsetMap.put(0.0f, this.links[0]);
    // float cur_offset = this.links[0].length()
    // - this.getFirstSpot().offset;
    // for (int i = 1; i < this.links.size() - 1; i++) {
    // this.offsetMap.put(this.links[i], cur_offset);
    // this.reverseOffsetMap.put(cur_offset, this.links[i]);
    // cur_offset += this.links[i].length();
    // }
    // if (this.links.size() > 1) {
    // this.offsetMap.put(this.links[this.links.size() - 1], cur_offset);
    // this.reverseOffsetMap.put(cur_offset,
    // this.links[this.links.size() - 1]);
    // }
    // }

    // public LINK[] getLinks() {
    // return this.links;
    // }

    // @SuppressWarnings("rawtypes")
    // @Override
    // public boolean equals(Object obj) {
    // if (obj == null) {
    // return false;
    // }
    // if (getClass() != obj.getClass()) {
    // return false;
    // }
    // final Route other = (Route) obj;
    // if (this.spots.size() != other.spots.size()) {
    // return false;
    // }
    // for (int i = 0; i < this.spots.size(); i++) {
    // if (!this.spots[i].equals(other.spots[i])) {
    // return false;
    // }
    // }
    // return true;
    // }

    // /**
    // * Checks the equality of two routes within the tolerance margin of all
    // * offsets on a network.
    // *
    // * TODO(tjh) document
    // *
    // * @param other
    // * @return true if this route is equal to the given route within some
    // small
    // * tolerance
    // */
    // public boolean equalsWithinTolerance(Route<LINK> other) {
    // if (other.links.size() != links.size()) {
    // return false;
    // }
    // final double diff1 = this.startOffset() - other.startOffset();
    // if (Math.abs(diff1) > Link.size()_PRECISION) {
    // return false;
    // }
    // if (Math.abs(this.endOffset() - other.endOffset()) >
    // Link.size()_PRECISION) {
    // return false;
    // }
    // for (int i = 0; i < links.size(); ++i) {
    // if (links[i] != other.links[i]) {
    // return false;
    // }
    // }
    // return true;
    // }

    // @Override
    // public int hashCode() {
    // int hash = 5;
    // hash = 59 * hash + (this.spots != null ? this.spots.hashCode() : 0);
    // return hash;
    // }
    // /**
    // * Determines if this route is entirely contained within the given route.
    // *
    // * @param other_route
    // * A route to compare this route to, in order to see if this
    // * route is entirely contained with the supplied route
    // * @return True if this route is contained within the given route
    // *
    // * NOTE: <b>THIS FUNCTION DOES NOT TAKE INTO ACCOUNT LANE
    // * INFORMATION AS OF RIGHT NOW</b>
    // *
    // * TODO(?) Change function to take lane information into account
    // */
    // public boolean isContainedWithinRoute(Route<LINK> other_route) {
    // if (this.links.size() == 0 || other_route.links.size() == 0
    // || this.links.size() > other_route.links.size()) {
    // return false;
    // }
    // if (this.links[0].equals(other_route.links[0])) {
    // if (this.startOffset() < other_route.startOffset()) {
    // return false;
    // }
    // }
    // if (this.links[this.links.size() - 1]
    // .equals(other_route.links[other_route.links.size() - 1])) {
    // if (this.endOffset() > other_route.endOffset()) {
    // return false;
    // }
    // }
    // Integer startIndex = null;
    // for (int i = 0; i < other_route.links.size(); i++) {
    // if (this.links[0].equals(other_route.links[i])) {
    // startIndex = i;
    // break;
    // }
    // }
    // if (startIndex == null) {
    // return false;
    // }
    //
    // for (int i = 1; i < this.links.size(); i++) {
    // if (i + startIndex >= other_route.links.size()) {
    // break;
    // }
    // if (!this.links[i].equals(other_route.links[i + startIndex])) {
    // return false;
    // }
    // }
    //
    // return true;
    // }
    // /**
    // * This function determines if this spot is on this route object. If true,
    // * it means that this spot would be "driven over" if traversing the entire
    // * route.
    // *
    // * TODO(?) THIS FUNCTION DOES NOT CURRENTLY TAKE LANES INTO ACCOUNT
    // *
    // * @param spot
    // * A spot to check if it is on this route
    // * @return True if the given spot is on the route
    // */
    // public boolean isSpotOnRoute(Spot<LINK> spot) {
    // if (!this.offsetMap.containsKey(spot.link)) {
    // return false;
    // }
    // if (spot.link.equals(this.getFirstSpot().link)) {
    // if (spot.link.equals(this.getLastSpot().link)) {
    // return spot.offset >= this.getFirstSpot().offset
    // && spot.offset <= this.getLastSpot().offset;
    // } else {
    // return spot.offset >= this.getFirstSpot().offset;
    // }
    // }
    // if (spot.link.equals(this.getLastSpot().link)) {
    // return spot.offset <= this.getLastSpot().offset;
    // }
    //
    // return this.offsetMap.containsKey(spot.link); // spot must be in the
    // // "middle links" of the
    // // route
    // }

    /********** Static factory methods **************/
    // These should be the only methods avaialable to the user to create a Route
    // object.

    public static <LINK extends Link> Route<LINK> from(
            ImmutableList<Spot<LINK>> spots, ImmutableList<LINK> links) {
        return new Route<LINK>(spots, links);
    }

    /**
     * Instantiates a route in less general terms using only links. This is
     * converted to a list of spots with unknown lane information and one spot
     * for each link. The first spot is placed at the start of the first link,
     * the last spot is placed at the end of the last link, and all the others
     * placed in the middle of their link. The links provided must all be
     * adjacent.
     * 
     * @param links
     *            A list of (adjacent) links that make up the route.
     * @throws netconfig.NetconfigException
     */
    public static <LINK extends Link> Route<LINK> fromLinks(List<LINK> links)
            throws NetconfigException {
        return from(links, 0f, -1);
    }

    /**
     * Instantiates a route in its most general form. Has the restriction that
     * two consecutive spots must be on the same link or adjacent links. It is
     * assumed that one could "drive in a straight line" between each spot and
     * that would give the correct route.
     * 
     * @param spots
     *            The road locations that define the route.
     * @throws netconfig.NetconfigException
     */
    public static <LINK extends Link> Route<LINK> fromSpots(
            List<Spot<LINK>> spots0) throws NetconfigException {
        // We only need one of each LINK, even if mutiple spots on a link.
        // If the given spots are in driving order, then we only have
        // to check each link against the last one, and we can use the
        // reference to do this because links are created only once in the
        // network instatiation.
        ImmutableList<Spot<LINK>> spots = ImmutableList.copyOf(spots0);
        if (spots.size() == 0) {
            throw new NetconfigException(null, "Length of spots cannot be 0");
        }
        ImmutableList.Builder<LINK> noRepeatedLinks = new ImmutableList.Builder<LINK>();

        LINK lastLink = null;
        for (Spot<LINK> spot : spots) {
            if (spot.link != lastLink) {
                noRepeatedLinks.add(spot.link);
            }
            lastLink = spot.link;
        }
        ImmutableList<LINK> links = noRepeatedLinks.build();

        if (!linksAdjacent(links)) {
            throw new NetconfigException(null,
                    "Spots in the argument aren't on the same or adjacent links.");
        }
        sanityChecks(spots, links);
        return new Route<LINK>(spots, links);
    }

    /**
     * Same as the {@link Route#Route(netconfig.Link[]) Route(Link[])}
     * constructor but also includes information about the offset on the
     * starting and ending links of the route. The first spot is placed at
     * startOffset along the first link, ditto for the ending link, the others
     * are placed in the middle of the link.
     * 
     * @param links
     *            List of links that make up the route.
     * @param startOffset
     *            Offset along the first link of the route.
     * @param endOffset
     *            Offset along the last link of the route.
     * @throws netconfig.NetconfigException
     */
    public static <LINK extends Link> Route<LINK> from(List<LINK> links,
            double startOffset, double endOffset) throws NetconfigException {
        if (links == null) {
            Monitor.debug("The links of a route cannot be null");
            throw new NetconfigException(null,
                    "The links of a route cannot be null or zero length");
        }
        if (links.size() == 0) {
            Monitor.debug("zero length");
            throw new NetconfigException(null,
                    "The links of a route cannot be null or zero length");
        }
        // if (!linksAdjacent(links)) {
        // throw new NetconfigException(null,
        // "At least one non-adjacent link in argument.");
        // }

        if (startOffset < 0) {
            throw new NetconfigException(null,
                    "Argument startOffset negative.  Distance beyond link is "
                            + startOffset + " meters");
        }
        if (startOffset < 0 || startOffset > links.get(0).length() + 1e-3) {
            throw new NetconfigException(null,
                    "Argument startOffset outside of 1st link.  Distance beyond link is "
                            + (startOffset - links.get(0).length()) + " meters");
        }
        if (startOffset > links.get(0).length()) {
            startOffset = links.get(0).length();
        }
        if (endOffset == -1) {
            endOffset = links.get(links.size() - 1).length();
        }
        if (links.size() == 1 && startOffset > endOffset) {
            throw new NetconfigException(null, " Bad offsets " + links.get(0)
                    + " start=" + startOffset + ", end=" + endOffset);
        }
        if (endOffset < 0
                || endOffset > links.get(links.size() - 1).length() + 1e-3) {
            throw new NetconfigException(
                    null,
                    "Argument endOffset outside of last link.  Distance beyond link is "
                            + (endOffset - links.get(links.size() - 1).length())
                            + " meters");
        }
        if (endOffset > links.get(links.size() - 1).length()) {
            endOffset = links.get(links.size() - 1).length();
        }

        for (int i = 0; i < links.size(); ++i) {
            if (links.get(i) == null) {
                throw new NetconfigException(null, "Null link");
            }
        }

        ImmutableList<LINK> links0 = ImmutableList.copyOf(links);
        if (!linksAdjacent(links0)) {
            throw new NetconfigException(null,
                    "Spots in the argument aren't on the same or adjacent links.");
        }

        ImmutableList<Spot<LINK>> spots0 = null;
        Spot<LINK> startSpot = Spot.from(links.get(0), startOffset);
        Spot<LINK> endSpot = Spot.from(links.get(links.size() - 1), endOffset);
        if (links.size() == 1) {
            spots0 = ImmutableList.of(startSpot, endSpot);
        } else {
            ImmutableList.Builder<Spot<LINK>> spotsBuilder = 
                new ImmutableList.Builder<Spot<LINK>>();
            spotsBuilder.add(startSpot);
            for (int i = 1; i < links0.size() - 1; i++) {
                spotsBuilder.add(Spot.from(links0.get(i), 0.5 * links0.get(i)
                        .length()));
            }

            spots0 = spotsBuilder.build();
        }
        sanityChecks(spots0, links0);
        return from(spots0, links0);
    }

    /**
     * Static helper function to check if each link in the given list is
     * connected and next from the previous link in the list.
     * 
     * @param links
     *            the list of links to check.
     * @return true if all the links are adjacent.
     */
    private static <LINK extends Link> boolean linksAdjacent(List<LINK> links)
            throws NetconfigException {
        if (links.get(0) == null) {
            throw new NetconfigException(null,
                    "Links on the route are not allowed to be null.");
        }
        for (int i = 1; i < links.size(); i++) {
            if (links.get(i) == null) {
                throw new NetconfigException(null,
                        "Links on the route are not allowed to be null.");
            }
            // Since network creates all the nodes we can use reference to
            // check if they are the same node.
            if (!(links.get(i).startNode() == links.get(i - 1).endNode())) {
                return false;
            }
        }
        return true;
    }

    /**
     * Additional sanity checks.
     * 
     * @throws NetconfigException
     */
    private static <LINK extends Link> void sanityChecks(
            ImmutableList<Spot<LINK>> spots, ImmutableList<LINK> links)
            throws NetconfigException {
        // Going backward
        for (int i = 1; i < spots.size(); ++i) {
            if (spots.get(i - 1).link == spots.get(i).link
                    && spots.get(i - 1).offset > spots.get(i).offset) {
                throw new NetconfigException(null, "Bad spots in route "
                        + spots + "," + links);
            }
        }
        // Links are not unique
        HashSet<LINK> links0 = new HashSet<LINK>();
        for (LINK l : links) {
            if (links0.contains(l)) {
                throw new NetconfigException(null, "Found duplicate link " + l
                        + " in " + links);
            }
            links0.add(l);
        }
    }

    // ********* SCALA CONVENIENCE FUNCTIONS ***********
    /**
     * Same as the {@link Route#Route(netconfig.Link[]) Route(Link[])}
     * constructor but also includes information about the offset on the
     * starting and ending links of the route. The first spot is placed at
     * startOffset along the first link, ditto for the ending link, the others
     * are placed in the middle of the link.
     * 
     * @param links
     *            List of links that make up the route.
     * @param startOffset
     *            Offset along the first link of the route.
     * @param endOffset
     *            Offset along the last link of the route.
     * @throws netconfig.NetconfigException
     */
    public static <LINK extends Link> Route<LINK> apply(List<LINK> links,
            double startOffset, double endOffset) throws NetconfigException {
        return from(links, startOffset, endOffset);
    }

}
