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
 * New, optimized version of the AStar algorithm much faster than the previous implementation.
 */
package path_inference.shortest_path

import collection.mutable.ArrayBuffer
import collection.mutable.PriorityQueue

import netconfig_extensions.projection.{ EuclidianPoint, EuclidianMapping }
import netconfig.Link
import core_extensions.MMLogging
import collection.JavaConversions._

/**
 * max_travel in meters.
 */
final class AStar2(
  private[this] val max_iters: Int,
  private[this] val max_travel: Double) extends PathGenerator2 {

  // Link, path length so far, heuristic cost of the path
  private[this]type LinkWithCosts = (Link, Double, Double)

  private[this] val ordering = new Ordering[List[LinkWithCosts]] {
    def compare(p1: List[LinkWithCosts], p2: List[LinkWithCosts]): Int = {
      math.signum(p2.head._3 - p1.head._3).toInt
    }
  }

  def path_str(path: Seq[Link]): String = {
    return "Path" + path.foldLeft("[")((s, l) => s + "," + l) + "]"
  }

  def queue_str(queue: Seq[Seq[Link]]): String = {
    return "Queue:\n" + queue.foldLeft("")((s, p) => s + ">>>" + path_str(p) + "\n")
  }

  def getShortestPath(start_link: Link, end_link: Link): Array[Link] = {
    val paths = getShortestPaths(start_link, end_link, 1)
    if (paths.length == 1)
      paths(0)
    else
      null
  }

  private[this] def distance(p1: EuclidianPoint, p2: EuclidianPoint): Double = {
    return math.sqrt((p1.x - p2.x) * (p1.x - p2.x) + (p1.y - p2.y) * (p1.y - p2.y))
  }

  private[this] def distance(l1: Link, l2: Link, em: EuclidianMapping): Double = {
    return distance(em.map(l1.geoMultiLine.getLastCoordinate), em.map(l2.geoMultiLine.getFirstCoordinate()))
  }

  def getShortestPaths(start_link: Link, end_link: Link, max_num_paths: Int): Array[Array[Link]] = {
    val res = new ArrayBuffer[Array[Link]]()
    val queue: PriorityQueue[List[LinkWithCosts]] = new PriorityQueue[List[LinkWithCosts]]()(ordering)

    val local_mapping = new EuclidianMapping(start_link.geoMultiLine.getLastCoordinate())

    queue += List((start_link, 0.0, distance(start_link, end_link, local_mapping)))

    var i = 0
    while ((!queue.isEmpty) && res.length < max_num_paths && i < max_iters) {
      i += 1
      val elems = queue.dequeue
      val heur_cost = elems.head._3
      val path_cost = elems.head._2
      assert(path_cost <= heur_cost)
      if (heur_cost <= max_travel) {
        // Discard all the outlinks that would create a loop in the path
        val all_plinks = Set.empty[Link] ++ elems.map(_._1)
        val last_link = elems.head._1
        last_link.outLinks.foreach(l => {
          if (!all_plinks.contains(l)) {
            if (l == end_link) {
              // Build the final path.
              // Do not forget to reverse the list.
              res += (l :: elems.map(_._1)).reverse.toArray
              if (res.length >= max_num_paths) {
                return res.toArray
              }
            } else {
              // Continue exploring
              val new_path_cost = l.length + path_cost
              val new_heur_cost = distance(l, end_link, local_mapping) + new_path_cost
              val new_elems = (l, new_path_cost, new_heur_cost) :: elems
              queue += new_elems
            }
          }
        })
      }
    }
    res.toArray
  }
}
