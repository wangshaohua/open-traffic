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

/**
 * @author tjhunter
 */

package path_inference.models
import netconfig.Datum.ProbeCoordinate
import netconfig.Link

/**
 * Transition model.
 * @author tjhunter
 */
/**
 * Observation model for gps points.
 *
 * This interface defines the representation of a model for generating gps
 * points from spots on the road.
 * @author tjhunter
 */
trait ObservationModel {
  /**
   * The vector of probabilities for the observed Coordinate given each
   * spot. (Unnormalized logarithm of probability)
   *
   * Computes the probability that each spot generated the observed coordinate.
   *
   * @param point a map-matched coordinate. The number of matching spots is positive, and the coordinate is non null. The other informations (driver ID,
   * speed, etc) may be null or undefined.
   */
  def logObs(point: ProbeCoordinate[Link]): Array[Double]
}

object ObservationModel {
  def defaultModel: ObservationModel = {
    new IsoGaussianObservationModel(15.0 * 15.0)
  }
}