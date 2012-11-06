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

package pif.run
import java.io.File
import scala.collection.JavaConversions.asScalaBuffer
import scala.collection.mutable.{ Map => MMap }
import scala.collection.mutable.Queue
import com.google.common.collect.ImmutableList
import org.joda.time.LocalDate
import scopt.OptionParser
import core_extensions.MMLogging
import core.Time
import netconfig.Datum.PathInference
import netconfig.Datum.ProbeCoordinate
import netconfig.io.Dates.parseDate
import netconfig.io.Dates.parseRange
import netconfig.io.files.PathInferenceViterbi
import netconfig.io.files.ProbeCoordinateViterbi
import netconfig.io.files.RouteTTViterbi
import netconfig.io.files.TSpotViterbi
import netconfig.io.files.TrajectoryViterbi
import netconfig.io.json.NetworkUtils
import netconfig.Datum.TrackPiece
import netconfig.Datum.TrackPiece.TrackPieceConnection
import netconfig.io.DataSink
import netconfig.io.Serializer
import netconfig_extensions.CollectionUtils.asImmutableList2
import netconfig_extensions.PathInferenceUtils
import netconfig_extensions.ProbeCoordinateUtils
import netconfig.Link
import netconfig.Route
import netconfig.io.Dates

/**
 * A stateful object that merges streams of ProbeCoordinate and PathInference objects belonging
 * to the same driver.
 */
