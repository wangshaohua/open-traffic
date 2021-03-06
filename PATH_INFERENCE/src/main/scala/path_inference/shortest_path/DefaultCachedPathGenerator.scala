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
import collection.JavaConversions._
import com.google.common.collect.MapMaker
import java.util.concurrent.ConcurrentMap
import core_extensions.MMLogging
import com.google.common.cache.CacheBuilder
import com.google.common.cache.Cache

final class DefaultCachedPathGenerator(
  private[this] val path_gen: PathGenerator2,
  private[this] val printMessageValue: Int = 100000,
  path_cache_size: Int) extends PathGenerator2 with MMLogging {

  val x: Cache[PathKey, Array[Link]] = (CacheBuilder.newBuilder()).maximumSize(path_cache_size).build()

  private[this] val pathCache: Cache[PathKey, Array[Link]] = (CacheBuilder.newBuilder()).maximumSize(path_cache_size).build()
  private[this] val pathsCache: Cache[PathKey, Array[Array[Link]]] = (CacheBuilder.newBuilder()).maximumSize(path_cache_size).build()

  private[this] var total_queries = 0
  private[this] var cache_misses = 0
  private[this] var num_paths_computed = 0

  def getPathInCache(key: PathKey): Option[Array[Link]] = {
    val res = pathCache.getIfPresent(key)
    if (res == null)
      None
    else Some(res)
  }

  def putPathInCache(key: PathKey, path: Array[Link]): Unit = {
    pathCache.put(key, path)
  }

  def getPathsInCache(key: PathKey): Option[Array[Array[Link]]] = {
    val res = pathsCache.getIfPresent(key)
    if (res == null) {
      None
    } else {
      Some(res)
    }
  }

  def putPathsInCache(key: PathKey, paths: Array[Array[Link]]): Unit = {
    pathsCache.put(key, paths)
  }

  def getApproximatePathsCacheSize: Int = pathsCache.size.toInt

  def getApproximatePathCacheSize: Int = pathCache.size.toInt

  def getShortestPath(start_link: Link, end_link: Link): Array[Link] = {
    val key = PathKey(start_link, end_link)
    var print_info = false
    var print_total_queries = 0
    var print_cache_misses = 0
    total_queries += 1
    if (this.total_queries >= printMessageValue) {
      print_info = true
      print_total_queries = total_queries
      print_cache_misses = cache_misses
      total_queries = 0
      cache_misses = 0
    }
    if (print_info) {
      // We do not care about being slightly off here.
      logInfo("Single path cache: " + print_total_queries +
        " queries, " + print_cache_misses + " misses, elements currently in cache: " +
        getApproximatePathsCacheSize + " , all path computations: " +
        num_paths_computed)
    }

    getPathInCache(key) match {
      case Some(path) => path
      case None => {
        val path = path_gen.getShortestPath(start_link, end_link)
        putPathInCache(key, path)
        cache_misses += 1
        path
      }
    }
  }

  def getShortestPaths(start_link: Link, end_link: Link, max_num_paths: Int): Array[Array[Link]] = {
    var print_info = false
    var print_total_queries = 0
    var print_cache_misses = 0
    total_queries += 1
    if (total_queries >= printMessageValue) {
      print_info = true
      print_total_queries = total_queries
      print_cache_misses = cache_misses
      total_queries = 0
      cache_misses = 0
    }
    if (print_info) {
      // We do not care about being slightly off here.
      logInfo("Paths cache: " + print_total_queries + " queries, " +
        print_cache_misses + " misses, elements currently in cache: " +
        getApproximatePathsCacheSize + " , all path computations: " +
        num_paths_computed)
    }

    val key = PathKey(start_link, end_link)
    getPathsInCache(key) match {
      case Some(paths) => paths
      case None => {
        val paths = path_gen.getShortestPaths(start_link, end_link, max_num_paths)
        putPathsInCache(key, paths)
        cache_misses += 1
        num_paths_computed += paths.length
        paths
      }
    }
  }

  override def finalizeOperations(): Unit = path_gen.finalizeOperations()
}
