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
package netconfig_extensions

import edu.berkeley.path.bots.netconfig._
import edu.berkeley.path.bots.netconfig.Datum._
import collection.mutable.HashSet
import collection.JavaConversions._
import edu.berkeley.path.bots.netconfig.Link

/**
 * Some utility functions pertaining to links in general.
 */
object LinkUtils {

  /**
   * All the neighbouring links from a set of links.
   * @param links : the set of links to start from
   * @param depth the number of links to explore around
   */
  def getAllNeighboursFromLinks[L <: Link](links: Array[L], depth: Int)(implicit manifest: Manifest[L]): Array[L] = {
    val all_links = HashSet.empty[L]

    def sub_fun(l: L, d: Int): Unit = {
      if (d >= 0 && (!all_links.contains(l))) {
        all_links += l
        for (l1 <- l.inLinks) {
          sub_fun(l1.asInstanceOf[L], d - 1)
        }
        for (l1 <- l.outLinks) {
          sub_fun(l1.asInstanceOf[L], d - 1)
        }
      }
    }

    for (l <- links) {
      sub_fun(l, depth)
    }
    all_links.toArray
  }

  def getAllNeighboursFromLink[L <: Link](link: L, depth: Int)(implicit manifest: Manifest[L]): Array[L] = {
    getAllNeighboursFromLinks(Array(link), depth)
  }
}