class Merger[L <: Link](
  traj_fun: (String, Int) => DataSink[TrackPiece[L]],
  val vehicle: String) extends MMLogging {

  private var _traj_idx = -1
  private var _sink: DataSink[TrackPiece[L]] = null
  private var no_pc_so_far = true
  private val pcs = new Queue[ProbeCoordinate[L]]
  private val pis = new Queue[PathInference[L]]
  // The last time sent to the sink
  private var current_time: Time = null

  private def sink() = {
    if (_sink == null) {
      _traj_idx += 1
      _sink = traj_fun(vehicle, _traj_idx)
      no_pc_so_far = true
    }
    _sink
  }

  private def closeSink() {
    if (_sink != null) {
      _sink.close()
      _sink = null
      no_pc_so_far = true
    }
  }

  def numProbeCoordinates = pcs.size

  def numPathInferences = pis.size

  def addProbeCoordinate(pc: ProbeCoordinate[L]): Unit = {
    pcs += pc
    // Update as far as we can.
    while (update2()) {}
  }

  def addPathInference(pi: PathInference[L]): Unit = {
    pis += pi
    // Update as far as we can.
    while (update2()) {}
  }

  private def update2(): Boolean = {
    pcs.headOption.map(pc => {
      if (no_pc_so_far) {
        no_pc_so_far = false
        val firstConnections: ImmutableList[TrackPieceConnection] = Array.empty[TrackPieceConnection]
        val routes: ImmutableList[Route[L]] = ImmutableList.of[Route[L]]
        val secondConnections: ImmutableList[TrackPieceConnection] = Array.empty[TrackPieceConnection]
        val point = pc
        val tp = TrackPiece.from(firstConnections, routes, secondConnections, point)
        sink().put(tp)
        pcs.dequeue()
        true // Potentially more work.
      } else {
        // Try to find a PI that finishes at the pc.
        pis.headOption.map(pi => {
          if (pi.endTime < pc.time) {
//            // PI is younger than PC
//            // Should not happen.
//            // We missed a PC to start the PI.
//            // There is a disconnect that should not have happened.
//            logWarning("Missing a PC before this PI!\nPrevious time: %s\nCurrent PI:\n%s" format (current_time, pi.toString()))
            false
          } else if (pi.endTime == pc.time) {
            // We can create a new track piece here.
            // Assuming there is a single projection and
            // a single path (that should be relaxed at some point
            // but easier to reason with).
            assert(pc.spots.size == 1)
            assert(pi.routes.size == 1)
            val firstConnections = ImmutableList.of(new TrackPieceConnection(0, 0))
            val routes = ImmutableList.of(pi.routes.head)
            val secondConnections = ImmutableList.of(new TrackPieceConnection(0, 0))
            val point = pc
            val tp = TrackPiece.from(firstConnections, routes, secondConnections, point)
            sink.put(tp)
            pcs.dequeue()
            pis.dequeue()
            true // Potentially more progress possible
          } else {
            assert(pi.endTime > pc.time)
            // The PI is older than the current PC
            // This happens if there is a disconnect.
            // Remove the last point and stop the current trajectory.
            closeSink()
            true // Potentially more progress.
          }
        }).getOrElse(false) // Nothing to do if no PI
      }
    }).getOrElse(false) // Nothing to do if no PC
  }

//  def update_old(): Boolean = {
//    // Check if we received a PI object to attach to:
//    (pis.headOption, pcs.headOption) match {
//      case (Some(pi), Some(pc)) => {
//        // Checking invariants
//        // We require the time to go strictly forward.
//        // Some feeds have some peculiar points that get sent instantly.
//        if (pi.endTime == pi.startTime) {
//          logWarning("The following PI object has a 0 delta:\n%s" format pi.toString)
//          if (pc.time == pi.endTime) {
//            logWarning("Also discarding the following PC:\n%s" format pc.toString())
//          }
//          pis.dequeue()
//          pcs.dequeue()
//          true
//        } else {
//          if (current_time != null) {
//            assert(pi.startTime >= current_time)
//            assert(pc.time > current_time)
//          }
//          if (pc.time < pi.startTime) {
//            // There is a disconnect with a single point somewhere
//            // We drop this point for now.
//            logInfo("Dropping orphan point:\n%s" format pc.toString)
//            pcs.dequeue()
//            true
//          } else if (pc.time == pi.startTime) {
//            // There is disconnect in the trajectory.
//            // We start a new trajectory.
//            // We know this PC is not orphan since there is
//            // a PI right behind it.
//            closeSink()
//            val firstConnections: ImmutableList[TrackPieceConnection] = Array.empty[TrackPieceConnection]
//            val routes: ImmutableList[Route[L]] = ImmutableList.of[Route[L]]
//            val secondConnections: ImmutableList[TrackPieceConnection] = Array.empty[TrackPieceConnection]
//            val point = pc
//            val tp = TrackPiece.from(firstConnections, routes, secondConnections, point)
//            sink().put(tp)
//            pcs.dequeue()
//            //              logInfo("Dequeuing start point:\n%s" format pc.toString())
//            current_time = pc.time
//            true
//          } else {
//            // The PC is somewhere after the PI started.
//            // It cannot be during the PI
//            assert(pc.time >= pi.endTime)
//            if (pc.time == pi.endTime) {
//              if (current_time != null && pi.startTime == current_time) {
//                // We can create a new track piece here.
//                // Assuming there is a single projection and
//                // a single path (that should be relaxed at some point
//                // but easier to reason with).
//                assert(pc.spots.size == 1)
//                assert(pi.routes.size == 1)
//                val firstConnections = ImmutableList.of(new TrackPieceConnection(0, 0))
//                val routes = ImmutableList.of(pi.routes.head)
//                val secondConnections = ImmutableList.of(new TrackPieceConnection(0, 0))
//                val point = pc
//                val tp = TrackPiece.from(firstConnections, routes, secondConnections, point)
//                sink.put(tp)
//                pcs.dequeue()
//                pis.dequeue()
//                //                  logInfo("Dequeuing point and path:\n%s\n" format (pc.toString(), pi.toString()))
//                current_time = pc.time
//                true
//              } else {
//                // We missed a PC to start the PI.
//                // There is a disconnect that should not have happened.
//                logError("Missing a PC before this PI!\nPrevious time: %s\nCurrent PI:\n%s" format (current_time, pi.toString()))
//                false
//              }
//            } else if (pc.time > pi.endTime) {
//              // Ooops we lost a PC??
//              logError("Received a PC after a PI: \n %s\n===========\n%s\n" format (pc.toString(), pi.toString()))
//              assert(false)
//              false
//            } else {
//              // Error case already covered above.
//              assert(false)
//              false
//            }
//          }
//        }
//      }
//      case _ => {
//        // One of the queues is empty, nothing we can do
//        // for now.
//        false
//      }
//    }
//  }

  def finish(): Unit = {
    closeSink()
  }
}

