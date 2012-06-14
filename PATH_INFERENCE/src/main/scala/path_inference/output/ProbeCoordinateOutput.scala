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

import core_extensions.MMLogging
import collection.JavaConversions._
import netconfig.Datum.ProbeCoordinate
import netconfig.{ Link, Spot }
import path_inference.PathInferenceParameters2
import path_inference.crf.CRFFrame

object ProbeCoordinateOutput extends MMLogging {

  def processFrame(frame: CRFFrame, params: PathInferenceParameters2): Option[ProbeCoordinate[Link]] = {
    processFrame(frame, params.minProjectionProbability, params.shuffleProbeCoordinateSpots)
  }

  def processFrame(frame: CRFFrame, minProjectionProbability: Double,
    shuffleProbeCoordinateSpots: Boolean): Option[ProbeCoordinate[Link]] = {
    frame.payload match {
      case pc: ProbeCoordinate[_] => {
        // Sort the projections in decreasing order of probability, if required
        val pairs = if (shuffleProbeCoordinateSpots)
          (pc.probabilities zip pc.spots)
            .filter(_._1 >= minProjectionProbability)
            .toSeq
            .sortBy(z => (-(z._1)))
        else
          (pc.probabilities zip pc.spots).filter(_._1 >= minProjectionProbability)

        val sum_prob = pairs.map(_._1.doubleValue()).sum
        assert(sum_prob > 0)
        Some(pc.asInstanceOf[ProbeCoordinate[Link]].clone(pairs.map(_._2.asInstanceOf[Spot[Link]]).toArray,
          pairs.map(_._1 / sum_prob).toArray))
      }
      case _ => None
    }
  }
}