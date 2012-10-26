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
import org.joda.time.LocalDate
import core_extensions.MMLogging
import core_extensions.TimeOrdering
import netconfig.Datum.ProbeCoordinate
import netconfig.io.Dates.parseDate
import netconfig.io.Dates.parseRange
import netconfig.io.files.PathInferenceViterbi
import netconfig.io.files.ProbeCoordinateViterbi
import netconfig.io.files.RawProbe
import netconfig.io.files.SerializedNetwork
import netconfig.io.files.TrajectoryViterbif
import netconfig.io.json.JSonSerializer
import netconfig.io.Serializer
import netconfig.storage.LinkIDRepr
import netconfig.Link
import network.gen.GenericLink
import network.gen.NetworkBuilder
import path_inference.crf.ComputingStrategy
import path_inference.manager.ProjectionHook
import path_inference.manager.ProjectionHookInterface
import path_inference.PathInferenceFilter
import path_inference.PathInferenceParameters2
import scopt.OptionParser
import netconfig.io.Dates
import scala.actors.Futures._
import netconfig.io.json.NetworkUtils

/**
 * Runs the path inference on some serialized data, using a generic network representation.
 *
 * It uses some default sensible parameters for best accuracy.
 *
 * mvn install
 *
 * Debugging procedure for the PIF
 *
 */
object RunPif extends MMLogging {

  def main(args: Array[String]) = {
    // All the options
    import Dates._
    var network_id: Int = -1
    var date: LocalDate = null
    var range: Seq[LocalDate] = Seq.empty
    var feed: String = ""
    var driver_id: String = ""
    var net_type: String = ""
    var num_threads: Int = 1
    var extended_info: Boolean = false
    var sort_time: Boolean = false
    val parser = new OptionParser("test") {
      intOpt("nid", "the net id", network_id = _)
      intOpt("num-threads", "the number of threads (the program will use one thread per day)", num_threads = _)
      opt("date", "the date", { s: String => { for (d <- parseDate(s)) { date = d } } })
      opt("range", "the date", (s: String) => for (r <- parseRange(s)) { range = r })
      opt("feed", "data feed", feed = _)
      opt("net-type", "The network type", net_type = _)
      opt("driver_id", "Runs the filter on the selected driver id", driver_id = _)
      booleanOpt("extended-info", "Adds additional (redundant) information in the output file. Useful for python.", extended_info = _)
      booleanOpt("resort-data", "sort the data by timestamp before sending it to the PIF", sort_time = _)
    }
    parser.parse(args)

    // DEBUG
    val parameters = pifParameters2()

    logInfo("Loading links...")
    var links: Seq[Link] = NetworkUtils.getLinks(network_id, net_type)

    val serialier: Serializer[Link] = NetworkUtils.getSerializer(links)

    logInfo("Building projector...")
    val projection_hook: ProjectionHookInterface = ProjectionHook.create(links, parameters)

    val date_range: Seq[LocalDate] = {
      if (date != null) {
        Seq(date)
      } else {
        range
      }
    }

    val drivers_whitelist = if (driver_id.isEmpty) {
      Set.empty[String]
    } else {
      driver_id.split(",").toSet
    }

    assert(!date_range.isEmpty, "You must provide a date or a range of date")

    logInfo("Number of selected dates: %s" format date_range.size)
    logInfo("Feed:" + feed)
    logInfo("Driver whitelist: %s" format drivers_whitelist.toString)
    logInfo("Presorting data: %s" format sort_time)

    val tasks = for (findex <- RawProbe.list(feed = feed, nid = network_id, dates = date_range)) yield {
      future { runPIF(projection_hook, serialier, net_type, parameters, findex, drivers_whitelist, extended_info, sort_time) }
    }

    for (task <- tasks) {
      task()
    }
    logInfo("Done")
  }

  def pifParameters() = {
    val params = new PathInferenceParameters2()
    params.fillDefaultFor1MinuteOffline
    params.setReturnPoints(true)
    params.setReturnRoutes(true)
    params.setMinPathProbability(0.1)
    params.setMinProjectionProbability(0.1)
    params.setShuffleProbeCoordinateSpots(true)
    params.setComputingStrategy(ComputingStrategy.Viterbi)
    params
  }


  def pifParameters2() = {
    val params = new PathInferenceParameters2()
    params.fillDefaultForHighFreqOnlineFast
    params.setReturnPoints(true)
    params.setReturnRoutes(true)
    params.setShuffleProbeCoordinateSpots(true)
    params
  }

  def runPIF(projector: ProjectionHookInterface, serializer: Serializer[Link], net_type: String,
    parameters: PathInferenceParameters2, file_index: RawProbe.FileIndex, drivers_whitelist: Set[String], extended_info:Boolean, sort_time:Boolean): Unit = {
    val fname_in = RawProbe.fileName(file_index)
    val fname_pcs = ProbeCoordinateViterbi.fileName(feed = file_index.feed,
      nid = file_index.nid,
      date = file_index.date,
      net_type = net_type)
    val fname_pis = PathInferenceViterbi.fileName(feed = file_index.feed,
      nid = file_index.nid,
      date = file_index.date,
      net_type = net_type)
    val fname_trajs = (vid: String, traj_idx: Int) =>
      TrajectoryViterbif.fileName(feed = file_index.feed, nid = file_index.nid, date = file_index.date,
        net_type = net_type, vid, traj_idx)

    assert((new File(fname_in)).exists())

    val writer_pi = serializer.writerPathInference(fname_pis, extended_info)
    val writer_pc = serializer.writerProbeCoordinate(fname_pcs, extended_info)
    logInfo("Opening data source: %s" format fname_in)
    val data = {
      val x = serializer.readProbeCoordinates(fname_in)
      if (sort_time) {
        x.toArray.sortBy(_.time).toIterable
      } else {
        x.toIterable
      }
    }
    val pif = PathInferenceFilter.createManager(parameters, projector)
    for (raw <- data) {
      val pc = raw
      if (drivers_whitelist.isEmpty || pc.id == null || drivers_whitelist.contains(pc.id)) {
        pif.addPoint(pc)
        for (out_pi <- pif.getPathInferences) {
          writer_pi.put(out_pi)
        }
        for (out_pc <- pif.getProbeCoordinates) {
          writer_pc.put(out_pc)
        }
      }
    }
    pif.finalizeManager
    for (out_pi <- pif.getPathInferences) {
      writer_pi.put(out_pi)
    }
    for (out_pc <- pif.getProbeCoordinates) {
      writer_pc.put(out_pc)
    }
    writer_pc.close()
    writer_pi.close()
    logInfo("Close data source: %s" format fname_in)
  }
}