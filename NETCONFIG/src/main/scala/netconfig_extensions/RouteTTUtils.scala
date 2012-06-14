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
import java.util.Collection

import collection.mutable.ArrayBuffer
import collection.JavaConversions._

object RouteTTUtils extends MMLogging {
  import RouteUtils._
  import PathInferenceUtils._

  def cutRouteTTsByDriver[L <: Link](rtts: Array[RouteTT[L]]): Array[Array[RouteTT[L]]] = {
    rtts.groupBy(_.id).values.map(_.toArray).toArray
  }

  def mapRouteTTToPathInference[L <: Link](rtt: RouteTT[L]): PathInference[L] = {
    PathInference.from(rtt.id,
      rtt.startTime,
      rtt.endTime,
      Array(rtt.route),
      Array(1.0),
      rtt.hired)
  }

  def mapArrayRouteTTToPathInference[L <: Link](rtts: Array[RouteTT[L]]): Array[PathInference[L]] =
    rtts.map(mapRouteTTToPathInference(_))

//  def projectArrayRouteTTToTrajectory[L <: Link](rtts: Array[RouteTT[L]], reconnect_distance: Double = -1.0): Array[Trajectory[L]] = {
//    val builder = new TrajectoryBuilder[L](reconnect_distance)
//    val res = new ArrayBuffer[Trajectory[L]]
//
//    def build = {
//      try {
//        val traj = builder.createTrajectory
//        res += traj
//      } catch { case e => logInfo("Second failure " + e) }
//
//    }
//
//    for (rtt <- rtts) {
//      try {
//        builder.addRouteTT(rtt)
//      } catch {
//        case e: TrajectoryBuilder.EndOfPathException => {
//          logInfo(e.getMessage)
//          build
//          builder.reset
//        }
//      }
//    }
//    build
//    logInfo(rtts.length + " rtts in, about " + res.map(_.waypoints.length - 1).sum + " out")
//    res.toArray
//
//  }

}
