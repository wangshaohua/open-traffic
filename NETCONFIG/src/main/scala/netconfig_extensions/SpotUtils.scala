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
import core.Time
import netconfig.Datum._
import core_extensions.MMLogging

object SpotUtils extends MMLogging {
  /**
   * Returns -1 if not found.
   */
  def findSpotInSpotSeq[L <: Link](sp: Spot[L], sps: Seq[Spot[L]]): Int = {
    sps.indexWhere(x => (sp.link == x.link && sp.lane == x.lane && sp.offset == x.offset))
  }

  /**
   * Tolerance allowed in the distance between two point projections.
   *
   * If the points are further than this distance, you can assume your
   * projection is wrong.
   */
  val PROJECTION_TOLERANCE = 10.0
}
