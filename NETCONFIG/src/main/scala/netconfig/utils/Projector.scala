package netconfig.utils

import netconfig.Link
import netconfig._
import netconfig.Datum._
import netconfig_extensions.CollectionUtils._

/**
 * Interface to project data from a network to another network,
 * when success is not guaranteed.
 *
 * This is an injection in the mathematical sense.
 */
trait Projector[L1 <: Link, L2 <: Link] {

  /**
   * The only method to implement.
   */
  def project(sp: Spot[L1]): Option[Spot[L2]]

  def project(route: Route[L1]): Option[Route[L2]] = {
    val mapped_spots = route.spots.flatMap(sp => project(sp))
    if (mapped_spots.size == route.spots.size) {
      val mapped_route = Route.fromSpots(mapped_spots)
      Some(mapped_route)
    } else { None }
  }

  def project(pc: ProbeCoordinate[L1]): Option[ProbeCoordinate[L2]] = {
    val new_spots = pc.spots().flatMap(sp => project(sp))
    if (new_spots.size == pc.spots.size) {
      Some(pc.clone(new_spots))
    } else {
      None
    }
  }

  def project(pi: PathInference[L1]): Option[PathInference[L2]] = {
    val new_routes = pi.routes.flatMap(project(_))
    if (new_routes.size == pi.routes.size) {
      Some(pi.clone(new_routes))
    } else {
      None
    }
  }

  def project(rtt: RouteTT[L1]): Option[RouteTT[L2]] = {
    project(rtt.toPathInference).map(_.toRouteTT)
  }

  def project(tsp: TSpot[L1]): Option[TSpot[L2]] = {
    project(tsp.toProbeCoordinate).map(_.toTSpot)
  }
}