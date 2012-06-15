package netconfig_extensions.io
import scala.annotation.elidable
import annotation.elidable.ASSERTION
import core_extensions.TimeOrdering
import core_extensions.MMLogging
import core.Time
import netconfig.DataType.Cabspotting
import netconfig.DataType.NavteqProbesFeed
import netconfig.DataType.ProbeCoordinateHelper
import netconfig.DataType.TeleNavProbesFeed
import netconfig.Datum.ProbeCoordinate
import netconfig.NavteqLink
import netconfig.Network
import scopt.OptionParser
import org.joda.time.LocalDate
import scala.collection.mutable.{ Map => MMap }

/**
 * Export the data from the database.
 *
 * Here is an example on how to use it with maven in the command line
 *
 * mvn install
 * MAVEN_OPTS="-Xmx1g -verbose:gc -Dmm.data.dir=/windows/D/arterial_data" mvn exec:java -pl NETCONFIG -Dexec.mainClass="netconfig_extensions.io.ExportData" -Dexec.args="--nid 1 --range 2012-1-8:2012-2-5 --max-records 2000 --feed navteq"
 *
 * @author tjhunter
 */
object ExportData extends MMLogging {
  import Dates._

  def helper(feed: ProbeCoordinateHelper, feed_name: String, network: Network, end_time: Time) = {
    val ser: Serializer[NavteqLink] = new NavteqSerializer(network, true)
    var current_writer: DataSink[ProbeCoordinate[NavteqLink]] = null
    var current_key: String = null

    val last_time_seen = MMap.empty[String, ProbeCoordinate[_]]
    var stop = false
    while (!stop) {
      val out = feed.getData()
      if (out == null) {
        stop = false
      } else {
        logInfo("Got %d elements" format out.length)
        // Not necessary? It should already be sorted.
        val sorted = {
          if (out.exists(_.time > end_time)) {
            stop = true
          }
          out.filter(_.time <= end_time).sortBy(_.time)
        }
        for (pc <- sorted) {
          last_time_seen.get(pc.id) match {
            case Some(old_pc) if old_pc.time > pc.time => {
              logError("""Discarding a point back in time:
Previous point:
%s
New point:
%s
=================""" format (old_pc.toString, pc.toString))
            }
            case Some(old_pc) if old_pc.time == pc.time => {
              logError("""Discarding a point at the same time:
Previous point:
%s
New point:
%s
=================""" format (old_pc.toString, pc.toString))
            }
            case _ => {
              val date = fromBerkeleyTime(pc.time)
              val key = dateStr(date)
              if (current_key == null || current_key != key) {
                if (current_key != null) {
                  current_writer.close()
                  current_key = null
                }
                current_key = key
                val fname = RawProbe.fileName(feed_name, network.nid, date)
                logInfo("Opening %s " format fname)
                current_writer = ser.writerProbeCoordinate(fname)
              }
              // FIXME: I should not have to cast here.
              current_writer.put(pc.asInstanceOf[ProbeCoordinate[NavteqLink]])

            }
          }
          last_time_seen += pc.id -> pc
        }
      }
    }

    if (current_key != null) {
      current_writer.close()
      current_key = null

    }

  }

  def main(args: Array[String]) = {
    import Dates._
    var network_id: Int = 0
    var range: Seq[LocalDate] = Seq.empty
    var feed_name: String = ""
    var num_points: Int = 1000
    val parser = new OptionParser("test") {
      intOpt("nid", "the net id", network_id = _)
      opt("feed", "The network feed", feed_name = _)
      intOpt("max-records", "The maximum number of records to extract at each time from teh database", num_points = _)
      opt("range", "The range of date", (s: String) => range = parseRange(s).get)
    }
    parser.parse(args)

    assert(!range.isEmpty, "Empty range of dates.")

    val net = new Network(Network.NetworkType.NAVTEQ, network_id)
    val db_items_limit = num_points
    val feed_fun = feed_name match {
      case "cabspotting" => (pd: LocalDate) => new Cabspotting(net, toBerkeleyTime(pd), db_items_limit toShort, 0.0 toFloat, 0, false)
      case "telenav" => (pd: LocalDate) => new TeleNavProbesFeed(net, toBerkeleyTime(pd), db_items_limit toShort, 0.0 toFloat, 0, false)
      case "navteq" => (pd: LocalDate) => new NavteqProbesFeed(net, toBerkeleyTime(pd), db_items_limit toShort, 0.0 toFloat, 0, false)
      case _ => { logError("Feed %s is not registered " format feed_name); (pd: LocalDate) => null }
    }

    val feed = feed_fun(range.head)
    val end_time = toBerkeleyTime(range.last)
    logInfo("Processing %s -> %s " format (dateStr(range.head), dateStr(range.last)))
    helper(feed, feed_name, net, end_time)
  }
}