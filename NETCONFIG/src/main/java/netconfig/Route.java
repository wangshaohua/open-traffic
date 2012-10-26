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
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import core.Coordinate;

import core.GeoMultiLine;
import core.Monitor;
import com.google.common.collect.ImmutableList;

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
    private final ImmutableList<Spot<LINK>> spots_;
    /** The list of links that defines this route. */
    private final ImmutableList<LINK> links_;

    private Route(ImmutableList<Spot<LINK>> spots, ImmutableList<LINK> links) {
        this.spots_ = spots;
        this.links_ = links;
    }

    /**
     * returns the offset of the first spot (equivalently link) of the route
     * 
     * @return the offset of the first spot (equivalently link) of the route
     */
    public double startOffset() {
        return spots().get(0).offset();
    }

    /**
     * returns the offset of the last spot (equivalently link) of the route
     * 
     * @return the offset of the last spot (equivalently link) of the route
     */
    public double endOffset() {
        return spots().get(this.spots().size() - 1).offset();
    }

    /**
     * returns the length of the route (meters)
     * 
     * @return returns the length of the route (meters)
     */
    public double length() {
        if (this.links().size() == 1) {
            return this.endOffset() - this.startOffset();
        }
        double res = this.links().get(0).length() - this.startOffset();
        for (int i = 1; i < (this.links().size() - 1); i++) {
            res += this.links().get(i).length();
        }
        res += this.endOffset();
        return res;
    }

    public Spot<LINK> startSpot() {
        return spots().get(0);
    }

    public Spot<LINK> endSpot() {
        return spots().get(spots().size() - 1);
    }

    @Override
    protected Object clone() throws CloneNotSupportedException {
        throw new CloneNotSupportedException("No route cloneing!");
    }

    @Override
    public String toString() {
        StringBuilder ret = new StringBuilder();
        ret.append("Route with ");
        ret.append(this.spots().size());
        ret.append(" spots:");
        for (Spot<LINK> spot : this.spots()) {
            ret.append("\t");
            ret.append(spot.toString());
        }
        return ret.toString();
    }

    private boolean all_from_same_link() {
        for (int i = 1; i < this.spots().size(); ++i) {
            if (spots().get(i).link() != spots().get(0).link()) {
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
            return startSpot().link().geoMultiLine()
                    .getPartialGeometry(startOffset(), endOffset());
        }
        List<Coordinate> start_cs = new ArrayList<Coordinate>();
        for (Coordinate c : startSpot().link().geoMultiLine()
                .getPartialGeometry(startOffset(), startSpot().link().length())
                .getCoordinates()) {
            start_cs.add(c);
        }
        List<Coordinate> cs = start_cs;
        for (int i = 1; i < links().size() - 1; ++i) {
            final LINK l = links().get(i);
            List<Coordinate> link_cs = new ArrayList<Coordinate>();
            for (Coordinate c : l.geoMultiLine().getCoordinates()) {
                link_cs.add(c);
            }
            cs = Coordinate.greedyConcatenation(cs, link_cs);
        }
        List<Coordinate> end_cs = new ArrayList<Coordinate>();
        GeoMultiLine end_gmm = endSpot().link().geoMultiLine();
        for (Coordinate c : end_gmm.getPartialGeometry(0.0, endOffset())
                .getCoordinates()) {
            end_cs.add(c);
        }
        cs = Coordinate.greedyConcatenation(cs, end_cs);

        return new GeoMultiLine(cs);
    }

    /**
     * @return the spots_
     */
    public ImmutableList<Spot<LINK>> spots() {
        return spots_;
    }

    /**
     * @return the links_
     */
    public ImmutableList<LINK> links() {
        return links_;
    }
    
//     /**
//      * Returns the concatenation of a route with another one.
//      * @param other
//      * @return
//      * @throws NetconfigException if the first spot of other is different from
//      * the last spot of the other route.
//      */
//     public Route<LINK> concatenate(Route<LINK> other) throws NetconfigException {
//         int n = this.spots().size();
//         if (!other.spots().get(0).equals(this.spots().get(n-1))) {
//             throw new NetconfigException(null, "Routes are not joined");
//         }
//         int num_links = other.links().size();
//         ImmutableList.Builder<LINK> link_builder = new ImmutableList.Builder<LINK>();
//         for (LINK l:this.links()) {
//             link_builder.add(l);
//         }
//         UnmodifiableIterator<LINK> it = other.links().iterator();
//         it.next();
//         while(it.hasNext()) {
//             //FINISH
//         }
// 
//     }
    
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
     * @param spots_
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
            if (spot.link() != lastLink) {
                noRepeatedLinks.add(spot.link());
            }
            lastLink = spot.link();
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
            // Stupid way to break a line.
            ImmutableList.Builder<Spot<LINK>> spotsBuilder = null;
            spotsBuilder = new ImmutableList.Builder<Spot<LINK>>();
            spotsBuilder.add(startSpot);
            for (int i = 1; i < links0.size() - 1; i++) {
                spotsBuilder.add(Spot.from(links0.get(i), 0.5 * links0.get(i)
                        .length()));
            }
            // Do not forget the last spot...
            spotsBuilder.add(endSpot);
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
            if (spots.get(i - 1).link() == spots.get(i).link()
                    && spots.get(i - 1).offset() > spots.get(i).offset()) {
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