/**
 * TODO(tjh) move to netconfig-io
 */
object MapDataGeneric extends MMLogging {

  val longHelp = """ Generic data mapper.
This class helps you convert the output of the Path Inference Filter to different other datatypes:
 - RouteTT objects (single routes between points)
 - TSpot objects (unique projection for each GPS points)
 - Track objects (pieces of connected trajectories)

Possible actions:
 - tspot
 - routett
 - traj

This assumes the network uses generic links.
"""

  def main(args: Array[String]) = {
    import Dates._
    // All the options
    var network_id: Int = -1
    var net_type: String = ""
    var actions: Seq[String] = Seq.empty
    var date: LocalDate = null
    var range: Seq[LocalDate] = Seq.empty
    var feed: String = ""
    var extended_info = false
    val parser = new OptionParser("test") {
      intOpt("nid", "the net id", network_id = _)
      opt("date", "the date", { s: String => { for (d <- parseDate(s)) { date = d } } })
      opt("range", "the date", (s: String) => for (r <- parseRange(s)) { range = r })
      opt("feed", "data feed", feed = _)
      opt("actions", "data feed", (s: String) => { actions = s.split(",") })
      opt("net-type", "The type of network", net_type = _)
      booleanOpt("extended-info",
        "Saves redundant info in the output, like geometry information (useful when interfacing with another language)",
        extended_info = _)
    }
    parser.parse(args)

    assert(!actions.isEmpty, "You must specify one action or more")
    assert(!net_type.isEmpty, "You must specify a network type or more")
    logInfo("Actions: " + actions.mkString(" "))
    logInfo("Network type: " + net_type)

    val date_range: Seq[LocalDate] = {
      if (date != null) {
        Seq(date)
      } else {
        range
      }
    }
    assert(!date_range.isEmpty, "You must provide a date or a range of date")
    logInfo("Date range: " + date_range)

    logInfo("About to start")
    logInfo("Actions: %s" format actions.mkString(" "))
    logInfo("Networks: %s" format net_type)

    val serializer = NetworkUtils.getSerializer(net_type, network_id)

    for (action <- actions) {
      action match {
        case "tspot" => for (fidx <- ProbeCoordinateViterbi.list(feed = feed, nid = network_id, net_type = net_type, dates = date_range)) {
          mapTSpot(serializer, net_type, fidx, extended_info)
        }
        case "traj" => for (fidx <- ProbeCoordinateViterbi.list(feed = feed, nid = network_id, net_type = net_type, dates = date_range)) {
          mapTrajectory(serializer, net_type, fidx, extended_info)
        }
        case "routett" => for (fidx <- PathInferenceViterbi.list(feed = feed, nid = network_id, net_type = net_type, dates = date_range)) {
          mapRouteTT(serializer, net_type, fidx, extended_info)
        }
        case _ => {
          logInfo("Unknown action " + action)
        }
      }
    }
    logInfo("Done")
  }

  def mapRouteTT[L <: Link](serializer: Serializer[L], net_type: String,
    findex: PathInferenceViterbi.FileIndex, extended_information: Boolean): Unit = {
    val fname_pis = PathInferenceViterbi.fileName(feed = findex.feed, nid = findex.nid,
      date = findex.date,
      net_type = net_type)
    if (!(new File(fname_pis)).exists()) {
      logInfo("File " + fname_pis + " does not exists, skipping.")
      return
    }
    logInfo("Opening for reading : " + fname_pis)
    val data = serializer.readPathInferences(fname_pis)

    val fname_pis_out = RouteTTViterbi.fileName(feed = findex.feed, nid = findex.nid,
      date = findex.date,
      net_type = net_type)
    logInfo("Opening for writing : " + fname_pis_out)
    val writer_pi = serializer.writerPathInference(fname_pis_out, extended_information)

    for (
      pi <- data;
      rtt <- PathInferenceUtils.projectPathInferenceToRouteTT(pi)
    ) {
      writer_pi.put(rtt.toPathInference)
    }
    writer_pi.close()
  }

