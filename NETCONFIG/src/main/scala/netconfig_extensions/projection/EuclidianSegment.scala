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

class EuclidianSegment(
  val startX: Double,
  val startY: Double,
  val endX: Double,
  val endY: Double,
  val reference: Any) {

  def this(start: EuclidianPoint, end: EuclidianPoint, ref: Any) = this(start.x, start.y, end.x, end.y, ref)

  val minX = if (startX <= endX) startX else endX
  val minY = if (startY <= endY) startY else endY
  val maxX = if (startX >= endX) startX else endX
  val maxY = if (startY >= endY) startY else endY

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

  override def toString = "EuclidianSegment[%f,%f,%f,%f]".format(startX, startY, endX, endY)

}
