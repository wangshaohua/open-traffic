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

import netconfig.{ Link, Spot }
import core.{ Time, Coordinate }
import netconfig.Datum._
import core_extensions.MMLogging
import collection.JavaConversions._

object ProbeCoordinateUtils extends MMLogging {

  import SpotUtils._
  // The leading probability has to be twice the next one
  val prob_ratio = 1.0

  /**
   * If possible, extracts the most probable spot in the form of a coordinateT object.
   */
  def projectProbeCoordinateToTSpot[L <: Link](pc: ProbeCoordinate[L]): Option[TSpot[L]] = {
    // Do not forget case when init values are dummy negative values.
    if ((pc.probabilities.min <= -netconfig.Datum.ProbeCoordinate.probabilityPrecision)) {
      logWarning("Invalid probability distribution (negative elements) " + pc)
      return None
    }

    val best_spot: Spot[L] = pc.getMostProbableSpot
    if (best_spot == null) {
      logInfo("Could not find best spot " + best_spot)
      return None
    }

    // Need to make sure this spot is really a dominant one
    if (pc.spots.length > 1) {
      val sorted_probs = pc.probabilities.toSeq.sorted.reverse.toArray
      val i = pc.getMostProbableIndex.intValue
//      assert(sorted_probs.head == pc.probabilities.get(i))
      if (sorted_probs(0) < prob_ratio * sorted_probs(1)) {
        return None
      }
    }
    Some(new TSpot[L](best_spot, pc.id, pc.time, pc.hired, pc.speed))
  }

  def projectArrayProbeCoordinateToTSpot[L <: Link](pcs: Array[ProbeCoordinate[L]]): Array[TSpot[L]] = {
    pcs.flatMap(z => projectProbeCoordinateToTSpot(z)).toArray
  }

  def cutProbeCoordinatesByDriver[L <: Link](pcs: Array[ProbeCoordinate[L]]): Array[Array[ProbeCoordinate[L]]] = {
    val dic = pcs.groupBy(_.id)
    dic.keySet.toArray.sorted.map(id => (dic(id).sortBy(_.time.getTimeInMillis).toArray)).toArray
  }

  /**
   * Returns an array of (lon,lat).
   */
  def getLatLonFromProbeCoordinates[L <: Link](pcs: Array[ProbeCoordinate[L]]): Array[Array[Double]] = {

    pcs.map(pc => {
      val c = pc.coordinate
      Array(c.lon, c.lat)
    }).toArray
  }

  /**
   * Returns all the links that are referred by the spots of this ProbeCoordinate object.
   */
  def getAllLinksRelatedToProbeCoordinate[L <: Link](pc: ProbeCoordinate[L])(implicit manifest: Manifest[L]): Array[L] = {
    (Set.empty[L] ++ pc.spots.map(_.link)).toArray
  }

  /**
   * @see getAllLinksRelatedToProbeCoordinate
   */
  def getAllLinksRelatedToProbeCoordinates[L <: Link](pcs: Array[ProbeCoordinate[L]])(implicit manifest: Manifest[L]): Array[L] = {
    // One line...
    val res = (Set.empty[L] ++ pcs.map(_.spots.map(_.link)).flatten).toArray
    logInfo("res:" + res)
    res
  }

  /**
   * Convenience function for Java/Matlab/Python.
   *
   * Use this if you want to be quick and dirty, or if you use a scripting langugage (matlab, python).
   *
   * @see getAllLinksRelatedToProbeCoordinate
   */
  def getAllLinksRelatedToProbeCoordinate(pc: ProbeCoordinate[_ <: Link]) = getAllLinksRelatedToProbeCoordinate[Link](pc.asInstanceOf[ProbeCoordinate[Link]])

  /**
   * Searches for a particular tspot using a fast algorithm.
   * assumes pcs is sorted by time and is single driver.
   */
  def findTSpotInProbeCoordinates[L <: Link](tsp: TSpot[L], pcs: Array[ProbeCoordinate[L]]): Int = {
    var start = 0
    var end = pcs.length - 1
    while (start != end) {
      val pc1 = pcs(start)
      val pc2 = pcs(end)
      val d1 = math.abs(pc1.time - tsp.time)
      val d2 = math.abs(pc2.time - tsp.time)
      assert(pc1.time <= tsp.time)
      assert(pc2.time >= tsp.time)
      if (d1 < 0.2) {
        end = start
      }
      if (d2 < 0.2) {
        start = end
      } else {
        val middle = (start + end) / 2
        val pcm = pcs(middle)
        if (pcm.time >= tsp.time) {
          end = middle
        } else {
          start = middle
        }
      }
    }
    start
  }

