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
import netconfig._
import netconfig.Datum._
//import netconfig.Datum.Trajectory.TrajectoryBuilder
import core_extensions.MMLogging
import collection.JavaConversions._
import collection.mutable.{ ArrayBuffer, Map }

object RouteUtils extends MMLogging {

  import SpotUtils._

  /**
   * Assumes a sequence has the predicate true true .. true false false ...
   * Finds the last true element
   * Assumes the sequence is non empty and starts with true.
   *
   * FIXME: move to some general utils place.
   */
  def binarySearch[A](a: IndexedSeq[A], predicate: (A) => Boolean) = {
    def recurse(low: Int, high: Int): Int = {
      assert(predicate(a(low)))
      if (predicate(a(high))) {
        high
      } else if (high == low + 1) {
        low
      } else {
        val mid = (low + high) / 2
        if (predicate(a(mid)))
          recurse(mid, high)
        else
          recurse(low, mid)
      }
    }
    recurse(0, a.size - 1)
  }

  /**
   * The number of links different between each r
   */
  def distanceRouteLinks[L <: Link](r1: Route[L], r2: Route[L]): Int = {
    val s1 = Set.empty[L] ++ r1.links
    val s2 = Set.empty[L] ++ r2.links
    s1.diff(s2).size + s2.diff(s1).size
  }

  def distanceRouteLinksWithoutEnds[L <: Link](r1: Route[L], r2: Route[L]): Int = {
    val s1 = Set.empty[L] ++ r1.links.drop(1).dropRight(1)
    val s2 = Set.empty[L] ++ r2.links.drop(1).dropRight(1)
    s1.diff(s2).size + s2.diff(s1).size
  }

  def distanceRouteLength[L <: Link](r1: Route[L], r2: Route[L]): Double = {

    var i1 = 0
    val l1 = r1.links.length
    val l2 = r2.links.length
    while (i1 < l1 && i1 < l2 && r1.links(i1) == r2.links(i1))
      i1 += 1
    var i2 = l1 - 1
    while (i2 >= 0 && i2 >= (l1 - l2) && r1.links(i2) == r2.links(i2 + l2 - l1))
      i2 -= 1
    val d1 = (i1 to i2).map(r1.links(_).length).sum.toDouble
    val d2 = (i1 to i2 + (l2 - l1)).map(r2.links(_).length).sum.toDouble
    return math.max(d1, d2)
  }

  /**
   * How much r1 not covers of r2
   */
  def coverageRoute[L <: Link](r1: Route[L], r2: Route[L])(implicit manifest: Manifest[L]): Double = {
    val inner1 = Set.empty[L] ++ r1.links.drop(1).dropRight(1)
    val inner2 = Set.empty[L] ++ r2.links.drop(1).dropRight(1)
    val headl2 = r2.links.head
    val headl1 = r1.links.head
    val lastl1 = r1.links.last
    val lastl2 = r2.links.last
    if (r2.links.length == 1 && r1.links.length >= 2)
      return coverageRoute(r2, r1)
    // Start and end at the same link
    if (r1.links.length == 1) {
      val l = headl1

      if (inner2.contains(l))
        return r1.endOffset - r1.startOffset

      if (r2.links.length == 1 && l == headl2) {
        val start_offset = math.max(r1.startOffset, r2.startOffset)
        val end_offset = math.min(r1.endOffset, r2.endOffset)
        return math.max(end_offset - start_offset, 0.0)
      }

      if (l == headl2) {
        val start_offset = math.max(r1.startOffset, r2.startOffset)
        val end_offset = r1.endOffset
        return math.max(end_offset - start_offset, 0.0)
      }
      if (l == lastl2) {
        val start_offset = r1.startOffset
        val end_offset = math.min(r1.endOffset, r2.endOffset)
        return math.max(end_offset - start_offset, 0.0)
      }

      return 0.0
    }

    var res = 0.0
    // The inner links
    // The number of common inner links, with countings
    val r1_map = Map.empty[L, Int]
    for (l <- r1.links.drop(1).dropRight(1)) {
      if (r1_map.contains(l))
        r1_map(l) += 1
      else
        r1_map += l -> 1
    }

    val r2_map = Map.empty[L, Int]
    for (l <- r2.links.drop(1).dropRight(1)) {
      if (r2_map.contains(l))
        r2_map(l) += 1
      else
        r2_map += l -> 1
    }

    for ((l1, i1) <- r1_map) {
      val i2 = r2_map.getOrElse(l1, 0)
      res += math.min(i1, i2) * l1.length
    }

    // The starts and ends are processed using the 1-link logic
    val start_r1 = Route.from(Seq(headl1), r1.startOffset, headl1.length)
    res += coverageRoute(start_r1, r2)

    val end_r1 = Route.from(Seq(lastl1), 0, r1.endOffset)
    res += coverageRoute(end_r1, r2)

    if (inner1.contains(headl2)) {
      val start_r2 = Route.from(Seq(headl2), r2.startOffset, headl2.length)
      res += coverageRoute(start_r2, r1)
    }

    if (inner1.contains(lastl2)) {
      val end_r2 = Route.from(Seq(lastl2), 0, r2.endOffset)
      res += coverageRoute(end_r2, r1)
    }
    res
  }

//  def convertRouteToList[L <: Link](r: Route[L]): Array[(L, Double, Double)] = {
//    if (r.links.length == 1) {
//      Array((r.links.head, r.startOffset, r.endOffset))
//    } else {
//      (List((r.links.head, r.startOffset, r.links.head.length)) :::
//        (r.links.drop(1).dropRight(1).toList.map(l => (l, 0.0, l.length))) :::
//        List((r.links.last, 0.0, r.endOffset))).toArray
//    }
//  }

  /**
   * Takes a route and segments it into a sequence of NT link / start offset / end offset
   */
  def convertRouteToSegments[L <: Link](r: Route[L]): Seq[(L, Double, Double)] =
    {
      // Conversion to a vector to use the (cleaner) collection syntax
      val links: Vector[L] = Vector.empty[L] ++ r.links
      if (links.length == 1)
        List((links.head, r.startOffset, r.endOffset))
      else {
        List((links.head, r.startOffset.toDouble, links.head.length.toDouble)) ++
          links.slice(1, links.length).map(l => (l, 0.0, l.length.toDouble)) ++
          List((links.last, 0.0, r.endOffset.toDouble))
      }
    }

}
