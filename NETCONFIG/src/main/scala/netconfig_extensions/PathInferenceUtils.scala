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
import java.lang.{ Boolean => JBool }
import CollectionUtils._
import com.google.common.collect.ImmutableList
import collection.JavaConversions._
import edu.berkeley.path.bots.core.MMLogging
import edu.berkeley.path.bots.netconfig.Link
import edu.berkeley.path.bots.netconfig.Datum.ProbeCoordinate
import edu.berkeley.path.bots.netconfig.Route
import edu.berkeley.path.bots.netconfig.Datum.PathInference
import edu.berkeley.path.bots.netconfig.NetconfigException
import edu.berkeley.path.bots.netconfig.Datum.RouteTT

/**
 * Utilities related to Datum.PathInference, that should eventually go there.
 */
object PathInferenceUtils extends MMLogging {
  /**
   *
   * pcs have to be sorted in time
   * TODO(tjh) document this function.
   */
  def createPathInferenceFromProbeCoordinates[L <: Link: Manifest](
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

  /**
   * When possible, extracts the best route
   * TODO(tjh) document
   */
  def projectPathInferenceToRouteTT[L <: Link](
    pi: PathInference[L],
    paths_prob_ratio: Double = 1.0): Option[RouteTT[L]] = {

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
}
