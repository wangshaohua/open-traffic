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

import netconfig.Link
import core_extensions.MMLogging

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

/**
 * The ordering object.
 *
 * @author tjhunter
 */
class AStarOrdering(val heuristic: AStarHeuristic, val target: Link) extends Ordering[ArrayBuffer[Link]] {
  def compare(p1: ArrayBuffer[Link], p2: ArrayBuffer[Link]): Int =
    {
      val s = math.signum(-heuristic.estimatedCost(p1, target) + heuristic.estimatedCost(p2, target))
      s.toInt
    }
}

/**
 * Simple implementation of the A* algorithm.
 *
 * @author tjhunter
 */
abstract class AStarPathGenerator extends CachedPathGenerator2
  with MMLogging {
  /**
   * The maximum number of iterations allowed. One iteration
   * is one path expansion.
   */
  val maxIters: Int
  /**
   * The heuristic used in this case.
   */
  val heuristic: AStarHeuristic

  def getPathInCache(key: PathKey): Option[Array[Link]]

  def putPathInCache(key: PathKey, path: Array[Link]): Unit

  def getPathsInCache(key: PathKey): Option[Array[Array[Link]]]

  def putPathsInCache(key: PathKey, path: Array[Array[Link]]): Unit

  def getApproximatePathsCacheSize: Int

  def getApproximatePathCacheSize: Int

  private var total_queries = 0
  private var total_stored_paths = 0
  private var cache_misses = 0
  private val printMessageValue = 100000

  def computePath(key: PathKey): Array[Link] =
    {
      val paths = computePaths(key, 1, maxIters)
      if (paths.length == 1)
        paths(0)
      else
        null
    }

  /**
   * The actual A* algorithm is here
   *
   * @author tjhunter
   */
  def computePaths(key: PathKey,
    max_num_paths: Int,
    max_iters: Int): Array[Array[Link]] =
    {
      def path_str(path: Seq[Link]): String =
        {
          return "Path" + path.foldLeft("[")((s, l) => s + "," + l) + "]"
        }

      def queue_str(queue: Seq[Seq[Link]]): String =
        {
          return "Queue:\n" + queue.foldLeft("")((s, p) => s + ">>>" + path_str(p) + "\n")
        }

      val res = new ArrayBuffer[Seq[Link]]()
      val end = key.end_link

      val queue: PriorityQueue[ArrayBuffer[Link]] = new PriorityQueue[ArrayBuffer[Link]]()(new AStarOrdering(heuristic, end))

      val start = key.start_link
      val start_path: ArrayBuffer[Link] = ArrayBuffer.empty[Link] ++ List(start)
      queue += start_path

      var i = 0
      while (queue.length > 0 && res.length < max_num_paths && i < max_iters) {
        i += 1
        val path = queue.dequeue
        val last_link = path.last
        val all_plinks = Set.empty[Link] ++ path
        val out_links = last_link.outLinks
        for (out_link <- out_links) {
          val new_path = path ++ List(out_link)

          if (!all_plinks.contains(out_link)) {
            //          if (!out_link.sameGeometryAs(last_link) && !all_plinks.contains(out_link)) {
            if (out_link == key.end_link) {
              res += (new_path)
            } else
              queue += new_path
          }
        }
      }
      return res.map(_.toArray).toArray
    }

  def getShortestPath(start_link: Link, end_link: Link): Array[Link] =
    {
      val key = PathKey(start_link, end_link)

      var print_info = false
      var print_total_queries = 0
      var print_cache_misses = 0
      this.synchronized {
        total_queries += 1
        if (this.total_queries >= printMessageValue) {
          print_info = true
          print_total_queries = total_queries
          print_cache_misses = cache_misses
          total_queries = 0
          cache_misses = 0
        }
      }
      if (print_info) {
        // We do not care about being slightly off here.
        logInfo("Path cache: " + print_total_queries + " queries, " + print_cache_misses + " misses, elements in cache: " + getApproximatePathCacheSize)
      }

      //    (this.synchronized {pathCache.get(key)}) match
      getPathInCache(key) match {
        case Some(path) => path
        case None => {
          val path = computePath(key)
          putPathInCache(key, path)
          cache_misses += 1
          path
        }
      }
    }

  def getShortestPaths(start_link: Link, end_link: Link, max_num_paths: Int): Array[Array[Link]] =
    {
      var print_info = false
      var print_total_queries = 0
      var print_cache_misses = 0
      this.synchronized {
        total_queries += 1
        if (this.total_queries >= printMessageValue) {
          print_info = true
          print_total_queries = total_queries
          print_cache_misses = cache_misses
          total_queries = 0
          cache_misses = 0
        }
      }
      if (print_info) {
        // We do not care about being slightly off here.
        logInfo("Paths cache: " + print_total_queries + " queries, " +
            print_cache_misses + " misses, elements in cache: " +
            getApproximatePathsCacheSize + " , total stored paths: " +
            total_stored_paths)
      }

      val key = PathKey(start_link, end_link)
      //    (this.synchronized {pathsCache.get(key)}) match
      getPathsInCache(key) match {
        case Some(paths) => {
          for (p <- paths) {
            assert(p.forall(_ != null))
          }
          paths
        }
        case None => {
          val paths = computePaths(key, max_num_paths, maxIters)
          for (p <- paths) {
            assert(p.forall(_ != null))
          }
          putPathsInCache(key, paths)
          this.synchronized {
            cache_misses += 1
            total_stored_paths += paths.length
          }
          paths
        }
      }
    }
}

/**
 * The main class for the A* algorithm that users would probably want to use use.
 * All the default values are specified, except the heuristic which is really link-specific.
 * <p>
 * Warning: for now, this version keeps in memory all the paths computed so far,
 * which is not really optimal. If one wants something less memory hungry, one
 * should probably use another cache and create its own main class. It is easy.
 * <p>
 * Warning: reentrant (share nothing). A multithreaded application will probably
 * want to have concurrent caches instead.
 *
 * @author tjhunter
 */
class AStar(val heuristic: AStarHeuristic,
  val maxIters: Int)
  extends AStarPathGenerator {
  private val pathCache: ConcurrentMap[PathKey, Array[Link]] = (new MapMaker()).softValues().makeMap()
  private val pathsCache: ConcurrentMap[PathKey, Array[Array[Link]]] = (new MapMaker()).softValues().makeMap()

  def getPathInCache(key: PathKey): Option[Array[Link]] = {
    val res = pathCache.get(key)
    if (res == null)
      None
    else Some(res)
  }

  def putPathInCache(key: PathKey, path: Array[Link]): Unit = {
    pathCache.putIfAbsent(key, path)
  }

  def getPathsInCache(key: PathKey): Option[Array[Array[Link]]] = {
    val res = pathsCache.get(key)
    if (res == null) {
      None
    } else {
      Some(res)
    }
  }

  def putPathsInCache(key: PathKey, paths: Array[Array[Link]]): Unit = {
    pathsCache.putIfAbsent(key, paths)
  }

  def getApproximatePathsCacheSize: Int = pathsCache.size

  def getApproximatePathCacheSize: Int = pathCache.size
}

class ConcurrentAStar(heuristic: AStarHeuristic,
  maxIters: Int) extends AStar(heuristic, maxIters) with ConcurrentCachedPathGenerator2
