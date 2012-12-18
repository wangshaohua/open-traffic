/**
 * Copyright 2012. The Regents of the University of California (Regents).
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

package edu.berkeley.path.bots.netconfig.projection
import edu.berkeley.path.bots.core.Coordinate
import edu.berkeley.path.bots.core.MMLogging
import edu.berkeley.path.bots.core.CoordinateGeometry


/**
 * A segment in cartesian coordinates.
 *
 * It approximates the same segment in geodesic coordinates.
 */
final class Segment(val start: Coordinate, val end: Coordinate)
  extends MMLogging {

  val length = CoordinateGeometry.distance(start, end)

  lazy val (dlat, dlon) = CoordinateGeometry.localGeometry(start)

  private val (dlat1, dlon1) = CoordinateGeometry.difference(start, end)
  private val (x1, y1) = (dlat1 * dlat, dlon1 * dlon)
  private val local_length_square = x1 * x1 + y1 * y1

  override def hashCode = (start, end).hashCode

  override def equals(o: Any) = o match {
    case s: Segment => (s.start == start) && (s.end == end)
    case _ => false
  }

  def breakIntoSmallerSegments(cutoff_length: Double): Seq[Segment] = {
    if (length <= cutoff_length)
      List(this)
    else {
      // Find a cutting point and build the rest by induction.
      val theta = cutoff_length / length;
      val c = new Coordinate(start.srid,
        theta * end.lat + (1 - theta) * start.lat,
        theta * end.lon + (1 - theta) * start.lon)
      return List(new Segment(start, c)) ++ (new Segment(c, end)).breakIntoSmallerSegments(cutoff_length)
    }
  }

  /**
   * @return (distance,offset on link,created point on link)
   */
  def closestPoint(p: Coordinate, precision: Double): (Double, Double, Coordinate) = {

    val (dlat2, dlon2) = CoordinateGeometry.difference(start, p)
    val (x2, y2) = (dlat2 * dlat, dlon2 * dlon)

    // Do not forget the parens...
    val k = (x1 * x2 + y1 * y2) / local_length_square

    if (k <= 0) {
      return (CoordinateGeometry.distance(start, p), 0.0, start)
    }
    if (k >= 1) {
      return (CoordinateGeometry.distance(end, p), length, end)
    }
    val closest_p = CoordinateGeometry.convexCombination(start, end, k)
    val d = CoordinateGeometry.distance(closest_p, p)
    return (d, k * length, closest_p)
  }

}

