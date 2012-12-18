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

package netconfig_extensions.projection


import collection.immutable.{ HashMap => IMap }
import collection.mutable.{ HashMap => MMap, PriorityQueue }
import math.Ordering
import edu.berkeley.path.bots.netconfig.Link
import edu.berkeley.path.bots.core.CoordinateGeometry
import edu.berkeley.path.bots.core.Coordinate
import edu.berkeley.path.bots.netconfig.Spot

/**
 * Efficient projector that uses a KD-tree.
 */
class KDProjector[L <: Link](val links: Seq[L])
  extends Projector[L] {

  val center = segments.keys.head.start
  val (dlat, dlon) = CoordinateGeometry.localGeometry(center)

  // Build mapping segments => euclidian segments
  //  def euc_segments = segments.keys.map(seg => {
  //      new EuclidianSegment(project(seg.start),
  //                           project(seg.end),
  //                           seg)
  //    }).toArray
  // Build KD tree
  val tree = {
    val euc_segments = segments.keys.map(seg => {
      new EuclidianSegment(project(seg.start), project(seg.end), seg)
    }).toArray
    KDTree.build(euc_segments)
  }

  def project(c: Coordinate): EuclidianPoint = {
    val z1 = CoordinateGeometry.difference(center, c)
    val X1 = z1._1
    val Y1 = z1._2
    val (x1, y1) = (X1 * dlat, Y1 * dlon)
    new EuclidianPoint(x1, y1)
  }

  override def getCandidateSegmentsWithinRadius(c: Coordinate, radius: Double): Seq[Segment] = {
    val euc_segs = tree.segmentsWithinRadius(project(c), radius)
    euc_segs.map(_.reference.asInstanceOf[Segment])
  }

  /**
   * If you want a grid projection within the radius.
   * FIXME: more documentation...
   */
  override def getGridProjection(c: Coordinate,
    radius: Double,
    max_returns: Int,
    grid_step: Double): Array[Spot[L]] = {
    val closest_projs = getClosestLinks(c, radius, max_returns)
    val c_euc = project(c)
    closest_projs.flatMap(p => {
      val link = p.link
      val geom = link.geoMultiLine
      (0.0 to link.length.toDouble by grid_step).flatMap(offset => {
        val c2 = geom.getCoordinate(offset)
        val c2_euc = project(c2)
        // Much faster but imprecise way to compute distance
        val d = math.sqrt((c2_euc.x - c_euc.x) * (c2_euc.x - c_euc.x) + (c2_euc.y - c_euc.y) * (c2_euc.y - c_euc.y))
        if (d < radius)
          Some((Spot.from(link, offset), d))
        else
          None
      })
    }).toArray
      .sortBy(_._2)
      .take(max_returns)
      .map(_._1)
      .toArray
  }

}
