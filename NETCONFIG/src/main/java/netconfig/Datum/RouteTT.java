package netconfig.Datum;

import com.google.common.collect.ImmutableList;

import netconfig.Link;
import netconfig.NetconfigException;
import netconfig.Route;
import core.Time;

/**
 * Container class for a {@link Route} and a travel time. If startTime is after
 * endTime then the TT is negative. Possible to only have a TT and no start or
 * end time.
 * 
 * @param <LINK>
 *            b/c a Route is defined this way.
 * @author tjhunter
 */
public class RouteTT<LINK extends Link> {

    /** Route that the travel time is on. (never null). */
    public final Route<LINK> route;
    /** Start time, never null. */
    public final Time startTime;
    /** End time, never null. More or equal than startTime. */
    public final Time endTime;
    /** Travel time for this data record, non negative. */
    public final float tt;
    /**
     * Can be null. If not null, this is the id of the probe vehicle that
     * generated this record.
     */
    public final String id;
    /**
     * Indicate wether the vehicle was hired between two points. If it null, the
     * hired status is not available, or it is changing from between the start
     * and end point.
     */
    public final Boolean hired;

    /**
     * Constructor taking start/end and calculating TT.
     * 
     * @param route
     *            Route< LINK > this TT is defined for.
     * @param startTime
     *            time starting on the route
     * @param endTime
     *            time leaving the route
     * @param id
     *            Can be null. If not null, this is the id of the probe vehicle
     *            that generated this record.
     * @see Route
     * @throws NetconfigException
     *             containing an IllegalArgumentException if any argument is
     *             null.
     */
    private RouteTT(Route<LINK> route, Time startTime, Time endTime, String id,
            Boolean hired) throws NetconfigException {
        this.route = route;
        this.startTime = startTime;
        this.endTime = endTime;
        this.tt = endTime.secondsSince(startTime);
        this.id = id;
        this.hired = hired;
    }

    public double averageSpeedOnRoute() {
        return this.route.length() / this.tt;
    }

    /**
     * Converts this route travel time to the equivalent path inference object.
     * 
     * @return A path inference object representing this route travel time
     */
    public PathInference<LINK> toPathInference() throws NetconfigException {
        ImmutableList<Route<LINK>> routes = ImmutableList.of(route);
        return PathInference.from(id, startTime, endTime, routes, null, null);
    }

    public String toString() {
        return String.format(
                "RouteTT[id = %s, start time = %s, end time = %s, route = %s]",
                id, startTime.toString(), endTime.toString(), route.toString());
    }

    // ************** PUBLIC CONSTRUCTORS ***************

    public static <LINK extends Link> RouteTT<LINK> from(Route<LINK> route,
            Time startTime, Time endTime, String id, Boolean hired)
            throws NetconfigException {
        if (null == route) {
            throw new NetconfigException(new IllegalArgumentException(
                    "Route can't be null."), null);
        }
        if (null == startTime) {
            throw new NetconfigException(new IllegalArgumentException(
                    "Start time can't be null with this constructor."), null);
        }
        if (null == endTime) {
            throw new NetconfigException(new IllegalArgumentException(
                    "End time can't be null with this constructor."), null);
        }
        return new RouteTT<LINK>(route, startTime, endTime, id, hired);
    }

    public static <LINK extends Link> RouteTT<LINK> from(Route<LINK> route,
            Time startTime, Time endTime, String id) throws NetconfigException {
        return from(route, startTime, endTime, id, null);
    }

    // public RouteTT(Route<LINK> route, Time startTime, Time endTime, String
    // id)
    // throws NetconfigException {
    // this(route, startTime, endTime, id, null);
    // }

    // /**
    // * Constructor taking start/end and calculating TT.
    // *
    // * @param route
    // * Route< LINK > this TT is defined for.
    // * @param startTime
    // * time starting on the route
    // * @param endTime
    // * time leaving the route
    // * @see Route
    // * @throws NetconfigException
    // * containing an IllegalArgumentException if any argument is
    // * null.
    // */
    // public RouteTT(Route<LINK> route, Time startTime, Time endTime)
    // throws NetconfigException {
    // this(route, startTime, endTime, null);
    // }

    // /**
    // * Constructor taking only a TT, start and end Time set to null.
    // *
    // * @param route
    // * Route< LINK > this TT is defined for.
    // * @param tt
    // * Travel time (can be negative, infinity, or NaN).
    // * @see Route
    // * @throws NetconfigException
    // * containing an IllegalArgumentException if route is null.
    // */
    // public RouteTT(Route<LINK> route, float tt) throws NetconfigException {
    // if (null == route) {
    // throw new NetconfigException(new IllegalArgumentException(
    // "Route can't be null."), null);
    // }
    // this.route = route;
    // this.startTime = null;
    // this.endTime = null;
    // this.tt = tt;
    // this.id = null;
    // this.hired = null;
    // }
} // RouteTT