  def findProbeCoordinateInTSpots[L <: Link](pc: ProbeCoordinate[L], tsps: Array[TSpot[L]]) = {
    var start = 0
    var end = tsps.length - 1
    while (start != end) {
      val pc1 = tsps(start)
      val pc2 = tsps(end)
      val d1 = math.abs(pc1.time - pc.time)
      val d2 = math.abs(pc2.time - pc.time)
      assert(pc1.time <= pc.time)
      assert(pc2.time >= pc.time)
      if (d1 < 0.2) {
        end = start
      }
      if (d2 < 0.2) {
        start = end
      } else {
        val middle = (start + end) / 2
        val pcm = tsps(middle)
        if (pcm.time >= pc.time) {
          end = middle
        } else {
          start = middle
        }
      }
    }
    start
  }

  def decimateProbeCoordinates[L <: Link](pcs: Seq[ProbeCoordinate[L]], decimation_rate: Int): Array[ProbeCoordinate[L]] = {
    pcs.grouped(decimation_rate).map(_.head).toArray
  }

  def decimateProbeCoordinates[L <: Link](
    pcs: Array[ProbeCoordinate[L]],
    decimation_rate: Int): Array[ProbeCoordinate[L]] = {
    decimateProbeCoordinates(pcs.toSeq, decimation_rate)
  }

  case class Discrepancy(val dt: Int, val count: Int, val maxJump: Float)

  def getTimingDiscrepancies[L <: Link](pcs: Seq[ProbeCoordinate[L]]): Array[Discrepancy] = {
    val x: Seq[(Float, Float)] = (pcs.dropRight(1) zip pcs.tail).map(z => {
      val (pc1, pc2) = z
      val d = pc1.coordinate.distanceVincentyInMeters(pc2.coordinate).toFloat
      (pc2.time - pc1.time, d)
    })
    val y = x.groupBy(_._1.toInt).toSeq
    y.map(z => {
      val (dt, arr) = z
      val max_jump = arr.map(_._2).max
      new Discrepancy(dt, arr.length, max_jump)
    }).sortBy(_.dt).toArray
  }

  def getTimingDiscrepancies[L <: Link](pcs: Array[ProbeCoordinate[L]]): Array[Discrepancy] = getTimingDiscrepancies(pcs.toSeq)

  def filterProbeCoordinatesBefore[L <: Link](pcs: Seq[ProbeCoordinate[L]], end_time: Time): Array[ProbeCoordinate[L]] = {
    pcs.filter(_.time < end_time).toArray
  }

  def filterProbeCoordinatesBefore[L <: Link](pcs: Array[ProbeCoordinate[L]], end_time: Time): Array[ProbeCoordinate[L]] =
    filterProbeCoordinatesBefore(pcs.toSeq, end_time)

  /**
   * Decimates and cuts the frequency based on the provided time values.
   */
  def decimateByTime[L <: Link](objs: Seq[ProbeCoordinate[L]], sample_rate: Double, window: Double): Array[Array[ProbeCoordinate[L]]] = {

    def splitByPoints(seq: List[ProbeCoordinate[L]], time_limit: Time): (List[ProbeCoordinate[L]], List[ProbeCoordinate[L]]) = {
      if (seq.isEmpty || seq.head.getTime > time_limit) {
        (List.empty[ProbeCoordinate[L]], seq)
      } else {
        val o = seq.head
        val next_time = o.getTime + (sample_rate - window).toFloat
        val next_time_limit = o.getTime + (sample_rate + window).toFloat
        val (ps, rest) = splitByPoints(seq.filter(_.getTime >= next_time), next_time_limit)
        (o :: ps, rest)
      }
    }

    def splitInternal(seq: List[ProbeCoordinate[L]]): List[List[ProbeCoordinate[L]]] = {
      if (seq.isEmpty) {
        List(seq)
      } else {
        val start_timeout = seq.head.getTime + window.toFloat
        val (current_track, rest) = splitByPoints(seq, start_timeout)
        current_track :: splitInternal(seq)
      }
    }
    splitInternal(objs.toList).map(_.toArray).toArray
  }

  /**
   * Remaps the points, one projection by link.
   * The most likely projection is also chosen on the corresponding link.
   */
  def decimateProjections[L <: Link](pc: ProbeCoordinate[L]): ProbeCoordinate[L] = {
    val projs = pc.spots
    val best_spot = pc.getMostProbableSpot
    val new_projs = pc.spots.groupBy(_.link).values.flatMap(sps => {
      val sorted_sps = sps.sortBy(_.toCoordinate.distanceVincentyInMeters(pc.coordinate))
      List(sorted_sps.head)
    })
    if (new_projs.exists(_ == best_spot)) {
      pc.reprojected(new_projs.toArray)
    } else {
      val all_projs = (new_projs ++ List(best_spot)).toArray
      pc.reprojected(all_projs)
    }
  }
}
