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

package path_inference.output

import netconfig.Datum.PathInference
import netconfig.Link
import core_extensions.MMLogging
import path_inference.PathInferenceParameters2
import path_inference.Delta
import path_inference.crf.CRFFrame
import netconfig_extensions.CollectionUtils._

object PathInferenceOuput extends MMLogging {

  def processDelta(delta: Delta, params: PathInferenceParameters2): Option[PathInference[Link]] = {
    processDelta(delta, params.minPathProbability)
  }

  def processFrame(frame: CRFFrame, params: PathInferenceParameters2): Option[PathInference[Link]] = {
    frame.payload match {
      case delta: Delta => processDelta(delta, params)
      case _ => None
    }
  }

  def processDelta(
    delta: Delta,
    minPathProbability: Double,
    sortByProbability:Boolean=true): Option[PathInference[Link]] = {
    // A delta should contain at least one path
    logInfo("Processing delta")
    assert(delta.paths.length > 0)
    // Filter the paths of too low probability and sort them
    val path_pairs = {
      val z1 = (delta.paths zip delta.probabilities).filter(
        _._2 >= minPathProbability)
      z1
    }
    // I am not sure how this case could ever happen.
    // It mostly means the probabilities were incorrect
    if (path_pairs.isEmpty) {
      logWarning("No path pair was found")
      logWarning(delta.toString)
      None
    } else {
      // if some paths are backward, discard this paths
      val min_length = path_pairs.map(_._1.length).min
      // Find the aggregated status
      // If all the points have the same status, it is this one
      // null otherwise
      val first_point = delta.points.head
      val last_point = delta.points.last
      assert(first_point.time <= last_point.time)
      val agg_status = delta.points.find(p =>
        p.hired == null || (!p.hired.eq(first_point.hired))) match {
        case None => first_point.hired
        case Some(x) => null
      }
      if (min_length >= 0) {
        // The sum of all probability mass that got trhough
        // Use it as a normalization factor.
        // Should be > 0 (FIXME add a check here)
        val sum_prob = path_pairs.map(_._2).sum
        assert(!sum_prob.isNaN)
        if (sum_prob == 0) {
          logWarning("Sum prob is zero!")
          None
        } else {
          val output_path_pairs = if(sortByProbability) {
            path_pairs.sortBy(-_._2)
          } else {
            path_pairs
          }
          Some(delta.toPathInference.clone(
            output_path_pairs.map(z => z._1.toRoute),
            output_path_pairs.map(z => z._2 / sum_prob).toArray))
        }
      } else {
        logWarning(" Minimum length is negative: " + min_length)
        None
      }
    }
  }
}