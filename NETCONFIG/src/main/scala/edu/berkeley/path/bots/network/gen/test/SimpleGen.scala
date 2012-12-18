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
package edu.berkeley.path.bots.network.gen.test

import edu.berkeley.path.bots.netconfig.Link
import edu.berkeley.path.bots.netconfig.storage.{ NodeIDRepr, LinkIDRepr }
import edu.berkeley.path.bots.network.gen.GenericLinkRepresentation
import edu.berkeley.path.bots.network.gen.NetworkBuilder
import edu.berkeley.path.bots.core.storage.CoordinateRepresentation
import edu.berkeley.path.bots.core.Coordinate
import edu.berkeley.path.bots.core.storage.GeoMultiLineRepr
/**
 * Creates some simple networks useful for testing.
 */
object SimpleGen {

  /**
   * Creates a line network.
   * Each link has the same length, oriented in the lat axis, and
   * coordinates are in the cartesian domain.
   *
   * Unit: meters
   */
  def line(n: Int, length: Double = 10.0): IndexedSeq[Link] = {
    require(n >= 1)
    require(length >= 0.0)

    val nodeReprs = for (idx <- 0 until (n + 1)) yield NodeIDRepr(idx, 0)
    val linkReprs = for (idx <- 0 until n) yield LinkIDRepr(idx, 0)
    val nodeGeomReprs = (0 until (n + 1)).map(idx => {
      CoordinateRepresentation(
        srid = Some(Coordinate.SRID_CARTESIAN),
        lat = length * idx,
        lon = 0.0)
    }).toArray
    val glrs = for (
      (((n1, n2), lr), idx) <- nodeReprs.dropRight(1)
        .zip(nodeReprs.drop(1))
        .zip(linkReprs)
        .zipWithIndex
    ) yield {
      val geom = GeoMultiLineRepr(Seq(nodeGeomReprs(idx), nodeGeomReprs(idx + 1)))
      GenericLinkRepresentation(lr, n1, n2, Some(geom), Some(length))
    }
    val builder = new NetworkBuilder()
    builder.build(glrs)
      .sortBy(_._1.toString)
      .map(_._2)
      .toIndexedSeq
  }
}