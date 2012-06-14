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

import collection.mutable.ArrayBuffer

object TSpotUtils extends MMLogging {
  import SpotUtils._

  def mapArrayTSpotToProbeCoordinate[L <: Link](tsps: Array[TSpot[L]]): Array[ProbeCoordinate[L]] =
    tsps.map(mapTSpotToProbeCoordinate(_))

  def mapTSpotToProbeCoordinate[L <: Link](tsp: TSpot[L]): ProbeCoordinate[L] =
    ProbeCoordinate.from(
      tsp.id,
      tsp.time,
      tsp.spot.toCoordinate,
      Array(tsp.spot),
      null,
      null,
      tsp.hired,
      null)

  /**
   * The coordinates, first col is lon, second column is lat
   */
  def getLatLonFromTSpots[L <: Link](tspots: Array[TSpot[L]]): Array[Array[Double]] = {
    tspots.map(ts => {
      val c = ts.spot.toCoordinate
      Array(c.lon, c.lat)
    }).toArray
  }

  def cutTSpotBagByDriver[L <: Link](tspots: Array[TSpot[L]]): Array[Array[TSpot[L]]] = {
    tspots.groupBy(_.id).values.map(_.toArray).toArray
  }

  /**
   * Removes off road links
   *
   * FIXME: the filter should not produce them in the first place...
   */
  def cleanTrace[L <: Link](tspots: Array[TSpot[L]], threshold: Int): Array[TSpot[L]] = {

    val res = new ArrayBuffer[TSpot[L]]
    val stage = new ArrayBuffer[TSpot[L]]
    for (tsp <- tspots) {
      if (!stage.isEmpty && stage.head.spot.link != tsp.spot.link) {
        if (stage.size >= threshold) {
          res ++= stage
        }
        stage.clear
      }
      stage += tsp
    }
    if (stage.size >= threshold)
      res ++= stage
    res.toArray
  }

  //  /**
  //   * Takes a trace of tspots and builds a trajectory out of it.
  //   */
  //  def getTrajectories[L <: Link](trace: Array[TSpot[L]], reconnect_distance: Double = -1.0): Array[Trajectory[L]] = {
  //    val builder = new TrajectoryBuilder[L](reconnect_distance)
  //    val res = new ArrayBuffer[Trajectory[L]]
  //
  //    def createTraj: Unit = {
  //      try {
  //        val traj = builder.createTrajectory
  //        val sp1 = traj.waypoints.head.spot.toCoordinate
  //        val sp2 = traj.waypoints.last.spot.toCoordinate
  //        if (sp1.distanceVincentyInMeters(sp2) > 1)
  //          res += traj
  //      } catch { case e => logInfo("Failed twice : " + e) }
  //      builder.reset
  //
  //    }
  //
  //    for (tsp <- trace) {
  //      try {
  //        builder.addPoint(tsp)
  //      } catch {
  //        case e: TrajectoryBuilder.EndOfPathException => {
  //          logInfo(e.getMessage)
  //          createTraj
  //        }
  //      }
  //    }
  //    createTraj
  //    logInfo(trace.length + " points in, " + res.map(_.waypoints.length).sum + " out")
  //    res.toArray
  //  }

}
