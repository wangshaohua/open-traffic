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

package edu.berkeley.path.bots.netconfig.Datum.storage
import edu.berkeley.path.bots.core.storage.TimeRepr
import edu.berkeley.path.bots.netconfig.storage.RouteRepr

case class PathInferenceRepr(
  val id: String,
  val startTime: TimeRepr,
  val endTime: TimeRepr,
  val routes: Seq[RouteRepr],
  val probabilities: Seq[Double]) {
}