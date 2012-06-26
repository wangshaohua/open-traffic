/**
 * Copyright 2008. The Regents of the University of California (Regents).
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy
 * of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software 
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
/* Geodesy by Mike Gavaghan
 *
 * http://www.gavaghan.org/blog/free-source-code/geodesy-library-vincentys-formula/
 *
 * This code may be freely used and modified on any personal or professional
 * project.  It comes with no warranty.
 */
package core;

import org.apache.commons.math.util.FastMath;
import java.io.Serializable;

/**
 * Java class which should be used to represent both PostGIS and PostgreSQL
 * points. It is convert-able (via the database classes) to both PostgreSQL
 * points and PostGIS points in the database, controlled by the
 * <code>srid</code> field. If this field is <code>null</code> this is
 * considered to be a PostgreSQL point, otherwise it is considered to be a
 * PostGIS point.
 * <p/>
 * For planar systems (like UTM) the field <code>lat</code> corresponds to
 * <code>y</code> and <code>lon</code> corresponds to <code>x</code>.
 * 
 * Implemented SRIDs:
 * 
 * - 4326 (WGS84) - 0 (Cartesian)
 * 
 * See http://infocenter.sybase.com/help/index.jsp?topic=/com.sybase.help.
 * sqlanywhere.12.0.1/dbspatial/spatial-reference-identifier.html for more
 * details about cartesian coordinates.
 * 
 * 
 * TODO(?) have a function that returns the most reasonable distance based on
 * the provided SRIDs: some codes manipulate Coordinate objects with several
 * SRIDs.
 * 
 * 
 * @author Saneesh Apte
 * @author tjhunter
 */
public class Coordinate implements Comparable<Coordinate>, Serializable {

    public static final long serialVersionUID = 3L;
    /**
     * Maximum deviation when computing the distance between two coordinates by
     * using different distance methods.
     * 
     * It has been tested for distanceVincentyInMeters and distanceSpheroid. In
     * practice, this value is close to 10e-8.
     */
    public static final double DISTANCE_PRECISION = 1e-4;
    /** SRID of the cartesian system. */
    public static final int SRID_CARTESIAN = 0;
    public static final int SRID_WGS84 = 4326;
    /**
     * TODO(?) detail what an SRID is.
     * These three are final because if they need to be modified, then a
     * complicated conversion needs to be done (if you don't think so, then you
     * are almost certainly not doing something right). If that is still what
     * you want, then create a new instance.
     * <p/>
     * <code>y</code> in planar systems (like UTM).
     */
    public final Integer srid;
    /**
     * These three are final because if they need to be modified, then a
     * complicated conversion needs to be done (if you don't think so, then you
     * are almost certainly not doing something right). If that is still what
     * you want, then create a new instance.
     * <p/>
     * <code>y</code> in planar systems (like UTM).
     */
    public final double lat;
    /**
     * These three are final because if they need to be modified, then a
     * complicated conversion needs to be done (if you don't think so, then you
     * are almost certainly not doing something right). If that is still what
     * you want, then create a new instance.
     * <p/>
     * <code>x</code> in planar systems (like UTM).
     */
    public final double lon;

    /**
     * Basic constructor, everything is immutable.
     * 
     * @param srid
     *            <code>null</code> for PostgreSQL point, number for a PostGIS
     *            point.
     * @param lat
     *            latitude (or <code>y</code> in planar systems).
     * @param lon
     *            longitude (or <code>x</code> in planar systems).
     */
    public Coordinate(Integer srid, double lat, double lon) {
        this.srid = srid;
        this.lat = lat;
        this.lon = lon;
    }

    /**
     * This function is here just because this comparison has to be done, a lot,
     * in this class and it is very easy to forget or screw up the checking
     * since either or both SRID~s can be null and with the dual meaning of ==
     * and then having to use .equals() (and otherCoord can be null).
     * 
     * @param otherCoord
     *            to compare the SRID from.
     * @return true if SRID~s null or same value, false otherwise.
     */
    public boolean equalsSRID(Coordinate otherCoord) {
        // Instance ``this'' cannot be null (but its SRID can be).
        if (null == otherCoord) {
            return false;
        }
        // Now we know that this and otherCoord are objects with SRID fields.
        // If both SRID~s are null or both refer to the same object, then true.
        if (this.srid == otherCoord.srid) {
            return true;
        }
        // At least one of the SRID~s are not null (b/c we would have returned
        // if both are null), but one could still be null.
        if (null == this.srid) {
            return false;
        }
        // OK so this.srid is not null and therefore has an .equals() method.
        // Finally the real compare (why does Java make this so hard?).
        // Method .equals() returns false if otherCoord.srid is null.
        return this.srid.equals(otherCoord.srid);
    }

