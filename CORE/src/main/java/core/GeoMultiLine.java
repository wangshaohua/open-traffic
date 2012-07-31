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
/**
 * @author tjhunter
 */
package core;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * This is the replacement of postgis.LineString to get around some issues with
 * it. It should be used instead. This class is final. If you want to manipulate
 * it, build a list of coordinates first.
 * 
 * @author tjhunter
 */
public final class GeoMultiLine implements Serializable {
    public static final long serialVersionUID = 3L;

    /**
     * length N
     * 
     * TODO(tjh) use immutable collections.
     */
    private final Coordinate[] waypoints;
    /**
     * length N, with t[i] = partial length from o to waypoint[i]
     */
    public final double[] cumulative_lengths; // should be private!
    public final int srid;

    public GeoMultiLine(List<Coordinate> wps) {
        this(wps.toArray(new Coordinate[0]));
    }

    public GeoMultiLine(Coordinate[] wps) {
        assert (wps.length > 1);
        waypoints = Arrays.copyOf(wps, wps.length);
        this.srid = wps[0].srid;
        int n = waypoints.length;
        cumulative_lengths = new double[n];
        cumulative_lengths[0] = 0.0;

        for (int i = 1; i < n; ++i) {
            double l = waypoints[i]
                    .distanceDefaultMethodInMeters(waypoints[i - 1]);
            cumulative_lengths[i] = cumulative_lengths[i - 1] + l;
            assert this.srid == wps[i].srid;
        }
    }

    public Coordinate getFirstCoordinate() {
        return this.waypoints[0];
    }

    public Coordinate getLastCoordinate() {
        return this.waypoints[this.waypoints.length - 1];
    }

    public GeoMultiLine reverse() {
        Coordinate[] reversed_points = new Coordinate[waypoints.length];
        for (int i = 0; i < waypoints.length; ++i)
            reversed_points[i] = waypoints[waypoints.length - i - 1];
        return new GeoMultiLine(reversed_points);
    }

    /**
     * 
     * @return the length of this multiline.
     */
    public double getLength() {
        return cumulative_lengths[cumulative_lengths.length - 1];
    }

    /**
     * max_i { i | cumulative_lengths[i] <= offset}
     * 
     * @param offset
     *            The offset along the line (in meters) for which that way point
     *            index is needed
     * @return the index of the way point just before the specified offset
     */
    public int indexBeforeOffset(double offset) {
        assert (offset >= 0);
        assert (offset <= getLength());
        assert (waypoints != null && waypoints.length > 1);
        // if(offset >= getLength()-Coordinate.DISTANCE_PRECISION)
        // return waypoints.length-2;
        // TODO(?) use fast bisection here
        int cur_idx = 0;
        while ((cur_idx < waypoints.length)
                && (cumulative_lengths[cur_idx] <= offset))
            cur_idx++;
        return cur_idx - 1;
    }

    /**
     * 
     * @param offset
     *            the offset on the line
     * @return the coordinate of the point at the corresponding offset.
     */
    public Coordinate getCoordinate(double offset) {
        assert (offset >= 0);
        assert (offset <= getLength());
        assert (waypoints != null && waypoints.length > 1);
        // Make sure there is no weird error at the end
        if (offset >= getLength() - Coordinate.DISTANCE_PRECISION)
            return waypoints[waypoints.length - 1];

        // TODO(?) use fast bisection here
        int cur_idx = indexBeforeOffset(offset);
        assert (cur_idx <= cumulative_lengths.length - 2);

        double cur_offset = offset - cumulative_lengths[cur_idx];
        double d = cumulative_lengths[cur_idx + 1]
                - cumulative_lengths[cur_idx];
        final Coordinate before = waypoints[cur_idx];
        final Coordinate after = waypoints[cur_idx + 1];
        final double ratio = cur_offset / d;
        final double newLat = (1 - ratio) * before.lat + ratio * after.lat;
        final double newLon = (1 - ratio) * before.lon + ratio * after.lon;
        return new Coordinate(before.srid, newLat, newLon);
    }

    /**
     * 
     * @return the list of coordinates
     */
    public Coordinate[] getCoordinates() {
        return waypoints;
    }