  def mapTSpot[L <: Link](serializer: Serializer[L], net_type: String,
    findex: ProbeCoordinateViterbi.FileIndex, extended_information: Boolean): Unit = {
    val fname_pcs = ProbeCoordinateViterbi.fileName(feed = findex.feed, nid = findex.nid,
      date = findex.date,
      net_type = net_type)
    if (!(new File(fname_pcs)).exists()) {
      logInfo("File " + fname_pcs + " does not exists, skipping.")
      return
    }
    logInfo("Opening for reading : " + fname_pcs)
    val data = serializer.readProbeCoordinates(fname_pcs)

    val fname_pcs_out = TSpotViterbi.fileName(feed = findex.feed, nid = findex.nid,
      date = findex.date,
      net_type = net_type)
    logInfo("Opening for writing : " + fname_pcs_out)
    val writer_pc = serializer.writerProbeCoordinate(fname_pcs_out, extended_information)

    for (
      pc <- data;
      tsp <- ProbeCoordinateUtils.projectProbeCoordinateToTSpot(pc)
    ) {
      writer_pc.put(tsp.toProbeCoordinate)
    }
    writer_pc.close()
  }

  private[this] val numCountsPrint = 1000

  def mapTrajectory[L <: Link](serializer: Serializer[L], net_type: String,
    findex: ProbeCoordinateViterbi.FileIndex, extended_information: Boolean): Unit = {
    import findex._
    logInfo("Mapping traj: %s" format findex.toString())
    val mg_pcs = {
      val fname_pcs = ProbeCoordinateViterbi.fileName(feed = findex.feed, nid = findex.nid,
        date = findex.date,
        net_type = net_type)
      // This file may not exist.
      if (!(new File(fname_pcs)).exists()) {
        logInfo("File " + fname_pcs + " does not exists, skipping.")
        return
      }
      serializer.readProbeCoordinates(fname_pcs).iterator
    }
    val mg_pis = {
      val fname_pis = PathInferenceViterbi.fileName(feed = findex.feed, nid = findex.nid,
        date = findex.date,
        net_type = net_type)
      // This file may not exist.
      if (!(new File(fname_pis)).exists()) {
        logInfo("File " + fname_pis + " does not exists, skipping.")
        return
      }
      serializer.readPathInferences(fname_pis).iterator
    }
    def writer_trajs_fun(vehicle: String, traj_idx: Int) = {
      val fname_trajs_out = TrajectoryViterbi.fileName(feed, nid, date, net_type, vehicle, traj_idx)
      serializer.writerTrack(fname_trajs_out, extended_information)
    }
    logInfo("Created data sources")

    // Perform the merge
    val mergers = MMap.empty[String, Merger[L]]

    var counter = 0
    while (mg_pis.hasNext || mg_pcs.hasNext) {
      // TODO(tjh) collect the last seen time
      counter += 1
      if (counter >= numCountsPrint) {
        val num_mergers = mergers.size
        val num_inside_pis = mergers.values.map(_.numPathInferences).sum
        val num_inside_pcs = mergers.values.map(_.numProbeCoordinates).sum
        logInfo("%d mergers: %d total pcs, %d total pis" format (num_mergers, num_inside_pcs, num_inside_pis))
        counter = 0
      }
      if (mg_pis.hasNext) {
        val mg_pi = mg_pis.next()
        val vehicle_id = mg_pi.id
        val merger = mergers.getOrElseUpdate(vehicle_id, new Merger(writer_trajs_fun, vehicle_id))
        merger.addPathInference(mg_pi)
      }
      if (mg_pcs.hasNext) {
        val mg_pc = mg_pcs.next()
        val vehicle_id = mg_pc.id
        val merger = mergers.getOrElseUpdate(vehicle_id, new Merger(writer_trajs_fun, vehicle_id))
        merger.addProbeCoordinate(mg_pc)
      }
      // Do some cleanup
      // We assume that the values are written in block.
      // We add a timeout of 1 hour: if we have not seen a value for this id during the
      // previous hour, we assume it is stopped, and we flush the trajectory.
    }
    for (merger <- mergers.values) {
      merger.finish()
    }
  }
}