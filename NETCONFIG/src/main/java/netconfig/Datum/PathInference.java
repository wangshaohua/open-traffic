package netconfig.Datum;

import java.io.Serializable;

import netconfig.Link;
import netconfig.NetconfigException;
import netconfig.Route;
import bots_math.ImmutableTensor1;
import core.Time;
import com.google.common.collect.ImmutableList;

/**
 * This class represents possible paths between two GPS locations and the
 * probability associated with each path, it is primarily returned from the path
 * inference code, hence the name.
 * <p>
 * The probability array is <u>always</u> the same length as routes and defaults
 * to all -1~s, which causes the hasValidProbabilites function to return false
 * without having to transverse the array.
 * <p/>
 * The probabilities are set to an invalid value. (Which can be changed
 * individually, but the whole field is final so the length (or the Array class
 * it points to) cannot.)
 * <p/>
 * Link is needed because Route takes it as a parameter.
 * 
 * @param <LINK>
 *            the type of link used.
 * @author tjhunter
 */
public class PathInference<LINK extends Link> implements Serializable {

    private static final long serialVersionUID = 1L;
    /** Driver ID, cannot be null. */
    private final String id_;
    /** Start time, cannot be null. */
    private final Time startTime_;
    /** End time, cannot be null. */
    private final Time endTime_;
    /**
     * The array of route (same length as the probabilities. The path inference
     * filter has no way to know the exact subtype of this and only operates on
     * the Link superclass, so this whole class may need to be casted to the
     * right thing.
     */
    private final ImmutableList<Route<LINK>> routes_;
    /**
     * Probability of each route.
     * <p/>
     * This array is the same length as the routes array and each index
     * corresponds to the route with the same index in the routes array.
     * <p/>
     * Use {@link #hasValidProbabilities(double)} to see if it contains a valid
     * distribution.
     */
    private final ImmutableTensor1 probabilities_;
    /** Hired status. null for N/A or unknown. */
    private final Boolean hired_;

    private PathInference(String id, Time startTime, Time endTime,
            ImmutableList<Route<LINK>> routes, ImmutableTensor1 probabilities,
            Boolean hired) {
        this.probabilities_ = probabilities;
        this.id_ = id;
        this.startTime_ = startTime;
        this.endTime_ = endTime;
        this.routes_ = routes;
        this.hired_ = hired;
    }

    /**
     * Helper function in case one wants both the most probable route travel
     * time and the corresponding probability, see
     * {@link #getMostProbableRouteTT()} for a list of caveats and the ordering.
     * 
     * @return the index of the most probable route travel time
     *         probability/probability, or null if there are no spots.
     * @see #getMostProbableRouteTT()
     * @see #sortByDescendingProbabilities()
     */
    public Integer getMostProbableIndex() {
        double lastMax = Double.NEGATIVE_INFINITY;
        Integer ret = null;
        // Won't loop at all if length == 0
        for (int i = 0; i < this.routes().size(); i++) {
            // Use compare() b/c the operators don't handle some edge cases,
            // namely NaN, +/- Inf, and -0.0.
            if (0 < Double.compare(this.probabilities().get(i), lastMax)) {
                lastMax = this.probabilities().get(i);
                ret = i;
            }
        }
        return ret;
    }

    /**
     * Returns a copy of this probe coordinate object with a new set of
     * projections and a net set of probabilities.
     * 
     * It does not modify the original object.
     * 
     * @param new_spots
     *            the new spots. They can have a different size from the
     *            original spot distribution.
     * @return
     * @throws NetconfigException
     */
    public <LINK2 extends Link> PathInference<LINK2> clone(
            ImmutableList<Route<LINK2>> new_routes,
            ImmutableTensor1 new_probabilities) {
        return new PathInference<LINK2>(id(), startTime(), endTime(), new_routes,
                new_probabilities, hired());
    }

    public <LINK2 extends Link> PathInference<LINK2> clone(
            ImmutableList<Route<LINK2>> new_routes, double[] new_probabilities)
            throws NetconfigException {
        ImmutableTensor1 new_probs = buildProbabilities(new_probabilities);
        return new PathInference<LINK2>(id(), startTime(), endTime(), new_routes,
                new_probs, hired());
    }

    public PathInference<LINK> clone(double[] new_probabilities)
            throws NetconfigException {
        ImmutableTensor1 new_probs = buildProbabilities(new_probabilities);
        return new PathInference<LINK>(id(), startTime(), endTime(), routes(),
                new_probs, hired());
    }

    /** Creates a duplicate of this object (not implemented). */
    public PathInference<LINK> clone() {
        throw new java.lang.UnsupportedOperationException(
                "Function not implemented!");
    }

