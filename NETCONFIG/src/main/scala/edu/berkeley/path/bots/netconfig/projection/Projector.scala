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


import collection.immutable.{ HashMap => IMap }
import math.Ordering
import edu.berkeley.path.bots.netconfig.Link
import edu.berkeley.path.bots.core.MMLogging
import edu.berkeley.path.bots.core.Coordinate
import edu.berkeley.path.bots.netconfig.Spot
import edu.berkeley.path.bots.core.CoordinateGeometry

/**
 * A projector class.
 *
 * This trait implements most of the logic for projecting a [[core.Coordinate]]
 * object onto a sequence of [[netconfig.Spot]] objects.
 *
 * The only function users should be using is '''getClosestLinks'''.
 *
 * The concrete implementations for this class are provided by
 * [[netconfig.projection.KDProjector]]
 * and [[netconfig.projection.NaiveProjector]].
 *
 * A projector object is usually built using one of the static methods from
 * [[netconfig.projection.ProjectorFactory]]
 *
 * @see [[netconfig.projection.ProjectorFactory]]
 *
 * @author tjhunter
 */
trait Projector[L <: Link] extends MMLogging {

  /**
   * All the links that will be used for the projection.
   */
  val links: Seq[L]

  val searchRadius: Double = 30.0

  /**
   * Computes the cumulative value of an array.
   */
  private def cumsum(xs: Array[Double]): Array[Double] = {
    for (i <- 1 until xs.length) {
      xs(i) += xs(i - 1)
    }
    xs
  }

  /**
   * Decomposes all the GeoMultLine objects into sequences of segments and
   * builds a map from segments to the original link, and the offset on this
   * link.
   */
  protected val segments = IMap.empty[Segment, (L, Double)] ++ links.flatMap(l => {
    val geom = l.geoMultiLine
    val length = geom.getLength
    if (math.abs(length - l.length) > 1) {
      logWarning("Bad length: " + length + " " + l.length + " " + l)
    }
    val coords = geom.getCoordinates
    val coord_pairs = coords.dropRight(1) zip coords.drop(1)
    val segments = coord_pairs.flatMap(z => {
      val (start, end) = z
      // Add some extra intermediate coordinates to make sure there is
      // always one coordinate within the radius of the search circle.
      // Necessary for very long segments.
      val s = new Segment(start, end)
      s.breakIntoSmallerSegments(searchRadius)
    })
    val cum_lengths = Seq(0.0) ++ cumsum(segments.map(_.length).toArray).dropRight(1)
    val res = (segments zip cum_lengths).map(z => (z._1, (l, z._2)))
    if (res.isEmpty) {
      logWarning("The projector has an empty set of segments!!")
    }
    res
  })

  /**
   * Inner ordering of tuples.
   */
  private val ordering = new Ordering[(Double, L, Double)] {
    def compare(x1: (Double, L, Double), x2: (Double, L, Double)) = x1._1.compare(x2._1)
  }

  /**
   * Returns a set of segments.
   *
   * This set contains at least all the segments within a radius of the
   * given [[core.Coordinate]].
   *
   * This set is computed efficiently in [[netconfig.projection.KDProjector]]
   * and very simply in [[netconfig.projection.NaiveProjector]].
   */
  protected def getCandidateSegmentsWithinRadius(c: Coordinate, radius: Double): Seq[Segment]

  /**
   * If you want a grid projection within the radius.
   * FIXME: more documentation...
   */
  def getGridProjection(c: Coordinate,
    radius: Double,
    max_returns: Int,
    grid_step: Double): Array[Spot[L]] = {
    val closest_projs = getClosestLinks(c, radius, max_returns)
    closest_projs.flatMap(p => {
      val link = p.link
      val geom = link.geoMultiLine
      (0.0 to link.length.toDouble by grid_step).flatMap(offset => {
        val c2 = geom.getCoordinate(offset)
        val d = CoordinateGeometry.distance(c, c2)
        if (d < radius)
          Some(Spot.from(link, offset))
        else
          None
      })
    }).toArray
  }

  /**
   * Returns the k links closest to a coordinate within a certain radius.
   *
   * @param c the coordinate you want to find the projection on.
   * @param radius the maximum radius to look about. This radius is with respect
   *   to the distance function used by the coordinate system.
   * @param maxReturns the maximum number of links to return
   *
   * @return an array of [[netconfig.Spot]] objects, sorted by increasing
   * distance from the coordinate, in which the offset indicates the point
   * that realizes the minimum distance for this link. The lane information
   * is set to 0.
   *
   */
  def getClosestLinks(c: Coordinate,
    radius: Double,
    maxReturns: Int): Array[Spot[L]] = {
    val candidates = getCandidateSegmentsWithinRadius(c, radius)

    // a single statement: scala power...
    candidates.flatMap(segment => {
      val (link, cum_l) = segments(segment)
      val (distance, offset, p) = segment.closestPoint(c, 0.1)
      // Check the offset makes sense
      assert(cum_l <= link.length, (link, link.length, offset, cum_l, distance, p))
      assert(cum_l >= 0, (link, link.length, offset, cum_l, distance, p))
      assert(offset >= 0, (link, link.length, offset, cum_l, distance, p))
      assert(offset + cum_l <= link.length + 0.1, (link, link.length, offset, cum_l, distance, p))

      val corrected_offset = math.min(offset + cum_l, link.length)
      if (distance < radius)
        Some((distance, link, corrected_offset))
      else
        None
    }).groupBy(_._2)
      .values
      .map(_.min(ordering))
      .toSeq
      .sorted(ordering)
      .take(maxReturns)
      .map(z => Spot.from(z._2, z._3, 0)).toArray
  }
}
