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

import netconfig.Datum.ProbeCoordinate
import netconfig.Link
import path_inference.Delta
import path_inference.PathInferenceParameters2

trait ConditionalRandomField {

  def +=(point: ProbeCoordinate[Link]): Unit

  def +=(delta: Delta, point: ProbeCoordinate[Link]): Unit

  def finalizeComputations(): Unit

  def finalizeComputationsAndRestart(point: ProbeCoordinate[Link]): Unit

  def outputQueue(): Seq[CRFFrame]
}

/**
 * Exception triggered when the probability model fails.
 * (Which may happen because the parameters were too restrictive or the model was too agressive).
 *
 * Note: with the new implmentation in the log domain, it should
 * never happen.
 */
class InternalMathException extends Exception

