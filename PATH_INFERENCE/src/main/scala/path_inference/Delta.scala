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
package path_inference

import edu.berkeley.path.bots.netconfig.Link
import edu.berkeley.path.bots.netconfig.Datum.ProbeCoordinate
import edu.berkeley.path.bots.netconfig.Datum.PathInference
import edu.berkeley.path.bots.netconfig_extensions.PathInferenceUtils

/**
 * A delta: a list of paths and a sequence of points.
 *
 * All the paths go from the first point to the last point. This is the basic
 * block of a trajectory.
 *
 * @author tjhunter
 */
class Delta(val points: Array[ProbeCoordinate[Link]],
  val paths: Array[Path]) {
  assert(points.length >= 2)

  val probabilities = paths.map(p => 1.0 / paths.length).toArray[Double]

  def toPathInference: PathInference[Link] = {
    // Will throw NC exception in case of backward path
    val routes = paths.map(_.toRoute)
    PathInferenceUtils.createPathInferenceFromProbeCoordinates(points,
      routes,
      probabilities)
  }

  override def toString: String =
    "Delta[" + points.head.id + "(" +
      points.head.time + "->" + points.last.time + ", " +
      points.length + " points, " + paths.length + " paths]\n" +
      (0 until paths.length).map(i => (
        "\t" + probabilities(i) + "\t" + paths(i) + "\n")).foldLeft("")(_ + _)
}
