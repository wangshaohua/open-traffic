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
package netconfig_extensions

import netconfig._
import netconfig.Datum._
import core_extensions.MMLogging
import java.lang.{ Boolean => JBool }
import bots_math.ImmutableTensor1
import CollectionUtils._
import com.google.common.collect.ImmutableList
import collection.JavaConversions._

/**
 * Utilities related to Datum.PathInference, that should eventually go there.
 */
object PathInferenceUtils extends MMLogging {

  /**
   * The leading probability has to be twice the next one.
   * Just pick a most probable one, do not care about getting a really better one.
   */
  val paths_prob_ratio = 1.0

  /**
   *
   * pcs have to be sorted in time
   * @todo: doc
   */
  def createPathInferenceFromProbeCoordinates[L <: Link : Manifest](
    pcs: Array[ProbeCoordinate[L]],
    paths: Array[Route[L]],
    probabilities: Array[Double]): PathInference[L] = {
    if (pcs.isEmpty) {
      throw new NetconfigException(null, "Empty set")
    }
    val id = {
      val ids = pcs.map(_.id).toSet
      if (ids.size != 1) {
        throw new NetconfigException(null, "Mangling different drivers")
      }
      ids.head
    }
    // The hired value
    val hired: JBool = {
      val hireds = pcs.map(_.hired)
      if (hireds.exists(_ == null)) {
        null
      } else {
        val hired_values = hireds.map(_.booleanValue).toSet
        if (hired_values.size != 1) {
          null
        } else {
          hired_values.head
        }
      }
    }
    PathInference.from(id, pcs.head.time, pcs.last.time,
      paths, probabilities, hired)
  }

  def projectArrayPathInferenceToRouteTT[L <: Link](pis: Array[PathInference[L]]): Array[RouteTT[L]] =
    pis.flatMap(projectPathInferenceToRouteTT(_))

  def projectSeqPathInferenceToRouteTT[L <: Link](pis: Seq[PathInference[L]]): Seq[RouteTT[L]] =
    pis.flatMap(projectPathInferenceToRouteTT(_))

  /**
   * When possible, extracts the best route
   */
  def projectPathInferenceToRouteTT[L <: Link](pi: netconfig.Datum.PathInference[L]): Option[netconfig.Datum.RouteTT[L]] = {

    val idx = pi.getMostProbableIndex
    if (idx == null) {
      return None
    }
    val best_route: Route[L] = pi.routes.get(idx.intValue)
    // Make sure this route dominates
    if (pi.routes.size > 1) {
      val sorted_probs = pi.probabilities.toSeq.sorted.reverse
      val first = sorted_probs(0)
      val second = sorted_probs(1)
      if (first < second * paths_prob_ratio) {
        return None
      }
    }
    Some(RouteTT.from(best_route, pi.startTime, pi.endTime, pi.id))
  }

  def mapPathInferenceToRouteTT[L <: Link](pi: netconfig.Datum.PathInference[L]): netconfig.Datum.RouteTT[L] = {
    projectPathInferenceToRouteTT(pi) match {
      case Some(x) => x
      case None => assert(false); null //FIXME throw exception
    }
  }

//  def findRouteTTIndex[L <: Link](pi: PathInference[L], rtt: RouteTT[L]): Int = {
//    assert(pi.startTime == rtt.startTime)
//    assert(pi.endTime == rtt.endTime)
//    assert(pi.id == rtt.id)
//    findRouteIndex(pi, rtt.route)
//
//  }
//  def findRouteIndex[L <: Link](pi: PathInference[L], r: Route[L]): Int = {
//    pi.routes.indexWhere(pir => (pir != null && pir.equalsWithinTolerance(r)))
//  }
//
//  def findRouteIndex[L <: Link](routes: Array[Route[L]], r: Route[L]): Int = {
//    routes.indexWhere(pir => (pir != null && pir.equalsWithinTolerance(r)))
//  }

//  /**
//   * Sets all values of the path inference object probabilities with 0.
//   *
//   * If a path begins with the correct start spot, ends with the correct start
//   * spot and goes along the links from the trajectory at the right time, then
//   * a prbability of one is assigned to this spot and true is returned.
//   *
//   */
//  def fillPathInferenceProbabilitiesFromTrajectory[L <: Link](pi: PathInference[L],
//    traj: Trajectory[L]): Boolean = {
//
//    // TODO:Initialize the probas at zero for the PI
//    for (i <- 0 until pi.probabilities.length) {
//      pi.probabilities(i) = 0.0
//    }
//
//    // Find the start spot and start index
//    val start_idx = traj.waypoints.indexWhere(_.time.equals(pi.startTime))
//    if (start_idx < 0) {
//      logWarning("Path Inference object " + pi + "\n does not start with a point from trajectory:\n" + traj)
//      return false
//    }
//    // Find the end spot and end index
//    val end_idx = traj.waypoints.indexWhere(_.time.equals(pi.endTime))
//    if (end_idx < 0) {
//      logWarning("Path Inference object " + pi + "\n does not end with a point from trajectory:\n" + traj)
//      return false
//    }
//
//    // Check all the links in between are from the trajectory as well
//    val path_links: Set[L] = (start_idx until end_idx).map(i => traj.links(traj.waypoints_map(i))).toSet
//    val start_spot = traj.waypoints(start_idx).spot
//    val end_spot = traj.waypoints(end_idx).spot
//    val compatible_indexes = pi.routes.zipWithIndex.flatMap(z => {
//      val (r, idx) = z
//      if (r.spots.head.equals(start_spot) && r.spots.last.equals(end_spot)) {
//        val links_set: Set[L] = r.links.toSet
//        if (links_set.subsetOf(path_links) && path_links.subsetOf(links_set)) {
//          Some(idx)
//        } else {
//          None
//        }
//      } else {
//        None
//      }
//    })
//
//    if (compatible_indexes.length == 0) {
//      // No path was found to be compatible
//      return false
//    }
//    if (compatible_indexes.length == 1) {
//      // One match. It *should* be the most common case
//      pi.probabilities.get(compatible_indexes.head) = 1.0
//      return true
//    }
//    logWarning("Found several compatible paths")
//    logWarning(compatible_indexes.map(pi.routes(_)).mkString("\n"))
//    throw new Exception()
//    return false
//  }
}