    /**
     * 
     * @param start_offset
     * @param end_offset
     * @return the sub multiline corresponding to the start and end offsets.
     */
    public GeoMultiLine getPartialGeometry(double start_offset,
            double end_offset) {
        // TODO(tjh) remove asserts and use exceptions instead
        // We allow singular offsets.
    	assert (start_offset <= end_offset);
        assert (start_offset <= this.getLength());
        assert (start_offset >= 0);
        assert (end_offset <= this.getLength());
        assert (end_offset >= 0);
        int start_idx = indexBeforeOffset(start_offset);
        int end_idx = indexBeforeOffset(end_offset);
        
        if (start_offset == end_offset) {
            ArrayList<Coordinate> points = new ArrayList<Coordinate>();
            points.add(getCoordinate(start_offset));
            points.add(getCoordinate(start_offset));
            return new GeoMultiLine(points);
        }

        ArrayList<Coordinate> points = new ArrayList<Coordinate>();
        points.add(getCoordinate(start_offset));
        for (int i = start_idx + 1; i <= end_idx; ++i) {
            points.add(waypoints[i]);
        }
        if (cumulative_lengths[end_idx] < end_offset
                - Coordinate.DISTANCE_PRECISION) {
            points.add(getCoordinate(end_offset));
        }
        return new GeoMultiLine(points);
    }

    /**
     * Concatenates two geo multilines, the order of concatenations determined
     * by the distance of their end points.
     * 
     * If one of the geomultiline is null, the other one will be returned (even
     * if it is also null). This is scala's option on the cheap...
     * 
     * @param l1
     * @param l2
     * @param tolerance
     * @return the concatenated GeoMultiLine
     * @throws assertion
     *             error if the minimum distance between any end point is
     *             greater that tolerance.
     */
    public static GeoMultiLine greedyConcatenation(GeoMultiLine l1,
            GeoMultiLine l2, double tolerance) {
        if (l1 == null)
            return l2;
        if (l2 == null)
            return l1;

        double best = Double.POSITIVE_INFINITY;
        boolean r1 = true;
        boolean r2 = true;
        double d = l1.getLastCoordinate().distanceDefaultMethodInMeters(
                l2.getFirstCoordinate());
        if (d < best) {
            r1 = false;
            r2 = false;
            best = d;
        }

        d = l1.getLastCoordinate().distanceDefaultMethodInMeters(
                l2.getLastCoordinate());
        if (d < best) {
            r1 = false;
            r2 = true;
            best = d;
        }

        d = l1.getFirstCoordinate().distanceDefaultMethodInMeters(
                l2.getFirstCoordinate());
        if (d < best) {
            r1 = true;
            r2 = false;
            best = d;
        }

        d = l1.getFirstCoordinate().distanceDefaultMethodInMeters(
                l2.getLastCoordinate());
        if (d < best) {
            r1 = true;
            r2 = true;
            best = d;
        }

        assert (d < tolerance);

        List<Coordinate> ll = new ArrayList<Coordinate>();
        for (Coordinate c : (r1 ? l1.reverse() : l1).getCoordinates())
            ll.add(c);

        for (Coordinate c : (r2 ? l2.reverse() : l2).getCoordinates())
            ll.add(c);

        return new GeoMultiLine(ll);
    }
    
    /**
     * Conversion to array of doubles.
     * 
     * @return an 2 by N matrix, first column is lats, second column is longs
     */
    public double[][] toMArray() {
        double[][] xys = new double[waypoints.length][2];
        for (int i = 0; i < waypoints.length; ++i) {
            xys[i][0] = waypoints[i].lat;
            xys[i][1] = waypoints[i].lon;
        }
        return xys;
    }

    /**
     * A very simple container for a 2D vector between coordinates.
     */
    private class Vector {
        public final double dx;
        public final double dy;

        public Vector(double dx, double dy) {
            this.dx = dx;
            this.dy = dy;
        }

        public Vector normalized() {
            final double n = Math.sqrt(dx * dx + dy * dy);
            return new Vector(dx / n, dy / n);
        }

        public String toString() {
            return "Vector[" + dx + ", " + dy + "]";
        }
    }

    private Vector shiftVector(Vector u, Vector v, double delta) {
        double discriminant = u.dx * v.dy - u.dy * v.dx;
        assert (discriminant != 0);
        double new_dx = delta * v.dy / discriminant;
        double new_dy = delta * v.dx / discriminant;
        return new Vector(new_dx, -new_dy);
    }

