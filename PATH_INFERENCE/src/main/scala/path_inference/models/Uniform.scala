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
import path_inference.Path

class UniformTransitionModel extends TransitionModel {
  def logTrans(path: Path): Double = 0.0

}

class UniformObservationModel extends ObservationModel {
  def logObs(point: ProbeCoordinate[Link]): Array[Double] =
    {
      (0 until point.probabilities.size).map(i => 0.0).toArray
    }

}