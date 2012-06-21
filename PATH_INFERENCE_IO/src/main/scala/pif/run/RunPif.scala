package pif.run

import java.io.File
import org.joda.time.LocalDate
import core_extensions.MMLogging
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

  def getLinks(network_id: Int, source_name: String): Seq[Link] = {
    val fname = SerializedNetwork.fileName(network_id, source_name)
    val glrs = JSonSerializer.getGenericLinks(fname)
    val builder = new NetworkBuilder
    builder.build(glrs)
  }

  def getSerializer(network_id: Int, source_name: String, links: Seq[Link]): Serializer[Link] = {
    val map: Map[LinkIDRepr, Link] = Map.empty ++ links.map(l => {
      val linkId = l.asInstanceOf[GenericLink].idRepr
      (linkId, l)
    })
    JSonSerializer.from(map)
  }

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
    val parser = new OptionParser("test") {
      intOpt("nid", "the net id", network_id = _)
      intOpt("num-threads", "the number of threads (the program will use one thread per day)", num_threads = _)
      opt("date", "the date", { s: String => { for (d <- parseDate(s)) { date = d } } })
      opt("range", "the date", (s: String) => for (r <- parseRange(s)) { range = r })
      opt("feed", "data feed", feed = _)
      opt("net-type", "The network type", net_type = _)
      opt("driver_id", "Runs the filter on the selected driver id", driver_id = _)
    }
    parser.parse(args)

    val parameters = pifParameters()

    logInfo("Loading links...")
    var links: Seq[Link] = getLinks(network_id, net_type)
//    println(links.head.geoMultiLine())

    val serialier: Serializer[Link] = getSerializer(network_id, feed, links)

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

    logInfo("Selected dates: %s" format date_range.toString)
    logInfo("Feed:" + feed)
    logInfo("Driver whitelist: %s" format drivers_whitelist.toString)
    logInfo("Using %d thread(s)" format num_threads)

    val tasks = for (findex <- RawProbe.list(feed = feed, nid = network_id, dates = date_range)) yield {
      future {runPIF(projection_hook, serialier, parameters, findex, drivers_whitelist)}     
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

  def runPIF(projector: ProjectionHookInterface, serializer: Serializer[Link],
    parameters: PathInferenceParameters2, file_index: RawProbe.FileIndex, drivers_whitelist: Set[String]): Unit = {
    val fname_in = RawProbe.fileName(file_index)
    val fname_pcs = ProbeCoordinateViterbi.fileName(feed = file_index.feed,
      nid = file_index.nid,
      date = file_index.date,
      net_type = "navteq")
    val fname_pis = PathInferenceViterbi.fileName(feed = file_index.feed,
      nid = file_index.nid,
      date = file_index.date,
      net_type = "navteq")
    val fname_trajs = (vid: String, traj_idx: Int) =>
      TrajectoryViterbif.fileName(feed = file_index.feed, nid = file_index.nid, date = file_index.date,
        net_type = "navteq", vid, traj_idx)

    assert((new File(fname_in)).exists())

    val writer_pi = serializer.writerPathInference(fname_pis)
    val writer_pc = serializer.writerProbeCoordinate(fname_pcs)
    logInfo("Opening data source: %s" format fname_in)
    val data = serializer.readProbeCoordinates(fname_in)
    val pif = PathInferenceFilter.createManager(parameters, projector)
    for (raw <- data) {
      val pc = raw
//      println((drivers_whitelist.isEmpty,pc.id == null,drivers_whitelist.contains(pc.id)))
//      logInfo("pc: %s" format pc.toString())
      if (drivers_whitelist.isEmpty || pc.id == null || drivers_whitelist.contains(pc.id)) {
//        logInfo("Adding point: %s" format pc.toString())
        pif.addPoint(pc)
        for (out_pi <- pif.getPathInferences) {
//          logInfo("Output PI: %s" format out_pi.toString())
          writer_pi.put(out_pi)
        }
        for (out_pc <- pif.getProbeCoordinates) {
//          logInfo("Output PC: %s" format out_pc.toString())
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
  }
}