    /**
     * Returned the shifted GML, i.e. the geomultiline obtained by shifting all
     * its *segments* to the right by some distance offset. The shifted GML
     * recalculates the waypoints to respect this shit.
     * 
     * @param distance
     *            in meters, if positive the shift will be to the right, and if
     *            negeative it will be to the left.
     * @return
     * 
     *         Internally, computes a local representation of the line in a
     *         local cartesian geometry, the performs the shit. TODO(?) a
     *         picture is worth a 1000 words, how to add a picture here?
     */
    public GeoMultiLine shifted(double distance) {
        Coordinate[] local = new Coordinate[this.waypoints.length];
        // Compute the local differential, using the defaul distance.
        final Coordinate local_center = this.waypoints[0];
        final double epsi = 0.0001;
        double dlat = 1.0;
        {
            // Shift by epsilon:
            final Coordinate dc = new Coordinate(local_center.srid,
                    local_center.lat + epsi, local_center.lon);
            final double d = dc.distanceDefaultMethodInMeters(local_center);
            dlat = d / epsi;
        }
        double dlon = 1.0;
        {
            // Shift by epsilon:
            final Coordinate dc = new Coordinate(local_center.srid,
                    local_center.lat, local_center.lon + epsi);
            final double d = dc.distanceDefaultMethodInMeters(local_center);
            dlon = d / epsi;
        }

        // All the local coordinates.
        local[0] = new Coordinate(Coordinate.SRID_CARTESIAN, 0.0, 0.0);
        for (int i = 1; i < local.length; ++i) {
            double local_x = dlat * (this.waypoints[i].lat - local_center.lat);
            double local_y = dlon * (this.waypoints[i].lon - local_center.lon);
            local[i] = new Coordinate(Coordinate.SRID_CARTESIAN, local_x,
                    local_y);
            // System.out.println("Local: "+local[i]);
        }

        // The normal vectors to each segment of the GML, in cartesian system:
        Vector[] us = new Vector[local.length - 1];
        for (int i = 0; i < local.length - 1; ++i) {
            double dx = local[i + 1].lat - local[i].lat;
            double dy = local[i + 1].lon - local[i].lon;
            us[i] = (new Vector(dy, -dx)).normalized();
            // System.out.println("u: "+us[i]);
        }

        // The normal vector to the planes separating each segment.
        Vector[] vs = new Vector[local.length - 1];
        for (int i = 0; i < local.length - 2; ++i) {
            double dux = us[i + 1].dx + us[i].dx;
            double duy = us[i + 1].dy + us[i].dy;
            vs[i] = (new Vector(duy, -dux)).normalized();
            // System.out.println("v: "+vs[i]);
        }

        // The final coordinates.
        Coordinate[] shifted = new Coordinate[local.length];
        // The first one and the last one are a bit special:
        {
            Vector v_first = new Vector(-us[0].dy, us[0].dx);
            Vector shift = shiftVector(us[0], v_first, distance);
            // Get the new global coordinates
            double new_lat = shift.dx / dlat + this.waypoints[0].lat;
            double new_lon = shift.dy / dlon + this.waypoints[0].lon;
            // System.out.println("new local coords: "+(shift.dx +
            // local[0].lat)+", "+(shift.dy + local[0].lon));
            shifted[0] = new Coordinate(waypoints[0].srid, new_lat, new_lon);
            // System.out.println("shift: "+shift);
        }
        for (int i = 1; i < local.length - 1; ++i) {
            Vector shift = shiftVector(us[i], vs[i - 1], distance);
            // System.out.println("shift: "+shift);
            // Get the new global coordinates
            // System.out.println("new local coords: "+(shift.dx +
            // local[i].lat)+", "+(shift.dy + local[i].lon));
            double new_lat = shift.dx / dlat + waypoints[i].lat;
            double new_lon = shift.dy / dlon + waypoints[i].lon;
            shifted[i] = new Coordinate(waypoints[i].srid, new_lat, new_lon);
        }
        {
            Vector vLast = new Vector(-us[local.length - 2].dy,
                    us[local.length - 2].dx);
            Vector shift = shiftVector(us[local.length - 2], vLast, distance);
            // System.out.println("shift: "+shift);
            // Get the new global coordinates
            double newLat = shift.dx / dlat + waypoints[local.length - 1].lat;
            double newLon = shift.dy / dlon + waypoints[local.length - 1].lon;
            // System.out.println("new local coords: "+(shift.dx +
            // local[local.length-1].lat)+", "+(shift.dy +
            // local[local.length-1].lon));
            shifted[local.length - 1] = new Coordinate(
                    waypoints[local.length - 1].srid, newLat, newLon);
        }
        return new GeoMultiLine(shifted);
    }

    /**
     * Creates a new line such that the first point is also copied at the last
     * position to create a ring.
     * 
     * @return a new line such that the first point is also copied at the last
     *         position to create a ring.
     */
    public GeoMultiLine closedLine() {
        final int numPoints = this.waypoints.length;
        Coordinate[] newWaypoints = new Coordinate[numPoints + 1];
        for (int i = 0; i < numPoints; ++i) {
            newWaypoints[i] = this.waypoints[i];
        }
        newWaypoints[numPoints] = this.waypoints[0];
        return new GeoMultiLine(newWaypoints);
    }

} // class

