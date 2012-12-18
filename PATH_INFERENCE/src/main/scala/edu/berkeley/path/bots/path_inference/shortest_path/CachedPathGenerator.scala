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

import edu.berkeley.path.bots.netconfig.Link
import collection.JavaConversions._
import com.google.common.collect.MapMaker
import java.util.concurrent.ConcurrentMap
import edu.berkeley.path.bots.core.MMLogging

trait CachedPathGenerator extends PathGenerator2 {
  /**
   * Caching methods
   */
  def getPathInCache(key: PathKey): Option[Array[Link]]

  def putPathInCache(key: PathKey, path: Array[Link]): Unit

  def getPathsInCache(key: PathKey): Option[Array[Array[Link]]]

  def putPathsInCache(key: PathKey, path: Array[Array[Link]]): Unit

  def getApproximatePathsCacheSize: Int

  def getApproximatePathCacheSize: Int
}
