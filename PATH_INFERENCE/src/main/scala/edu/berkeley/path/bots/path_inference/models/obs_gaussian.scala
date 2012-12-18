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
import edu.berkeley.path.bots.netconfig.Datum.ProbeCoordinate
import edu.berkeley.path.bots.netconfig.Link

case class IsoGaussianObservationParameters(var variance: Double)

class IsoGaussianObservationModel(val parameters: IsoGaussianObservationParameters) extends ObservationModel {

  def this(variance: Double) = this(new IsoGaussianObservationParameters(variance))

  def features(point: ProbeCoordinate[Link]): Array[Double] = {
    val num_spots = point.spots.size
    val res = new Array[Double](num_spots)
    (0 until num_spots).map(spidx => {
      val spot = point.spots.get(spidx)
      val c = spot.toCoordinate
      val d = point.coordinate.distanceDefaultMethodInMeters(spot.toCoordinate)
      -0.5 * d * d
    }).toArray
  }

  def logObs(point: ProbeCoordinate[Link]): Array[Double] =
    {
      val num_spots = point.spots.size
      val res = new Array[Double](num_spots)
      for (spidx <- 0 until num_spots) {
        val spot = point.spots.get(spidx)
        val c = spot.toCoordinate
        val d = point.coordinate.distanceDefaultMethodInMeters(spot.toCoordinate)
        res(spidx) = -0.5 * d * d / parameters.variance
      }
      res
    }
}
