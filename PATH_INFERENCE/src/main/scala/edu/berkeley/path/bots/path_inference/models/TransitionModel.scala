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

import path_inference.Path

/**
 * The transition model in the network.
 *
 * @author tjhunter
 */
/**
 * The interface for implementing a transition model on a road network.
 *
 * @author tjhunter
 */
trait TransitionModel {
  /**
   * Logarithm of the unnormalized transition probability.
   *
   * @param start_points the map matched start probe point. Must have at least
   * one matching spot.
   * @param end_points the map matched start probe point. Must have at least
   * one matching spot.
   * @param start_idx the index of the spot considered for this path. The
   * first link of the path is guaranteed to be the same as the link of this
   * spot.
   * @param end_idx the index of the end spot considered for this path. The
   * last link of the path is guaranteed to be the same as the link of this
   * spot
   * @param path a path in the network. It is guaranteed to be of positive
   * length..
   */
  def logTrans(path: Path): Double
}

object TransitionModel {
  def defaultModel: TransitionModel = {
    new SimpleFeatureModel(new FeatureTransitionModelParameters(Array(-1 / 10.0, 0.0)))
  }
}
