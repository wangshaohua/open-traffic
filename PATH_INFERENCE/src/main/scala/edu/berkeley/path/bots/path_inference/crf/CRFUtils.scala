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

import collection.mutable.ArrayBuffer
import collection.JavaConversions._
import path_inference.Delta
import edu.berkeley.path.bots.netconfig.Datum.ProbeCoordinate
import edu.berkeley.path.bots.netconfig.Link

private[path_inference] object CRFUtils {
  def findTransitionsDeltaToPC2(delta: Delta, point: ProbeCoordinate[Link]): Array[(Int, Int)] = {
    // This operation needs to happen super fast with when using gridded projection
    // or lots of paths, so the implementation is careful to have close
    // to linear runtime.
    // This is basically a custom hash+merge join implmentation
    // Take all the paths and regroup them through their end link
    val paths_by_link = (delta.paths.zipWithIndex).groupBy(z => {
      val (path, idx) = z
      path.links.last
    })
    // Regroup the spots by their link
    val spots_by_link = point.spots.zipWithIndex.groupBy(z => {
      val (spot, spidx) = z
      spot.link
    })
    // Go on each link
    val res = new ArrayBuffer[(Int, Int)]
    spots_by_link.values.flatMap(spots => {
      val link = spots.head._1.link
      paths_by_link.get(link) match {
        case None => None
        case Some(p_by_ls) => {
          // Sort the paths by increasing start offset
          val compatible_paths = p_by_ls.sortBy(_._1.end_offset).toArray
          // Sort the spots by increasing offset and convert to array
          val sorted_spots = spots.sortBy(_._1.offset).toArray
          // Now do a sorted merge sort

          var sp_idx = 0
          var p_idx = 0
          while (sp_idx < sorted_spots.length) {
            val (spot, spidx) = sorted_spots(sp_idx)
            // Move forward the index on the paths
            while (p_idx < compatible_paths.length && compatible_paths(p_idx)._1.end_offset < spot.offset) {
              p_idx += 1
            }
            // Copy all the indexes that are compatible
            while (p_idx < compatible_paths.length && compatible_paths(p_idx)._1.end_offset == spot.offset) {
              val (path, pidx) = compatible_paths(p_idx)
              if (path.compatibleEnd(spot))
                res += pidx -> spidx
              p_idx += 1
            }
            // Increment the spot counter to monve to the next point
            sp_idx += 1
          }
          res
        }
      }
    }).toArray
  }

  def findTransitionsPCToDelta(point: ProbeCoordinate[Link], delta: Delta): Array[(Int, Int)] = {
    // This operation needs to happen super fast with when using gridded projection
    // or lots of paths, so the implementation is careful to have close
    // to linear runtime.
    // This is basically a custom hash+merge join implmentation
    // Take all the paths and regroup them through their end link
    val paths_by_link = (delta.paths.zipWithIndex).groupBy(z => {
      val (path, idx) = z
      path.links.head
    })
    // Regroup the spots by their link
    val spots_by_link = point.spots.zipWithIndex.groupBy(z => {
      val (spot, spidx) = z
      spot.link
    })
    // Go on each link
    spots_by_link.values.flatMap(spots => {
      val link = spots.head._1.link
      paths_by_link.get(link) match {
        case None => None
        case Some(p_by_ls) => {
          // Sort the paths by increasing start offset
          val compatible_paths = p_by_ls.sortBy(_._1.start_offset).toArray
          // Sort the spots by increasing offset and convert to array
          val sorted_spots = spots.sortBy(_._1.offset).toArray
          // Now do a sorted merge sort
          val res = new ArrayBuffer[(Int, Int)]
          var sp_idx = 0
          var p_idx = 0
          while (sp_idx < sorted_spots.length) {
            val (spot, spidx) = sorted_spots(sp_idx)
            // Move forward the index on the paths
            while (p_idx < compatible_paths.length && compatible_paths(p_idx)._1.start_offset < spot.offset) {
              p_idx += 1
            }
            // Copy all the indexes that are compatible
            while (p_idx < compatible_paths.length && compatible_paths(p_idx)._1.start_offset == spot.offset) {
              val (path, pidx) = compatible_paths(p_idx)
              if (path.compatibleStart(spot)) {
                res += spidx -> pidx
              }
              p_idx += 1
            }
            // Increment the spot counter to monve to the next point
            sp_idx += 1
          }
          res
        }
      }
    }).toArray
  }

}