    /**
     * Get the most probable route (or null) as a RouteTT without checking if
     * the probabilities array is valid and does not do any reordering. If you
     * are going to call this more than once, it may be better to call
     * {@link #sortByDescendingProbabilities()} and refer to the zeroth element
     * of the routes array thereafter.
     * <p/>
     * Sorting of the probabilities is like:<br/>
     * -Inf &lt; .get(negVal) &lt; -0 &lt; 0 &lt; .get(posVal) &lt; Inf &lt; NaN
     * 
     * @return the one most likely route with it's TT or null if the arrays are
     *         0-length.
     * @throws NetconfigException
     *             if the RouteTT can't be instantiated.
     */
    public RouteTT<LINK> getMostProbableRouteTT() throws NetconfigException {
        Route<LINK> ret = null;
        double lastMax = Double.NEGATIVE_INFINITY;
        for (int i = 0; i < this.routes().size(); i++) {
            // Use compare() b/c the operators don't handle some edge cases.
            if (0 < Double.compare(this.probabilities().get(i), lastMax)) {
                lastMax = this.probabilities().get(i);
                ret = this.routes().get(i);
            }
        }
        if (null == ret) {
            return null;
        } else {
            return RouteTT.from(ret, this.startTime(), this.endTime(), this.id(),
                    this.hired());
        }
    }

    /** Convenience function, returns the TT. */
    public float getTT() {
        return this.endTime().secondsSince(this.startTime());
    }

    /**
     * Attempts to convert a path inference object to a logically equivalent
     * route travel time object.
     * 
     * @return a route travel time
     * @throws NetconfigException
     *             if this object does not have a single, non-null path.
     */
    public RouteTT<LINK> toRouteTT() throws NetconfigException {
        if (this.routes().size() != 1 || this.routes().get(0) == null) {
            throw new NetconfigException(null, "Cannot convert to route TT");
        }
        return RouteTT.from(this.routes().get(0), this.startTime(), this.endTime(),
                this.id(), this.hired());
    }

    /**
     * @return a string representation of this PathInference instance.
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("%s from %s to %s .get(\n", this.id(),
                this.startTime().toString(), this.endTime().toString()));
        for (int i = 0; i < this.routes().size(); i++) {
            sb.append(String.format("\t\t%f\t%s\n)", this.probabilities().get(i),
                    this.routes().get(i).toString()));
        }
        return sb.toString();
    } // toString.

    // /**
    // * Invalidates all probabilities in a way that
    // * {@link #hasValidProbabilities(double)} will return almost immediately.
    // * <p/>
    // * It's final so it can be used in the constructor.
    // */
    // public final void fillProbabilitiesWithInvalidValues() {
    // // Cannot use a foreach b/c we want to change the values.
    // for (int i = 0; i < this.probabilities.length; i++) {
    // this.probabilities.get(i) = -1;
    // }
    // }
    //
    // /**
    // * Fills probabilities with a uniform distribution, which will make
    // * {@link #hasValidProbabilities(double)} return true with a reasonable
    // * epsilon.
    // */
    // public void fillProbabilitiesWithUniformDistribution() {
    // // If length is 0 then factor will be Infinity and
    // // the loop won't ever execute its body.
    // double factor = 1d / (double) this.probabilities.length;
    // for (int i = 0; i < this.probabilities.length; i++) {
    // this.probabilities.get(i) = factor;
    // }
    // }

    // ************** PUBLIC CONSTRUCTORS *************

    /**
     * Full constructor. Length of routes and probabilities must be the same and
     * greater than 0.
     * 
     * @param id
     *            Driver ID
     * @param startTime
     * @param endTime
     * @param routes
     * @param probabilities
     *            if null it is set to invalid probabilities.
     * @param hired
     * @throws NetconfigException
     *             containing an IllegalArgumentException on any error.
     */
    public static <LINK extends Link> PathInference<LINK> from(String id,
            Time startTime, Time endTime, Route<LINK>[] routes,
            double[] probabilities, Boolean hired) throws NetconfigException {
        ImmutableList<Route<LINK>> routes0 = ImmutableList.copyOf(routes);
        return PathInference.from(id, startTime, endTime, routes0,
                probabilities, hired);
    }

