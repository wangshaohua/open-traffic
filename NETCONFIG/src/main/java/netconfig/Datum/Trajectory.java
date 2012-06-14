//package netconfig.Datum;
//
//import core.Coordinate;
//import core.Time;
//import netconfig.NetconfigException;
//import netconfig.Spot;
//import netconfig.Link;
//import netconfig.Route;
//import java.io.Serializable;
//import java.util.ArrayList;
//import java.util.Collection;
//import java.util.HashSet;
//
///**
// * A trajectory segment: a collection of segments of routes put together.
// * 
// * Note: a lot of useful functions are also available in netconfig_extensions.
// * 
// * TODO(?) this structure is way too complicated. Check how useful it is. 
// * 
// * TODO(?) This structure looses information.
// * 
// * @param <LINK>
// * @author tjhunter
// */
//public class Trajectory<LINK extends Link> implements Serializable {
//
//    public static final long serialVersionUID = 0L;
//    /**
//     * Waypoints of the trajectory. The first one is the beginning of the
//     * trajectory, the last one is the end of the trajectory.
//     */
//    public final TSpot<LINK>[] waypoints;
//    /**
//     * The collection of links (may contain repetitions when loops occurs)
//     */
//    public final LINK[] links;
//    /**
//     * id of the driver.
//     */
//    public final String id;
//    /**
//     * Correspondance map between the waypoints and the links to determine
//     * without ambiguity how to associate a waypoint to the followed path.
//     * 
//     * Invariant: waypoints[i].spot.link == links[waypoints_map[i]]
//     */
//    public final int[] waypoints_map;
//
//    public Trajectory(TSpot<LINK>[] waypoints, LINK[] links, int[] wp_map)
//            throws NetconfigException {
//        this.waypoints = waypoints;
//        this.links = links;
//        this.waypoints_map = wp_map;
//        sanityChecks();
//        this.id = waypoints[0].id;
//    }
//
//    private void sanityChecks() throws NetconfigException {
//        // at least two end waypoints
//        if (waypoints == null) {
//            throw new NetconfigException(null, "Null waypoints");
//        }
//        if (waypoints.length < 2) {
//            throw new NetconfigException(null, "Not enough waypoints");
//        }
//        // at least one link
//        if (links == null) {
//            throw new NetconfigException(null, "Null waypoints");
//        }
//        if (links.length == 0) {
//            throw new NetconfigException(null, "Not enough waypoints");
//        }
//        // waypoint map is same size as waypoints
//        if (waypoints_map == null) {
//            throw new NetconfigException(null, "Null waypoint map");
//        }
//        if (waypoints.length != waypoints_map.length) {
//            throw new NetconfigException(null,
//                    "Waypoint map and list are different");
//        }
//
//        for (LINK l : links) {
//            if (l == null) {
//                throw new NetconfigException(null, "Null link detected");
//            }
//        }
//
//        // After this point, printing this object is safe (except for id)
//
//        for (int i : waypoints_map) {
//            if (i < 0 || i >= links.length) {
//                throw new NetconfigException(null,
//                        "Waypoint map does not adress link" + this);
//            }
//        }
//        for (int i = 0; i < waypoints_map.length; ++i) {
//            if (waypoints[i].spot.link != links[waypoints_map[i]]) {
//                throw new NetconfigException(null,
//                        "Wrong index for waypoint map" + this);
//            }
//        }
//        for (int i = 1; i < waypoints_map.length; ++i) {
//            if (waypoints_map[i - 1] > waypoints_map[i]) {
//                throw new NetconfigException(null, "Waypoints are not in order"
//                        + this);
//            }
//        }
//        if (waypoints_map[0] != 0) {
//            throw new NetconfigException(null, "First waypoint wrongly indexed"
//                    + this);
//        }
//        if (waypoints_map[waypoints_map.length - 1] != links.length - 1) {
//            throw new NetconfigException(null, "Last waypoint wrongly indexed"
//                    + this);
//        }
//        // First waypoint is on start link
//        if (waypoints[0].spot.link != links[0]) {
//            throw new NetconfigException(null,
//                    "First waypoint is not on start link" + this);
//        }
//        // Last waypoint is on end link
//        if (waypoints[waypoints.length - 1].spot.link != links[links.length - 1]) {
//            throw new NetconfigException(null,
//                    "Last waypoint is not on end link" + this);
//        }
//        // same id
//        final String id = waypoints[0].id;
//        for (TSpot<LINK> tsp : waypoints) {
//            if (tsp.id == null && id != null) {
//                throw new NetconfigException(null,
//                        "Different ids in the same trajectory" + this);
//            }
//            if (tsp.id != null && id == null) {
//                throw new NetconfigException(null,
//                        "Different ids in the same trajectory" + this);
//            }
//            if ((tsp.id != null) && !tsp.id.equals(id)) {
//                throw new NetconfigException(null,
//                        "Different ids in the same trajectory" + this);
//            }
//        }
//        // Waypoints in time order
//        // Waypoints in offset order
//        for (int i = 1; i < waypoints.length; ++i) {
//            TSpot<LINK> sp1 = waypoints[i - 1];
//            TSpot<LINK> sp2 = waypoints[i];
//            if (sp1.time.$greater(sp2.time)) {
//                throw new NetconfigException(null,
//                        "Some points are further in the future" + this);
//            }
//            if (sp1.spot.link == sp2.spot.link
//                    && sp1.spot.offset > sp2.spot.offset) {
//                throw new NetconfigException(null,
//                        "Some points go backward in space at index=" + i
//                                + ":\n" + this);
//            }
//        }
//        // links in order
//        for (int i = 1; i < links.length; ++i) {
//            LINK l1 = links[i - 1];
//            LINK l2 = links[i];
//            if (l1.getEndNode() != l2.getStartNode()) {
//                throw new NetconfigException(null, "Links are not aligned"
//                        + this);
//            }
//        }
//        // Waypoints are on the traj links
//        HashSet<LINK> ls = new HashSet<LINK>();
//        for (LINK l : links) {
//            ls.add(l);
//        }
//        for (TSpot<LINK> ts : waypoints) {
//            if (!ls.contains(ts.spot.link)) {
//                throw new NetconfigException(null, "Link/waypoints mismatch"
//                        + ts.spot);
//            }
//        }
//    }
//
//    @Override
//    public String toString() {
//        StringBuilder b = new StringBuilder("Trajectory[");
//        b.append(id).append(",");
//        b.append(getStartTime()).append("->").append(getEndTime()).append("]");
//        b.append("\nlinks: ").append(links[0]);
//        for (int i = 1; i < links.length; ++i) {
//            b.append(", ").append(links[i]);
//        }
//        b.append("\nwaypoints:\n");
//        for (int i = 0; i < waypoints.length; ++i) {
//            b.append("\t(").append(waypoints_map[i]).append(")")
//                    .append(waypoints[i]).append("\n");
//        }
//        return b.toString();
//    }
//
//    public Time getStartTime() {
//        return waypoints[0].time;
//    }
//
//    public Time getEndTime() {
//        return waypoints[waypoints.length - 1].time;
//    }
//
//    /**
//     * 
//     * note: loses info
//     * 
//     * @return a route travel time
//     * @throws NetconfigException
//     */
//    @SuppressWarnings({ "unchecked", "rawtypes" })
//    public RouteTT<LINK> convertToRouteTT() throws NetconfigException {
//        // Check if there is a uniform hired value
//        // TODO(?) should be factored somewhere, already implemented in
//        // path inference
//        Boolean hired = waypoints[0].hired;
//        for (TSpot ts : waypoints) {
//            if (hired != ts.hired
//                    || (hired != null && ts.hired != null && hired
//                            .booleanValue() != ts.hired.booleanValue())) {
//                hired = null;
//                break;
//            }
//        }
//        return new RouteTT(convertToRoute(), getStartTime(), getEndTime(), id,
//                hired);
//    }
//
//    /**
//     * 
//     * note: loses info
//     * 
//     * @return a route travel time
//     * @throws NetconfigException
//     *             TODO(?) wrong hired
//     */
//    @SuppressWarnings({ "unchecked", "rawtypes" })
//    public RouteTT<LINK> convertToRouteTT(int start_wp_index, int end_wp_index)
//            throws NetconfigException {
//        // Check if there is a uniform hired value
//        // TODO(?) should be factored somewhere, already implemented in
//        // path inference
//        return new RouteTT(convertToRoute(start_wp_index, end_wp_index),
//                waypoints[start_wp_index].time, waypoints[end_wp_index].time,
//                id, null);
//    }
//
//    /**
//     * 
//     * @return a route travel time
//     * @throws NetconfigException
//     */
//    @SuppressWarnings("unchecked")
//    public RouteTT<LINK>[] convertToRouteTTs() throws NetconfigException {
//        ArrayList<RouteTT<LINK>> rtts = new ArrayList<RouteTT<LINK>>();
//        for (int i = 0; i < waypoints.length - 1; ++i) {
//            rtts.add(convertToRouteTT(i, i + 1));
//        }
//        return rtts.toArray(new RouteTT[0]);
//    }
//
//    /**
//     * 
//     * note: loses info
//     * 
//     * @return a route object
//     * @throws NetconfigException
//     */
//    @SuppressWarnings({ "unchecked", "rawtypes" })
//    public Route<LINK> convertToRoute() throws NetconfigException {
//        return new Route(links, waypoints[0].spot.offset,
//                waypoints[waypoints.length - 1].spot.offset);
//    }
//
//    /**
//     * Return the portion of the trajectory between two travel times.
//     * 
//     * The portion of the trajectory between two travel times is defined as any
//     * route portion or spot for which the associated start time is greater or
//     * equal to the start time, and for which the end time is leass ro equal to
//     * the endTime.
//     * 
//     * @param startTime
//     * @param endTime
//     * @return a routett, or null if the no point can be found between the start
//     *         time and the end time.
//     * @throws NetconfigException
//     */
//    public RouteTT<LINK> convertToRouteTT(Time startTime, Time endTime)
//            throws NetconfigException {
//        // Check the bounds:
//        if (this.getEndTime().$less(startTime)) {
//            return null;
//        }
//        if (endTime.$less$eq(this.getStartTime())) {
//            return null;
//        }
//        if (endTime.$less(startTime)) {
//            throw new NetconfigException(null, "Wrong start times, end times");
//        }
//        if (endTime.$eq$eq(startTime)) {
//            return null;
//        }
//        // Find the start index:
//        // This is guaranteed to finish
//        int i = 0;
//        while (this.waypoints[i].time.$less(startTime)) {
//            i++;
//        }
//        int j = this.waypoints.length - 1;
//        while (this.waypoints[j].time.$greater(endTime)) {
//            j--;
//        }
//        // We need at least j >= i + 1 to get a chunk
//        if (j >= i + 1) {
//            return convertToRouteTT(i, j);
//        }
//        return null;
//    }
//
//    /**
//     * *includes* endpoints.
//     * 
//     *  TODO(?) doc note: loses info
//     * 
//     * @return a route
//     * @throws NetconfigException
//     */
//    public Route<LINK> convertToRoute(int start_wp_index, int end_wp_index)
//            throws NetconfigException {
//        assert (start_wp_index >= 0);
//        assert (end_wp_index <= waypoints.length - 1);
//        assert (start_wp_index < end_wp_index);
//        // Slicing, anyone?
//        ArrayList<LINK> list_slice = new ArrayList<LINK>();
//        int i = waypoints_map[start_wp_index];
//        while (i <= waypoints_map[end_wp_index]) {
//            list_slice.add(links[i]);
//            i += 1;
//        }
//        @SuppressWarnings("unchecked")
//        LINK[] arr = (LINK[]) list_slice.toArray(new Link[0]);
//        return new Route<LINK>(arr, waypoints[start_wp_index].spot.offset,
//                waypoints[end_wp_index].spot.offset);
//    }
//
//    /**
//     * Describe exact match vs approximate match.
//     * 
//     * @author tjhunter
//     * @param <LINK>
//     */
//    public static class TrajectoryBuilder<LINK extends Link> {
//
//        /**
//         * Thrown specifically when the current path ends. The builder is left
//         * in a state from which a path can safely be created. (May not be the
//         * case when another exception occurs)
//         */
//        public static class EndOfPathException extends NetconfigException {
//
//            static final long serialVersionUID = 0L;
//
//            public EndOfPathException() {
//                super(null,
//                        "End of path detected : and you are trying to add more things");
//            }
//
//            public EndOfPathException(String reason) {
//                super(null, reason);
//            }
//        };
//
//        private final ArrayList<LINK> links = new ArrayList<LINK>(20);
//        private final ArrayList<TSpot<LINK>> tspots = new ArrayList<TSpot<LINK>>(
//                20);
//        private final ArrayList<Integer> tspots_map = new ArrayList<Integer>();
//        /**
//         * Mapping-specific settings. Should always be smaller than the minimum
//         * link length. If the distance between the last spot and a new spot is
//         * greater than this value, and end of path exception will be thrown.
//         * 
//         * Set to a negative value to disable. In this case, the new spot will
//         * have to belong to the same link as the last seen spot (topological
//         * check).
//         */
//        private double maxSplitDistance = -1;
//        private double backward_margin = 250.0f; // a ridiculous value
//        // DB specific settings
//        private int start_tspot_idx = 0;
//        private int start_link_idx = 0;
//
//        public TrajectoryBuilder(double max_split_distance) {
//            this.maxSplitDistance = max_split_distance;
//        }
//
//        public TrajectoryBuilder() {
//        }
//
//        public void addRouteTTs(Collection<RouteTT<LINK>> rtts)
//                throws NetconfigException, EndOfPathException {
//            for (RouteTT<LINK> rtt : rtts) {
//                addRouteTT(rtt);
//            }
//        }
//
//        /**
//         * Use this when the paths are not ambiguous.
//         * 
//         * @param p
//         * @throws an
//         *             exception when adding a point failed.
//         */
//        @SuppressWarnings("unchecked")
//        public void addPoint(TSpot<LINK> p) throws NetconfigException,
//                EndOfPathException {
//            // Monitor.debug("Adding a tspot");
//            // Monitor.debug(p);
//            if (tspots.isEmpty()) {
//                assert (links.isEmpty());
//                tspots.add(p);
//                links.add(p.spot.link);
//                tspots_map.add(links.size() - 1);
//                return;
//            }
//            final TSpot<LINK> last_ts = tspots.get(tspots.size() - 1);
//            // // Is it forward in time?
//            // float dt = p.time.$minus(last_ts.time);
//            // if (last_ts.time.$minus(p.time) > maxSplitTT) {
//            // throw new EndOfPathException(
//            // "Too much time between the two TSpots "
//            // + last_ts + " , " + p);
//            // }
//            // Check that the new point is reachable
//            // Same link?
//            if (last_ts.spot.link == p.spot.link) {
//                if (last_ts.spot.offset > backward_margin + p.spot.offset) {
//                    throw new NetconfigException(null,
//                            "Too backward point, should not happen " + last_ts
//                                    + " , " + p);
//                }
//                if (last_ts.spot.offset < p.spot.offset) {
//                    tspots.add(p);
//                    tspots_map.add(links.size() - 1);
//                    return;
//                }
//                return; // othrerwise, drop the spot
//
//            }
//            // Adjacent link?
//            if (last_ts.spot.link.getEndNode() == p.spot.link.getStartNode()) {
//                tspots.add(p);
//                links.add(p.spot.link);
//                tspots_map.add(links.size() - 1);
//                return;
//            }
//            // Try even harder: adjacent nodes?
//            for (Link l1 : last_ts.spot.link.getOutLinks()) {
//                for (Link l2 : p.spot.link.getInLinks()) {
//                    if (l1 == l2) {
//                        links.add((LINK) l1);
//                        tspots.add(p);
//                        links.add(p.spot.link);
//                        tspots_map.add(links.size() - 1);
//                        return; // do not forget to return here
//                    }
//                }
//            }
//            // Not adjacent, failure
//            throw new EndOfPathException(
//                    "These spots are not related. You are trying to create "
//                            + "an adultery, which you should be ashamed of. "
//                            + last_ts + " , " + p);
//        }
//
//        public void addRouteTT(RouteTT<LINK> rtt) throws NetconfigException,
//                EndOfPathException {
//            if (tspots.isEmpty()) {
//                assert (links.isEmpty());
//                // Add start and end, and all the links
//                // WARNING: loss of information!
//                tspots.add(new TSpot<LINK>(rtt.route.getFirstSpot(), rtt.id,
//                        rtt.startTime, rtt.hired, null));
//                tspots_map.add(0);
//                tspots.add(new TSpot<LINK>(rtt.route.getLastSpot(), rtt.id,
//                        rtt.endTime, rtt.hired, null));
//                for (LINK l : rtt.route.getLinks()) {
//                    links.add(l);
//                }
//                tspots_map.add(links.size() - 1);
//                return;
//            }
//            final TSpot<LINK> last_ts = tspots.get(tspots.size() - 1);
//            final Spot<LINK> last_sp = last_ts.spot;
//            final Spot<LINK> first_rtt_sp = rtt.route.getFirstSpot();
//            final Spot<LINK> last_rtt_sp = rtt.route.getLastSpot();
//            LINK[] r_links = rtt.route.getLinks();
//
//            if (maxSplitDistance > 0.0) {
//                // Ugly code
//                // TODO(?) TODO(?) remove this workaround
//                Coordinate c1 = last_sp.toCoordinate();
//                Coordinate c2 = first_rtt_sp.toCoordinate();
//                // TODO(?) this is obviously wrong...
//                double dx = c1.srid == 4326 ? c1.distanceVincentyInMeters(c2)
//                        : c1.distanceCartesianInMeters(c2);
//                if (dx > maxSplitDistance) {
//                    throw new EndOfPathException(
//                            "Too much distance between the two RTTs " + last_ts
//                                    + " , " + rtt);
//                }
//            }
//
//            // They do not belong to the same link.
//            // But still are close enough.
//            if (last_sp.link != first_rtt_sp.link) {
//                // First case: some progress forward is made:
//                if (last_sp.link.getEndNode() == first_rtt_sp.link
//                        .getStartNode()) {
//                    // Add the first link
//                    links.add(r_links[0]);
//                    // Add the first spot
//                    tspots_map.add(links.size() - 1);
//                    tspots.add(new TSpot<LINK>(first_rtt_sp, rtt.id,
//                            rtt.startTime, rtt.hired, null));
//                    // Add all the other links
//                    for (int i = 1; i < r_links.length; ++i) {
//                        links.add(r_links[i]);
//                    }
//                    // Add the last spot
//                    tspots_map.add(links.size() - 1);
//                    tspots.add(new TSpot<LINK>(last_rtt_sp, rtt.id,
//                            rtt.endTime, rtt.hired, null));
//
//                    return;
//                }
//                // Special case (which happens)
//                // The new tspot is slightly before the last tspot
//                // (i.e. on the links before because the last tspot is at the
//                // head of the link)
//                if (rtt.route.links_.length == 2
//                        && last_sp.link == rtt.route.links_[1]) {
//                    // Make sure the start tspot was on the same track as the
//                    // trajectory
//                    // The new spot has to be on the second to last link
//                    if (links.size() < 2
//                            || first_rtt_sp.link != links.get(links.size() - 2)) {
//                        throw new EndOfPathException(
//                                "The two RTTs are backward - disconnected"
//                                        + last_ts + " , " + rtt);
//
//                    }
//
//                    // If the last spot is after, we add it
//                    if (last_sp.offset < last_rtt_sp.offset) {
//                        // Add the last spot
//                        tspots_map.add(links.size() - 1);
//                        tspots.add(new TSpot<LINK>(last_rtt_sp, rtt.id,
//                                rtt.endTime, rtt.hired, null));
//                    }
//
//                    return;
//                }
//
//                // The elements are disconnected
//                // Throw an exception
//                throw new EndOfPathException("The two RTTs are disconnected"
//                        + last_ts + " , " + rtt);
//            } else {
//                // We are on the same link
//
//                // Is the offset forward?
//                // Add the first spot
//                if (last_sp.offset < first_rtt_sp.offset) {
//                    tspots_map.add(links.size() - 1);
//                    tspots.add(new TSpot<LINK>(first_rtt_sp, rtt.id,
//                            rtt.startTime, rtt.hired, null));
//
//                }
//
//                // Add all but the first link
//                for (int i = 1; i < r_links.length; ++i) {
//                    links.add(r_links[i]);
//                }
//
//                // Add the last rtt spot
//                // Do not forget the case in which it is completely before
//                if (r_links.length > 1 || last_sp.offset < last_rtt_sp.offset) {
//                    tspots_map.add(links.size() - 1);
//                    tspots.add(new TSpot<LINK>(last_rtt_sp, rtt.id,
//                            rtt.endTime, rtt.hired, null));
//                }
//
//            }
//
//        }
//
//        /**
//         * Add a database row.
//         * 
//         * @param p
//         * @param link_idx
//         * @param l
//         * @throws NetconfigException
//         *             when the input does not conform to the DB invariants.
//         * @throws EndOfPathException
//         *             when the end of a path is detected and the new point
//         *             cannot be added without breaking the trajectory
//         *             invariants.
//         */
//        public void addRow(TSpot<LINK> p, int spot_idx, LINK l, int link_idx,
//                Time start_time) throws NetconfigException, EndOfPathException {
//            /**
//             * Make sure that we can start from anywhere in a trajectory and
//             * still correctly reconstruct it.
//             */
//            if (tspots.isEmpty()) {
//                assert (links.isEmpty());
//                assert (tspots_map.isEmpty());
//                start_tspot_idx = spot_idx;
//                start_link_idx = link_idx;
//            }
//            if (!(tspots.isEmpty() || tspots.get(0).id.equals(p.id))) {
//                throw new EndOfPathException("Driver switch");
//            }
//            if (!(tspots.isEmpty() || tspots.get(0).time.$eq$eq(start_time))) {
//                throw new EndOfPathException("Change of start time");
//            }
//            int relative_spot_idx = spot_idx - start_tspot_idx;
//            int relative_link_idx = link_idx - start_link_idx;
//            // Monitor.debug(relative_spot_idx+"("+spot_idx+"):"+p);
//            // Monitor.debug(relative_link_idx+"("+link_idx+"):"+l);
//            if (tspots.size() > relative_spot_idx + 1) {
//                throw new NetconfigException(null, "Wrong spot index received"
//                        + relative_spot_idx + "," + spot_idx + "<"
//                        + tspots.size());
//            }
//            if (tspots.size() < relative_spot_idx) {
//                throw new EndOfPathException("Missing spot in the sequence");
//            }
//            // This is a programming error, should not happen
//            // Throws an exception instead
//            if (relative_link_idx > links.size() + 1) {
//                throw new NetconfigException(null,
//                        "Wrong link index insequence");
//            }
//            if (links.size() > relative_link_idx + 1) {
//                throw new NetconfigException(null, "Wrong link index");
//            }
//            // The logic is actually simple
//            // New link, same root spot
//            if (relative_link_idx == links.size()) {
//                // Monitor.debug("Addiing link "+l);
//                links.add(l);
//            }
//            // New spot, same link
//            if (relative_spot_idx == tspots.size()) {
//                // Monitor.debug("Addiing point "+p);
//                tspots.add(p);
//                tspots_map.add(links.size() - 1);
//            }
//        }
//
//        public void reset() {
//            links.clear();
//            tspots.clear();
//            tspots_map.clear();
//        }
//
//        public boolean isEmpty() {
//            return tspots.isEmpty();
//        }
//
//        @SuppressWarnings("unchecked")
//        public Trajectory<LINK> createTrajectory() throws NetconfigException {
//            // Monitor.debug("Create trajectory");
//            if (tspots.size() < 2) {
//                throw new NetconfigException(null,
//                        "Not enough points in the builder");
//            }
//            // Autoboxing not smart enough?
//            int[] idx_map = new int[tspots_map.size()];
//            for (int i = 0; i < tspots_map.size(); ++i) {
//                idx_map[i] = tspots_map.get(i);
//            }
//            // Somthing ugly like this should work
//            return new Trajectory<LINK>(
//                    (TSpot<LINK>[]) tspots.toArray(new TSpot[0]),
//                    (LINK[]) links.toArray(new Link[0]), idx_map); // TODO(?) not
//                                                                   // sure if
//                                                                   // this is
//                                                                   // the
//                                                                   // correct
//                                                                   // way to
//                                                                   // convert to
//                                                                   // an array
//        }
//    }
//}
