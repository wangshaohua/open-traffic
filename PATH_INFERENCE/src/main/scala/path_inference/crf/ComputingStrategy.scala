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

object ComputingStrategy extends Enumeration {
  type ComputingStrategy = Value

  /**
   * Forward-looking strategy.
   * <p>
   * Very few memory and computation requirements, very small buffering,
   * but does not take into account any future information. It will be
   * confused by ramps for example.
   */
  val Online = Value("Online")

  val Offline = Value("Offline")

  val LookAhead1 = Value("LookAhead1")

  val LookAhead2 = Value("LookAhead2")

  val LookAhead3 = Value("LookAhead3")

  val LookAhead10 = Value("LookAhead10")

  val Window8 = Value("Window8")

  val Viterbi = Value("Viterbi")
}
