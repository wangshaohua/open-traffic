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

package path_inference.manager

import edu.berkeley.path.bots.core.MMLogging
import collection.JavaConversions._
import edu.berkeley.path.bots.netconfig.projection.KDProjector
import edu.berkeley.path.bots.netconfig.Datum.ProbeCoordinate
import edu.berkeley.path.bots.netconfig.Link
import path_inference.PathInferenceParameters2

trait ProjectionHookInterface {
  def projectPoint(point: ProbeCoordinate[Link]): ProbeCoordinate[Link]

  def projectPointWithPreviousCoords(
    point: ProbeCoordinate[Link],
    previous_point: ProbeCoordinate[Link]): ProbeCoordinate[Link]
}

class EmptyHook extends ProjectionHookInterface {
  def projectPoint(point: ProbeCoordinate[Link]): ProbeCoordinate[Link] = point

  def projectPointWithPreviousCoords(
    point: ProbeCoordinate[Link],
    previous_point: ProbeCoordinate[Link]): ProbeCoordinate[Link] = point
}

/**
 * @param forwardTimeDelta in seconds
 * @author tjhunter
 */
class ProjectionHook(
  links: Seq[Link],
  max_returns: Int,
  radius: Double,
  grid_step: Double) extends MMLogging with ProjectionHookInterface {

  val projector = new KDProjector(links)

  def projectPoint(point: ProbeCoordinate[Link]): ProbeCoordinate[Link] = {
    val proj_spots = if (grid_step > 0) {
      projector.getGridProjection(point.coordinate, radius, max_returns, grid_step)
    } else {
      projector.getClosestLinks(point.coordinate, radius, max_returns)
    }
    val reprojected = point.reprojected(proj_spots)
    reprojected
  }

  /**
   * Compute the projected points of the spot.
   * The basic contract is that for each link, the resulting probe coordinate
   * will only go forward, never backward.
   */
  def projectPointWithPreviousCoords(
    point: ProbeCoordinate[Link],
    previous_point: ProbeCoordinate[Link]): ProbeCoordinate[Link] = {
    val proj_spots = if (grid_step > 0) {
      projector.getGridProjection(point.coordinate, radius, max_returns, grid_step)
    } else {
      // In the case of picking the most likely per link, we need to be a bit
      // more careful.
      val new_projs = projector.getClosestLinks(point.coordinate,
        radius, max_returns)
      val previous_by_link = previous_point.spots.groupBy(_.link)
      new_projs.map(sp => {
        previous_by_link.get(sp.link) match {
          case Some(other_sps) => {
            assert(other_sps.size == 1)
            val other_sp = other_sps.head
            if (other_sp.offset > sp.offset)
              other_sp
            else sp
          }
          case None => sp
        }
      })
    }
    val reprojected = point.reprojected(proj_spots)
    reprojected
  }
}

object ProjectionHook {
  def create(links: Seq[Link], parameters: PathInferenceParameters2): ProjectionHookInterface =
    new ProjectionHook(links, parameters.maxProjectionReturns,
      parameters.projectionRadius,
      parameters.projectionGridStep)
}
