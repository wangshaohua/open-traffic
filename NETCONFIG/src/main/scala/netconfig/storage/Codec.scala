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

package netconfig.storage
import netconfig.Link
import netconfig.Spot
import netconfig.Route
import collection.JavaConversions._
import netconfig_extensions.CollectionUtils._
import com.google.common.collect.ImmutableList
import core.storage.CoordinateRepresentation
import core.storage.GeoMultiLineRepr

trait Codec[L <: Link] {

  def fromLinkID(lid: LinkIDRepr): L

  def toLinkID(l: L): LinkIDRepr

  /**
   * @param extended_presentation: encodes redundant information like coordinates
   *   (useful for interacting with matlab or python).
   */
  def toRepr(s: Spot[L], extended_representation: Boolean): SpotRepr = {
    val coo = if (extended_representation) {
      Some(CoordinateRepresentation.toRepr(s.toCoordinate()))
    } else {
      None
    }
    new SpotRepr(toLinkID(s.link), s.offset, coo)
  }

  /**
   * @param extended_presentation: encodes redundant information like coordinates
   *   (useful for interacting with matlab or python).
   */
  def toRepr(r: Route[L], extended_representation: Boolean): RouteRepr = {
    val geom = if (extended_representation) {
      Some(GeoMultiLineRepr.toRepr(r.geoMultiLine()))
    } else {
      None
    }
    new RouteRepr(
        r.links.map(toLinkID _),
        r.spots.map(sp => toRepr(sp, extended_representation)), geom)
  }

  def fromRepr(sr: SpotRepr): Spot[L] = {
    Spot.from(fromLinkID(sr.linkId), sr.offset)
  }

  def fromRepr(rr: RouteRepr): Route[L] = {
    val links: ImmutableList[L] = rr.links.map(fromLinkID _)
    val spots: ImmutableList[Spot[L]] = rr.spots.map(fromRepr _)
    Route.from(spots, links)
  }
}