    /**
     * Full constructor. Length of routes and probabilities must be the same and
     * greater than 0.
     * 
     * @param id
     *            Driver ID
     * @param startTime
     * @param endTime
     * @param routes
     * @param probabilities
     *            if null it is set to invalid probabilities.
     * @param hired
     * @throws NetconfigException
     *             containing an IllegalArgumentException on any error.
     */
    public static <LINK extends Link> PathInference<LINK> from(String id,
            Time startTime, Time endTime, ImmutableList<Route<LINK>> routes,
            double[] probabilities, Boolean hired) throws NetconfigException {
        if (null == id) {
            throw new NetconfigException(new IllegalArgumentException(
                    "Driver ID can't be null."), null);
        }
        if (null == startTime) {
            throw new NetconfigException(new IllegalArgumentException(
                    "Start time can't be null."), null);
        }
        if (null == endTime) {
            throw new NetconfigException(new IllegalArgumentException(
                    "End time can't be null."), null);
        }
        if (null == routes) {
            throw new NetconfigException(new IllegalArgumentException(
                    "Routes can't be null."), null);
        }
        if (routes.isEmpty()) {
            throw new NetconfigException(new IllegalArgumentException(
                    "Routes can't be empty."), null);
        }
        boolean any_null = false;
        for (Route<LINK> r : routes) {
            if (r == null) {
                any_null = true;
                break;
            }
        }
        if (any_null) {
            throw new NetconfigException(null, "Routes cannot be null.");
        }
        ImmutableTensor1 probs = null;
        if (probabilities == null) {
            probs = ImmutableTensor1.fillWith(1.0 / routes.size(),
                    routes.size());
        } else {
            if (routes.size() != probabilities.length) {
                throw new NetconfigException(new IllegalArgumentException(
                        "Routes and probabilities have different length"), null);
            }
            probs = ImmutableTensor1.from(probabilities);
        }
        if (!hasValidProbabilities(probs, 1e-5)) {
            throw new NetconfigException(new IllegalArgumentException(
                    "Invalid probability vector"), null);
        }
        return new PathInference<LINK>(id, startTime, endTime, routes, probs,
                hired);
    }

    /**
     * Constructor that fills the probabilities with invalid values. Just calls
     * the Full constructor with probabilities set to null.
     * 
     * @param id
     *            Driver ID.
     * @param startTime
     * @param endTime
     * @param routes
     * @throws NetconfigException
     */
    public static <LINK extends Link> PathInference<LINK> from(String id,
            Time startTime, Time endTime, Route<LINK>[] routes)
            throws NetconfigException {
        return from(id, startTime, endTime, routes, null, null);
    }

    public static ImmutableTensor1 buildProbabilities(double[] vals)
            throws NetconfigException {
        if (vals == null) {
            throw new NetconfigException(new IllegalArgumentException(
                    "null probability vector"), null);
        }
        ImmutableTensor1 probs = ImmutableTensor1.from(vals);
        if (!hasValidProbabilities(probs, 1e-5)) {
            throw new NetconfigException(new IllegalArgumentException(
                    "Invalid probability vector"), null);
        }
        return probs;
    }

    /**
     * Checks if <u>all</u> probabilities are valid and sum to 1.
     * 
     * @param epsilon
     *            the acceptable error when comparing to 1.
     * @return true if the elements of the probabilities array satisfy:
     *         <ul>
     *         <li>Each element value is between 0 and 1 inclusive.</li>
     *         <li>The sum of all the elements (this disallows NaN or infinity
     *         for any element) is equal to 1 plus or minus the passed in
     *         epsilon.</li>
     *         <li>The arrays are greater than 0-length.</li>
     *         </ul>
     *         and false otherwise (will return false if the array is 0-length
     *         or any element is NaN or infinity (but -0d is treated the same as
     *         0d).
     */
    public static boolean hasValidProbabilities(ImmutableTensor1 probabilities,
            double epsilon) {
        if (probabilities.size() == 0) {
            return true;
        }
        double total = 0d;
        for (double d : probabilities) {
            if (d < 0 // -0.0d will be false here.
                    || d > 1 // Will be false on either infinity.
                    || Double.isNaN(d)) {
                return false;
            }
            total += d;
        }
        // total is positive (or we wouldn't have gotten here)
        // epsilon could be negative, so we check the described way.
        if (total <= 1 + epsilon && total >= 1 - epsilon) {
            return true;
        } else {
            return false;
        }
    }

	/**
	 * @return the id_
	 */
	public String id() {
		return id_;
	}

	/**
	 * @return the startTime_
	 */
	public Time startTime() {
		return startTime_;
	}

	/**
	 * @return the endTime_
	 */
	public Time endTime() {
		return endTime_;
	}

	/**
	 * @return the routes_
	 */
	public ImmutableList<Route<LINK>> routes() {
		return routes_;
	}

	/**
	 * @return the probabilities_
	 */
	public ImmutableTensor1 probabilities() {
		return probabilities_;
	}

	/**
	 * @return the hired_
	 */
	public Boolean hired() {
		return hired_;
	}

} // PathInference
