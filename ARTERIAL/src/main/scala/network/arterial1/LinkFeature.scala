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

package edu.berkeley.path.bots.network.arterial1

/**
 * Encodes a specific feature at the end of the link.
 */
sealed trait LinkFeature {
  def string: String
}

object Stop extends LinkFeature {
  def string = "stop"
}

object Light extends LinkFeature {
  def string = "signal"
}

object PedestrianCross extends LinkFeature {
  def string = "cross"
}

object NoFeature extends LinkFeature {
  def string = "nothing"
}

object LinkFeature {
  def decode(s: String): Option[LinkFeature] = s match {
    case "stop" => Some(Stop)
    case "signal" => Some(Light)
    case "cross" => Some(PedestrianCross)
    case "nothing" => Some(NoFeature)
    case _ => None
  }
}