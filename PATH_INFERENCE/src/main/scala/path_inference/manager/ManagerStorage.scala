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

import collection.mutable.Queue
import edu.berkeley.path.bots.netconfig.Datum._
import edu.berkeley.path.bots.netconfig.Link
import edu.berkeley.path.bots.netconfig.PathInferenceUtils
import edu.berkeley.path.bots.netconfig.ProbeCoordinateUtils
import path_inference.crf.CRFFrame
import path_inference.FilterOutputInterface
import path_inference.PathInferenceParameters2
import path_inference.output._

class InternalStorage(val parameters: PathInferenceParameters2)
  extends FilterOutputInterface {
  import PathInferenceUtils._
  import ProbeCoordinateUtils._
  private val paths_queue = {
    if (parameters.returnRoutes)
      Some(new Queue[PathInference[Link]])
    else None
  }

  private val points_queue = {
    if (parameters.returnPoints)
      Some(new Queue[ProbeCoordinate[Link]])
    else None
  }

  private val routetts_queue = {
    if (parameters.returnRouteTravelTimes)
      Some(new Queue[RouteTT[Link]])
    else None
  }

//  private val trajectories_queue = {
//    if (parameters.returnTrajectories)
//      Some(new Queue[Trajectory[Link]])
//    else None
//  }

  private val tspots_queue = {
    if (parameters.returnTimedSpots)
      Some(new Queue[TSpot[Link]])
    else None
  }

  private def emptyQueue[T: Manifest](q: Option[Queue[T]]): Array[T] = {
    q.get.dequeueAll(_ => true).toArray
  }

  def getProbeCoordinates = emptyQueue(points_queue)

  def getPathInferences = emptyQueue(paths_queue)

  def getRouteTTs = emptyQueue(routetts_queue)

//  def getTrajectories = emptyQueue(trajectories_queue)

  def getTSpots = emptyQueue(tspots_queue)

  def addFrame(frame: CRFFrame): Unit = {
    for (pi <- PathInferenceOuput.processFrame(frame, parameters)) {
      for (q <- paths_queue) {
        q += pi
      }
      for (
        q <- routetts_queue;
        rtt <- projectPathInferenceToRouteTT(pi)
      ) {
        q += rtt
      }
    }

    for (pc <- ProbeCoordinateOutput.processFrame(frame, parameters)) {
      for (q <- points_queue) {
        q += pc
      }
      for (
        q <- tspots_queue;
        tsp <- projectProbeCoordinateToTSpot(pc)
      ) {
        q += tsp
      }
    }
  }
}
