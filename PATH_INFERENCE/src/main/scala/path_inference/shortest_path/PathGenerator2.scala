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

package path_inference.shortest_path

import netconfig.Link
import path_inference.PathInferenceParameters2

/**
 * Main interface to implement path searching / discovering in a network graph.
 *
 * @todo the current interfaces returns Seq[L], add some java compatilibty layer
 * to return java lists?
 *
 * @author tjhunter
 */
trait PathGenerator2 {
  /**
   * Returns a sequence of link corresponding to the shortest path, or null if
   * it failed to find a path between the two links.
   * @note a failure does not imply that the graph is disconnected but only that
   * the path search finished before any result was found.
   */
  def getShortestPath(start_link: Link, end_link: Link): Array[Link]

  /**
   * Return the k shortest paths, with 0<=k<=max_num_paths.
   *
   * <p>
   * The contract of this interface is on best effort. There is no guarantee that
   * the paths returned are the shortest ones, and in fact an implementation
   * that always returns the empty list is valid.
   *
   * fixme Should not depend on max_num_paths, put this value in the config instead.
   */
  def getShortestPaths(start_link: Link, end_link: Link, max_num_paths: Int): Array[Array[Link]]
  /**
   * A signal for caches that need to perform some final operations before being
   * discarded.
   */
  def finalizeOperations(): Unit = {
    // Nothing to do by default.
  }
}

/**
 * Object and builder everyone should use.
 * No need to read further for nearly all cases.
 */
object PathGenerator2 {
  def getDefaultPathGenerator(parameters: PathInferenceParameters2): PathGenerator2 = {
    val max_distance_meters = 2400
    new DefaultCachedPathGenerator(new AStar2(parameters.maxSearchDepth, max_distance_meters))
    //     new AStar(new DefaultHeuristic, parameters.maxSearchDepth)
  }
}