    /**
     * Computes the "direction" of travel between two coordinates.
     * If the starting latitude is less than the ending latitude then
     * the direction is "true" and if greater than, the direction is "false". If
     * the latitudes are exactly equal then the direction is true if the
     * starting longitude is less than the ending longitude
     * 
     * @param other
     *            The "end" point, using this object as the starting point to
     *            compute the direction from
     * @return The direction of travel between the two coordinates
     */
    public boolean isFromRefNodeComparedToOtherCoordinate(Coordinate other) {
        if (this.lat < other.lat) {
            return true;
        }
        if (this.lat > other.lat) {
            return false;
        }
        if (this.lon <= other.lon) {
            return true;
        }
        return false;
    }

    /**
     * Computes precise value comparison of two Coordinates (&epsilon; = 0), for
     * &epsilon; comparisons see {@link #equalsEpsilon(core.Coordinate)}.
     * 
     * @param o
     *            the Object to compare to.
     * @return <code>true</code> if these are equivalent (all fields match),
     *         <code>false</code> otherwise.
     * @see #equalsEpsilon(core.Coordinate)
     */
    @Override
    public boolean equals(Object o) {
        // Unlikely (very) to have two refs to the same instance,
        // so no need for the optimization of this == o.

        // Operator instanceof will return false if o is null.
        if (!(o instanceof Coordinate)) {
            return false;
        }

        Coordinate that = (Coordinate) o;

        // .equalsSRID returns false if that is null or not both SRID~s are.
        if (this.equalsSRID(that) && this.lat == that.lat
                && this.lon == that.lon) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * Equals which returns an &epsilon; value so client code can choose its own
     * &epsilon; and one does not need to be hard-coded in this class. A typical
     * use-case would be something like:
     * <p/>
     * <code>
     * double epsilon = .0005d;<br/>
     * if( 0 &gt; Double.compare( abs( coord.equalsEpsilon( otherCoord ) ),
     * epsilon ) )<br/>
     * &nbsp;&nbsp;// Considered equal.<br/>
     * else<br/>
     * &nbsp;&nbsp;// Considered not equal.<br/>
     * </code>
     * <p/>
     * <b>NOTE</b>: {@link #distanceCartesianInSRUnits(core.Coordinate)} is used
     * in this function, but other distance functions can be used if more
     * precise orderings are needed.
     * <p/>
     * {@link #compareTo(core.Coordinate)} returns a -1, 0, or 1, so this
     * function returns that times the distance:<br/>
     * <code>
     * {@link #distanceCartesianInSRUnits(core.Coordinate)} *
     * {@link #compareTo(core.Coordinate)}
     * </code>
     * 
     * @param otherCoord
     *            other coordinate to compare to.
     * @return an &epsilon; value of how &ldquo;far&rdquo; these coordinates are
     *         from each other.
     * @throws ClassCastException
     *             if SRID~s don't match or <code>otherCoord</code> is
     *             <code>null</code>.
     */
    public double equalsEpsilon(Coordinate otherCoord) {
        // .distanceCartesianInSRUnits() will throw the Exception and check if
        // the SRID~s match or if otherCoord is null.
        return this.distanceCartesianInSRUnits(otherCoord)
                * this.compareTo(otherCoord);
    }

    /**
     * Defines ordering for Coordinate objects, SRID then lat then lon.<br/>
     * SRID is sorted with null larger than the integers, and for the lat and
     * lon values, {@link Double#NaN} is bigger than
     * {@link Double#POSITIVE_INFINITY} which is bigger than the integers
     * (including -0.0) with {@link Double#NEGATIVE_INFINITY} as the smallest.
     * <p/>
     * Always returns one of -1, 0, or 1 to work within
     * {@link #equalsEpsilon(core.Coordinate)}.
     * 
     * @param otherCoord
     *            to compare to.
     * @return 0 if <code>srid</code>, <code>lat</code>, and <code>lon</code>
     *         (the combined fields, in that order) are all exactly equal.<br/>
     *         1 if combined fields (of this) are &ldquo;bigger&rdquo;.<br/>
     *         -1 if combined fields (of this) are &ldquo;smaller&rdquo;.
     * @throws ClassCastException
     *             if otherCoord is null
     */
    public int compareTo(Coordinate otherCoord) {
        // Unlikely (very) to have two refs to the same instance,
        // so no need for the optimization of this == otherCoord.

        // Need to sort by SRID first, null is biggest
        if (!this.equalsSRID(otherCoord)) {
            // Only need to sort by SRID (unless otherCoord is null).
            if (null == otherCoord) {
                throw new ClassCastException("Arg otherCoord is null.");
            }
            if (null == this.srid) {
                return 1;
            } else if (null == otherCoord.srid) {
                return -1;
            }
            // Use .compareTo() in Integer.
            if (0 < this.srid.compareTo(otherCoord.srid)) {
                return 1;
            } else {
                return -1;
            }
        }
        // SRID the same, compare lat

        // otherCoord is gaurenteed to not be null by the above call
        // to .equalsSRID().

        // Need to use Double.compare() to deal with edge values (NaN and Inf).
        int cmpi = Double.compare(this.lat, otherCoord.lat); // otherCoord can't
                                                             // be null.

        if (0 < cmpi) {
            return 1;
        }
        if (0 > cmpi) {
            return -1;
        }

        // OK, now lat
        cmpi = Double.compare(this.lon, otherCoord.lon);

        if (0 < cmpi) {
            return 1;
        }
        if (0 > cmpi) {
            return -1;
        }

        // Finally we can say they are the same.
        return 0;
    }

    /**
     * Does the normal thing.
     * 
     * @return a hash.
     */
    @Override
    public int hashCode() {
        int hash = 3;
        hash = 17 * hash + (null == this.srid ? 0 : this.srid.hashCode());
        hash = 17
                * hash
                + (int) (Double.doubleToLongBits(this.lat) ^ (Double
                        .doubleToLongBits(this.lat) >>> 32));
        hash = 17
                * hash
                + (int) (Double.doubleToLongBits(this.lon) ^ (Double
                        .doubleToLongBits(this.lon) >>> 32));
        return hash;
    }

    /**
     * This is the format: [srid](lat,lon).
     * 
     * @return a String representing this Coordinate.
     */
    @Override
    public String toString() {
        return String
                .format("[%d](%3.8f,%3.8f)", this.srid, this.lat, this.lon);
    }

    /**
     * This calculates the Cartesian distance using whatever units this
     * coordinate is in (degrees, maybe), and <b>DOES NOT TAKE INTO ACCOUNT</b>
     * the ellipsoid. It does, however check that the <code>srid</code>'s of the
     * two coordinates match and throws an ClassCastException if they don't.
     * 
     * @param otherCoord
     *            to get distance to.
     * @return the Cartesian, planer distance in <code>srid</code> units.
     * @throws ClassCastException
     *             if the <code>srid</code>'s don't match, or otherCoord is
     *             <code>null</code>.
     */
    public double distanceCartesianInSRUnits(Coordinate otherCoord) {
        // Has to work if either srid is null or the same object.
        // .equalsSRID returns false if that is null or not both SRID~s are.
        if (this.equalsSRID(otherCoord)) {
            double diffLat = otherCoord.lat - this.lat;
            double diffLon = otherCoord.lon - this.lon;
            return Math.sqrt((diffLat * diffLat) + (diffLon * diffLon));
        } else {
            throw new ClassCastException(String.format(
                    "SRID~s do not match: %s and %s", this.toString(),
                    null == otherCoord ? "(null)" : otherCoord.toString()));
        }
    }

    /**
     * Calculates the distance (in meters) between two coordinates (assumed to
     * be expressed in a Cartesian system of coordinates: SRID 0).
     * 
     * @param otherCoord
     * @return
     */
    public double distanceCartesianInMeters(Coordinate otherCoord) {
        // SRID's must match and not be null.
        // This can't be null, duh, and we don't check if otherCoord is, which
        // means the NullPointerException will be thrown.
        if (null == this.srid || null == otherCoord.srid) {
            throw new ClassCastException(
                    "This distance function uses the spheroid distance, but you "
                            + "are using Coordinates with null SRID~s (from a PostgreSQL "
                            + "point?).  This doesn't really make any sense and you "
                            + "probably want to use .distanceCarteasianInSRUnits( other )"
                            + "instead.");
        } else if (!this.srid.equals(otherCoord.srid)) {
            throw new ClassCastException(
                    "The SRID of otherCoord does't match this one.");
        }
        if (0 != this.srid || 0 != otherCoord.srid) {
            throw new ClassCastException(
                    "Only SRID 0 is supported by this function.");
        }
        return this.distanceCartesianInSRUnits(otherCoord);
    }

    /**
     * Returns the distance (in meters) between two coordinates belonging to the
     * same system of coordinates, and using the default distance method for
     * this system.
     * 
     * @param otherCoord
     * @return
     */
    public double distanceDefaultMethodInMeters(Coordinate otherCoord) {
        if (this.srid == SRID_CARTESIAN) {
            return this.distanceCartesianInMeters(otherCoord);
        }
        if (this.srid == 4326) {
            // return this.distanceHaversineInMeters(otherCoord);
            return this.distanceVincentyInMeters(otherCoord);
        }
        throw new ClassCastException("SRID not supported by this function.");
    }

    /**
     * This will eventually do something more useful than return the length of
     * the smallest link in dca.streets. It will: This function returns the
     * distance between this Coordinate and otherCoord (in meters) grossly and
     * quickly, always approximating to a shorter distance. This function in
     * intended to be used by heuristics that want a helicopter distance and
     * require that this value is never ever be larger than the length returned
     * by the accurate spheroid distance function. Currently only SRID 4326 is
     * supported.
     * 
     * @param otherCoord
     *            to find the distance to.
     * @return in meters a number that is less than or equal to the spheroid
     * @throws ClassCastException
     *             if the <code>srid</code>'s are not 4326.
     */
    public double distanceLessThanOrEqualToSpheroidDistanceInMeters(
            Coordinate otherCoord) {
        if (4326 != this.srid || 4326 != otherCoord.srid) {
            throw new ClassCastException("Only SRID 4326 accepted for now.");
        }
        return 2.02117036255978; // Smallest length in dca.streets.
    }

    // /**
    // * Calls the DB to get a real distance is meters,
    // * unsupported for now.
    // * @param dbr the database reader to use
    // * @param otherCoord
    // * @return the most accurate distance in meters.
    // * Note: in practice for the cases currently supported,
    // distanceVincentyInMeters
    // * is much faster and gives the same precision.
    // */
    // public double distanceSpheroidInMeters(DatabaseReader dbr,
    // Coordinate otherCoord)
    // throws DatabaseException {
    // if (4326 != this.srid || 4326 != otherCoord.srid) {
    // throw new ClassCastException(
    // "Only SRID 4326 accepted for now.");
    // }
    // if (!dbr.psHasPS(psDistanceName)) {
    // dbr.psCreate(psDistanceName, psDistance);
    // }
    // dbr.psSetPostGISPoint(psDistanceName, 1, this);
    // dbr.psSetPostGISPoint(psDistanceName, 2, otherCoord);
    // dbr.psQuery(psDistanceName);
    // if (dbr.psRSNext(psDistanceName)) {
    // double res = dbr.psRSGetDouble(psDistanceName, "spheroid_dist");
    // dbr.psRSDestroy(psDistanceName);
    // return res;
    // } else {
    // throw new DatabaseException(null,
    // "Could not get result for spheroid calculation", dbr, psDistanceName);
    // }
    // }

    /**
     * This will eventually do something similar to the less function but error
     * on the other side.
     * 
     * @param otherCoord
     *            to find the distance to.
     * @return in meters a number that is greater than or equal to the spheroid
     * @throws ClassCastException
     *             if the <code>srid</code>'s are not 4326.
     */
    public double distanceMoreThanOrEqualToSpheroidDistanceInMeters(
            Coordinate otherCoord) {
        throw new UnsupportedOperationException();
    }

    public double distanceHaversineInMeters(Coordinate otherCoord) {
        // SRID's must match and not be null.
        // This can't be null, duh, and we don't check if otherCoord is, which
        // means the NullPointerException will be thrown.
        if (null == this.srid || null == otherCoord.srid) {
            throw new ClassCastException(
                    "This distance function uses the spheroid distance, but you "
                            + "are using Coordinates with null SRID~s (from a PostgreSQL "
                            + "point?).  This doesn't really make any sense and you "
                            + "probably want to use .distanceCarteasianInSRUnits( other )"
                            + "instead.");
        } else if (!this.srid.equals(otherCoord.srid)) {
            throw new ClassCastException(
                    "The SRID of otherCoord does't match this one.");
        }
        final double piOver180 = Math.PI / 180.0;
        final double earthRadiusInMeters = 6367000;
        final double lon1 = piOver180 * this.lon;
        final double lat1 = piOver180 * this.lat;
        final double lon2 = piOver180 * otherCoord.lon;
        final double lat2 = piOver180 * otherCoord.lat;
        // Haversine formula:
        final double dlon = lon2 - lon1;
        final double dlat = lat2 - lat1;
        final double a = FastMath.sin(dlat / 2) * FastMath.sin(dlat / 2)
                + FastMath.cos(lat1) * FastMath.cos(lat2)
                * FastMath.sin(dlon / 2) * FastMath.sin(dlon / 2);
        final double c = 2 * FastMath.atan2(FastMath.sqrt(a),
                FastMath.sqrt(1 - a));
        return earthRadiusInMeters * c;
    }

    /**
     * Implementation of Thaddeus Vincenty's algorithms to solve the direct and
     * inverse geodetic problems.
     * <p/>
     * This implementation may be as good as the PostGIS provided one, the
     * errors seem to be pretty small (10 ^ -3 meters), but a thorough
     * check/analysis has not been done.
     * <p/>
     * For more information, see Vincenty's original publication on the NOAA
     * web-site: <a href="http://www.ngs.noaa.gov/PUBS_LIB/inverse.pdf">
     * http://www.ngs.noaa.gov/PUBS_LIB/inverse.pdf</a>
     * 
     * @param otherCoord
     * @return the distance calculated this way.
     * @throws ClassCastException
     *             if the SRID~s don't match or are null.
     * @throws NullPointerException
     *             if otherCoord is null.
     */
    public double distanceVincentyInMeters(Coordinate otherCoord) {
        // SRID's must match and not be null.
        // This can't be null, duh, and we don't check if otherCoord is, which
        // means the NullPointerException will be thrown.
        if (null == this.srid || null == otherCoord.srid) {
            throw new ClassCastException(
                    "This distance function uses the spheroid distance, but you "
                            + "are using Coordinates with null SRID~s (from a PostgreSQL "
                            + "point?).  This doesn't really make any sense and you "
                            + "probably want to use .distanceCarteasianInSRUnits( other )"
                            + "instead.");
        } else if (!this.srid.equals(otherCoord.srid)) {
            throw new ClassCastException(
                    "The SRID of otherCoord does't match this one.");
        }
        // else we are sure they are the same, and not null.

        final double a;
        final double b;
        final double f;
        // Set a, b, and f.
        switch (this.srid) {
        case 4326:
            a = 6378137.0;
            b = 6356752.3142;
            f = (a - b) / a;
            break;
        default:
            throw new ClassCastException(
                    "The given SRID is not entered in this function, you can"
                            + " get it by running the DB query defined in a function"
                            + " in the NETCONFIG project called "
                            + "util.SpatialDatabaseQueries.getSpheroidStringCached()"
                            + ".  This should return a spheroid string like:\n"
                            + "\tSPHEROID(\"WGS 84\",6378137,298.257223563)\n"
                            + "which is SPHEROID( desc, d1, d2 ), so then\n"
                            + "\ta = d1\n"
                            + "\tb = ( d2 * d1 - d1 ) / d2\n"
                            + "\tf = 1 / d2\n"
                            + "Then you can add the numbers to the switch statement"
                            + " in the Java.");
        }

        // CHECKSTYLE:OFF

        // Now a, b, and f should be set.

        //
        // All constants below refer to Vincenty's publication (see above).
        //
        // get parameters as radians
        final double PiOver180 = Math.PI / 180.0;
        final double phi1 = PiOver180 * this.lat;
        final double lambda1 = PiOver180 * this.lon;
        final double phi2 = PiOver180 * otherCoord.lat;
        final double lambda2 = PiOver180 * otherCoord.lon;

        // calculations
        final double a2 = a * a;
        final double b2 = b * b;
        final double a2b2b2 = (a2 - b2) / b2;

        final double omega = lambda2 - lambda1;

        final double tanphi1 = FastMath.tan(phi1);
        final double tanU1 = (1.0 - f) * tanphi1;
        final double U1 = FastMath.atan(tanU1);
        final double sinU1 = FastMath.sin(U1);
        final double cosU1 = FastMath.cos(U1);

        final double tanphi2 = FastMath.tan(phi2);
        final double tanU2 = (1.0 - f) * tanphi2;
        final double U2 = FastMath.atan(tanU2);
        final double sinU2 = FastMath.sin(U2);
        final double cosU2 = FastMath.cos(U2);

        final double sinU1sinU2 = sinU1 * sinU2;
        final double cosU1sinU2 = cosU1 * sinU2;
        final double sinU1cosU2 = sinU1 * cosU2;
        final double cosU1cosU2 = cosU1 * cosU2;

        // Below are the re-assignable fields
        // eq. 13
        double lambda = omega;
        // intermediates we'll need to compute 's'
        double A = 0.0;
        double B = 0.0;
        double sigma = 0.0;
        double deltasigma = 0.0;
        double lambda0;

        final int num_iters = 5; // 20
        final double change_threshold = 1e-5; // 0.0000000000001;
        for (int i = 0; i < num_iters; i++) {
            lambda0 = lambda;

            final double sinlambda = FastMath.sin(lambda);
            final double coslambda = FastMath.cos(lambda);

            // eq. 14
            final double sin2sigma = (cosU2 * sinlambda * cosU2 * sinlambda)
                    + (cosU1sinU2 - sinU1cosU2 * coslambda)
                    * (cosU1sinU2 - sinU1cosU2 * coslambda);
            final double sinsigma = FastMath.sqrt(sin2sigma);

            // eq. 15
            final double cossigma = sinU1sinU2 + (cosU1cosU2 * coslambda);

            // eq. 16
            sigma = FastMath.atan2(sinsigma, cossigma);

            // eq. 17 Careful! sin2sigma might be almost 0!
            final double sinalpha = (sin2sigma == 0) ? 0.0 : cosU1cosU2
                    * sinlambda / sinsigma;
            final double alpha = FastMath.asin(sinalpha);
            final double cosalpha = FastMath.cos(alpha);
            final double cos2alpha = cosalpha * cosalpha;

            // eq. 18 Careful! cos2alpha might be almost 0!
            final double cos2sigmam = cos2alpha == 0.0 ? 0.0 : cossigma - 2
                    * sinU1sinU2 / cos2alpha;
            final double u2 = cos2alpha * a2b2b2;

            final double cos2sigmam2 = cos2sigmam * cos2sigmam;

            // eq. 3
            A = 1.0 + u2 / 16384 * (4096 + u2 * (-768 + u2 * (320 - 175 * u2)));

            // eq. 4
            B = u2 / 1024 * (256 + u2 * (-128 + u2 * (74 - 47 * u2)));

            // eq. 6
            deltasigma = B
                    * sinsigma
                    * (cos2sigmam + B
                            / 4
                            * (cossigma * (-1 + 2 * cos2sigmam2) - B / 6
                                    * cos2sigmam * (-3 + 4 * sin2sigma)
                                    * (-3 + 4 * cos2sigmam2)));

            // eq. 10
            final double C = f / 16 * cos2alpha * (4 + f * (4 - 3 * cos2alpha));

            // eq. 11 (modified)
            lambda = omega
                    + (1 - C)
                    * f
                    * sinalpha
                    * (sigma + C
                            * sinsigma
                            * (cos2sigmam + C * cossigma
                                    * (-1 + 2 * cos2sigmam2)));

            // see how much improvement we got
            final double change = FastMath.abs((lambda - lambda0) / lambda);

            if ((i > 1) && (change < change_threshold)) {
                break;
            }
        } // for

        // eq. 19
        return b * A * (sigma - deltasigma);
        // CHECKSTYLE:ON
    } // distanceVincentyInMeters()

} // end class
