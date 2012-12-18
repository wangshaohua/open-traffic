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
package edu.berkeley.path.bots.netconfig.projection
import edu.berkeley.path.bots.core.MMLogging



class KDTree(
  val segments: List[EuclidianSegment],
  val box: KDBox,
  val left: Option[KDTree] = None,
  val middle: Option[KDTree] = None,
  val right: Option[KDTree] = None) {

  def segmentsWithinRadius(p: EuclidianPoint, radius: Double): List[EuclidianSegment] = {
    if (!box.withinDistance(p, radius))
      return List.empty[EuclidianSegment]
    segments ++ {
      if (middle.isEmpty)
        List.empty[EuclidianSegment]
      else
        middle.get.segmentsWithinRadius(p, radius)
    } ++ {
      if (left.isEmpty)
        List.empty[EuclidianSegment]
      else
        left.get.segmentsWithinRadius(p, radius)
    } ++ {
      if (right.isEmpty)
        List.empty[EuclidianSegment]
      else
        right.get.segmentsWithinRadius(p, radius)
    }
  }

  def depth: Int = {
    val l_depth = if (left.isDefined) left.get.depth + 1 else 0
    val r_depth = if (right.isDefined) right.get.depth + 1 else 0
    val m_depth = if (middle.isDefined) middle.get.depth + 1 else 0
    return math.max(l_depth, math.max(r_depth, m_depth))
  }
}

object KDTree extends MMLogging {

  def build(segments: Array[EuclidianSegment]): KDTree = {
    build(segments, Horizontal)
  }

  def buildLeaf(segments: Array[EuclidianSegment]): KDTree = {
    val box = segments.map(new KDBox(_)).reduceLeft(_ union _)
    new KDTree(segments.toList, box)
  }

  def build(segments: Array[EuclidianSegment], o: KDOrientation): KDTree = {

    //    logInfo("build segments: "+segments.length+o)

    assert(segments.length > 0)

    if (segments.length == 1) {
      return new KDTree(segments.toList, new KDBox(segments.head))
    }

    val sorting_fun1 = if (o == Horizontal) {
      (es1: EuclidianSegment, es2: EuclidianSegment) => (es1.minX < es2.minX)
    } else {
      (es1: EuclidianSegment, es2: EuclidianSegment) => (es1.minY < es2.minY)
    }

    val sorted_segments = segments.sortWith(sorting_fun1)

    //    // If it is degenerated along a line, use the other coordinate to sort
    //    if(o == Horizontal) {
    //      if(sorted_segments.head.minX == sorted_segments.last.maxX) {
    //        val maxy = sorted_segments.map(_.maxY).max
    //        val miny = sorted_segments.map(_.minY).min
    //        assert(maxy <= miny)
    //      }
    //    }

    val middle_segment = sorted_segments((sorted_segments.length - 1) / 2)
    val (x, y) = (middle_segment.endX, middle_segment.endY)

    val strictly_on_left = if (o == Horizontal) {
      (es: EuclidianSegment) => (es.maxX <= x)
    } else {
      (es: EuclidianSegment) => (es.maxY <= y)
    }

    val strictly_on_right = if (o == Horizontal) {
      (es: EuclidianSegment) => (es.minX > x)
    } else {
      (es: EuclidianSegment) => (es.minY > y)
    }

    val in_the_middle = if (o == Horizontal) {
      (es: EuclidianSegment) => (es.maxX > x && es.minX <= x)
    } else {
      (es: EuclidianSegment) => (es.maxY > y && es.minY <= y)
    }

    // All the elements strictly on the left
    val left_elements = segments.filter(strictly_on_left)
    //    logInfo("left: "+left_elements.length)
    val right_elements = segments.filter(strictly_on_right)
    //    logInfo("right: "+right_elements.length)
    val middle_elements = segments.filter(in_the_middle)
    //    logInfo("middle: "+middle_elements.length)

    if (left_elements.length == segments.length
      || right_elements.length == segments.length
      || middle_elements.length == segments.length)
      return buildLeaf(segments)

    val left = {
      if (left_elements.isEmpty)
        None
      else
        Some(build(left_elements, o.flip))
    }

    val right = {
      if (right_elements.isEmpty)
        None
      else
        Some(build(right_elements, o.flip))
    }

    val middle = {
      if (middle_elements.isEmpty)
        None
      else
        Some(build(middle_elements, o.flip))
    }

    // Wicked functional programming
    val box: KDBox = (for (tree <- Array(left, middle, right) if !tree.isEmpty) yield {
      tree.get.box
    }).toSeq.reduceLeft(_ union _)

    new KDTree(List.empty[EuclidianSegment], box, left, middle, right)
  }
}
