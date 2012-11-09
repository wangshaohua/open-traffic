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

package netconfig.utils

import netconfig.Link
import netconfig._
import netconfig.Datum._
import netconfig_extensions.CollectionUtils._

/**
 * Trait to map the data expressed in a network
 * into an equivalent representation in a different
 * network.
 *
 * This transform is guaranteed to succeed and to have
 * an inverse transform to map the data back (i.e. it is
 * a mapping in the mathematical sense).
 */
trait Converter[L1 <: Link, L2 <: Link] {

  /**
   * The only method to implement.
   */
  def map(sp: Spot[L1]): Spot[L2]

  def map(route: Route[L1]): Route[L2] = {
    val mapped_spots = route.spots.map(sp => map(sp))
    Route.fromSpots(mapped_spots)
  }

  def map(pc: ProbeCoordinate[L1]): ProbeCoordinate[L2] = {
    val new_spots = pc.spots().map(sp => map(sp))
    pc.clone(new_spots)
  }

  def map(pi: PathInference[L1]): PathInference[L2] = {
    val new_routes = pi.routes.map(map(_))
    pi.clone(new_routes)
  }

  def map(rtt: RouteTT[L1]): RouteTT[L2] = {
    map(rtt.toPathInference).toRouteTT
  }

  def map(tsp: TSpot[L1]): TSpot[L2] = {
    map(tsp.toProbeCoordinate).toTSpot
  }
}