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
package edu.berkeley.path.bots.core

/**
 * Operations that perform a linearization of the local geometry.
 */
object CoordinateGeometry {
  /**
   * The convex combination on the geodesic segment that joins two points.
   *
   * @todo the current implementation is incorrect.
   */
  def convexCombination(p1: Coordinate, p2: Coordinate, ratio: Double): Coordinate = {
    assert(p1.srid.intValue == p2.srid.intValue)
    if (p1.srid.intValue == -1)
      throw new Exception("NI!")
    val lat = p1.lat * (1 - ratio) + p2.lat * ratio
    val lon = p1.lon * (1 - ratio) + p2.lon * ratio
    new Coordinate(p1.srid, lat, lon)
  }

  /**
   * Distance between 2 points (meters).
   */
  def distance(p1: Coordinate, p2: Coordinate): Double = {
    if (p1.srid.intValue == Coordinate.SRID_CARTESIAN)
      return p1.distanceCartesianInMeters(p2)
    if (p1.srid.intValue == 4326)
      return p1.distanceVincentyInMeters(p2)
    throw new Exception("Not implemented")
  }

  /**
   * @return (lat,lon) of (end-start)
   *
   * @todo incorrect implementation
   */
  def difference(start: Coordinate, end: Coordinate): (Double, Double) = {
    (end.lat - start.lat, end.lon - start.lon)
  }

  /**
   * Returns the local approximation to the Euclidian
   * space.
   *
   * @return (dlat,dlon)  dlat is in  meter per angle unit
   *     dlon is in meter per angle unit
   */
  def localGeometry(p: Coordinate): (Double, Double) = {
    val delta = 1e-2
    val plat = new Coordinate(p.srid, p.lat + delta, p.lon)
    val plon = new Coordinate(p.srid, p.lat, p.lon + delta)
    val dlat = distance(p, plat)
    val dlon = distance(p, plon)
    (dlat / delta, dlon / delta)
  }
}
