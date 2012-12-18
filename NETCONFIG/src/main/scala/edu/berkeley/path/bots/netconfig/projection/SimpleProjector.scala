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
package edu.berkeley.path.bots.netconfig.projection

import edu.berkeley.path.bots.core.Coordinate
import edu.berkeley.path.bots.netconfig.{ Link, Spot }
import edu.berkeley.path.bots.core.MMLogging
import collection.immutable.{ HashMap => IMap }
import math.Ordering

/**
 * Super slow (linear) implementation of a projector.
 *
 * Useful for testing.
 */
class SimpleProjection[L <: Link](val links: Seq[L])
  extends Projector[L] {

  def getCandidateSegmentsWithinRadius(c: Coordinate, radius: Double): Seq[Segment] = {
    segments.keys.toSeq
  }
}

