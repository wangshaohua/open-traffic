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

package path_inference.crf

import core.Time
// TODO: replace by array
import scalala.tensor.dense.DenseVector

/**
 * A frame contains all the informations required to build the output,
 * and all the information for performing the computations of an HMM
 *
 * @todo doc
 */
class CRFFrame(val payload: Any,
  val time: Time,
  val logObservations: DenseVector[Double],
  val forward: DenseVector[Double],
  val backward: DenseVector[Double],
  val posterior: DenseVector[Double],
  var sp_transition: Array[(Int, Int)]) {
}
