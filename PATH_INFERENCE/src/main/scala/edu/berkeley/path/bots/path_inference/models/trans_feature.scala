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

import math.abs
import edu.berkeley.path.bots.netconfig.Datum.ProbeCoordinate
import edu.berkeley.path.bots.netconfig.Link
import path_inference.Path
import reflect.BeanProperty

case class FeatureTransitionModelParameters(val weigths: Array[Double]) {
  def setNewValue(w: Array[Double]) {
    for (i <- 0 until w.length)
      weigths(i) = w(i)
  }
}

/**
 * A feature-based transition model.
 */
abstract class FeatureTransitionModel(@BeanProperty val parameters: FeatureTransitionModelParameters) extends TransitionModel {
  def numFeatures = parameters.weigths.length

  def logTrans(path: Path): Double =
    {
      val phi = featureVector(path)
      var res = 0.0
      for (i <- 0 until phi.length) {
        res += phi(i) * parameters.weigths(i)
      }
      res
    }

  def featureVector(path: Path): Array[Double]
}

class SimpleFeatureModel(parameters: FeatureTransitionModelParameters) extends FeatureTransitionModel(parameters) {
  def featureVector(path: Path): Array[Double] =
    {
      val is_snap_point = path.end_offset >= path.links.last.length - 1e-1

      return Array(if (path.length > 0) path.length else (-5 * path.length), 0.0)
    }
}

class BasicFeatureModel(parameters: FeatureTransitionModelParameters) extends FeatureTransitionModel(parameters) {
  def featureVector(path: Path): Array[Double] =
    {
      return List(abs(path.length)).toArray
    }
}