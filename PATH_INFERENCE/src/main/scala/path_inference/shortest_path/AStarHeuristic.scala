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

import collection.mutable.ArrayBuffer
import collection.mutable.{ WeakHashMap => WHashMap }
import collection.mutable.PriorityQueue
import collection.JavaConversions._
import com.google.common.collect.MapMaker
import java.util.concurrent.ConcurrentMap

import edu.berkeley.path.bots.netconfig.Link
import edu.berkeley.path.bots.core_extensions.MMLogging

/**
 * The heuristic used in the A* algorithm.
 * <p>
 * The heuristic is assumed to be admissible.
 *
 * @author tjhunter
 */
trait AStarHeuristic {
  /**
   * The cost of the path discovered so far.
   */
  def cost(path: Seq[Link]): Double =
    {
      path.map(_.length).sum
    }

  /**
   * The expected cost for the rest of the travel. It has to be admissible.
   *
   * This cost is >= 0 and ==0 iff the last element of the path is equal to
   * the target.
   */
  def heuristicCost(path: Seq[Link], target: Link): Double

  /**
   * The complete estimated cost.
   */
  def estimatedCost(path: Seq[Link], target: Link): Double =
    {
      cost(path) + heuristicCost(path, target)
    }

}