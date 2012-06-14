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
package netconfig_extensions.projection

trait KDOrientation {
  def flip: KDOrientation
}

object Vertical extends KDOrientation {
  def flip = Horizontal
}

object Horizontal extends KDOrientation {
  def flip = Vertical
}

class KDBox(val minX: Double,
  val minY: Double,
  val maxX: Double,
  val maxY: Double) {

  def this(seg: EuclidianSegment) = this(seg.minX, seg.minY, seg.maxX, seg.maxY)

  def withinDistance(p: EuclidianPoint, radius: Double): Boolean = {
    // If in the box
    if (p.x >= minX - radius
      && p.x <= maxX + radius
      && p.y >= minY - radius
      && p.y <= maxY + radius) {
      // Perform projection to find the closest point
      // TODO: perform projection to make sure we can refine here
      true
    } else
      false
  }

  def union(other: KDBox): KDBox = new KDBox(math.min(minX, other.minX),
    math.min(minY, other.minY),
    math.max(maxX, other.maxX),
    math.max(maxY, other.maxY))

}

