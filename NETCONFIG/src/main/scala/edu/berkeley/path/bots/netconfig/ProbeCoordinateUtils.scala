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
package edu.berkeley.path.bots.netconfig


import collection.JavaConversions._
import edu.berkeley.path.bots.core.MMLogging
import edu.berkeley.path.bots.netconfig.Link
import edu.berkeley.path.bots.netconfig.Datum.ProbeCoordinate
import edu.berkeley.path.bots.netconfig.Datum.TSpot
import edu.berkeley.path.bots.netconfig.Spot

object ProbeCoordinateUtils extends MMLogging {

  import SpotUtils._
  /**
   * If possible, extracts the most probable spot in the form of a coordinateT object.
   */
  def projectProbeCoordinateToTSpot[L <: Link](pc: ProbeCoordinate[L], prob_ratio: Double = 1.0): Option[TSpot[L]] = {
    // Do not forget case when init values are dummy negative values.
    if ((pc.probabilities.min <= - ProbeCoordinate.probabilityPrecision)) {
      logWarning("Invalid probability distribution (negative elements) " + pc)
      return None
    }

    val best_spot: Spot[L] = pc.getMostProbableSpot
    if (best_spot == null) {
      logInfo("Could not find best spot " + best_spot)
      return None
    }

    // Need to make sure this spot is really a dominant one
    if (pc.spots.length > 1) {
      val sorted_probs = pc.probabilities.toSeq.sorted.reverse.toArray
      val i = pc.getMostProbableIndex.intValue
      if (sorted_probs(0) < prob_ratio * sorted_probs(1)) {
        return None
      }
    }
    Some(new TSpot[L](best_spot, pc.id, pc.time, pc.hired, pc.speed))
  }
}
