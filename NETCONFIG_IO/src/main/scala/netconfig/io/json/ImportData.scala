package netconfig.io.json

import scala.collection.mutable.{ Map => MMap }
import core_extensions.MMLogging
import core.Coordinate
import core.Time
import netconfig.Datum.ProbeCoordinate
import netconfig.io.files.RawProbe
import netconfig.io.DataSink
import netconfig.io.Dates
import netconfig.io.Serializer
import netconfig.io.StringSource
import netconfig.storage.LinkIDRepr
import netconfig.NetconfigException
import netconfig.Spot
import scopt.OptionParser
import netconfig.Datum.storage.ProbeCoordinateRepr
import core.storage.TimeRepr
import core.storage.CoordinateRepresentation
import netconfig.io.StringDataSink
import com.codahale.jerkson.Json._
import netconfig.Datum.storage.ProbeCoordinateRepr
import netconfig.io.DataSinks

/**
 * Imports a file containing probe data into the proper JSON structures and directories.
 *
 * --nid 108 --feed telenav --file /windows/D/arterial_data/telenav/dailydump.csv.gz --format paulb --start-time "2012-04-29 16:30:00" --end-time "2012-04-29 17:30:00" --use-geo-filter true
 *
 * mvn install
 * MAVEN_OPTS="-Xmx1g -verbose:gc -Dmm.data.dir=/windows/D/arterial_data" mvn exec:java -pl NETCONFIG -Dexec.mainClass="netconfig_extensions.io.ImportProbeData" -Dexec.args="--nid 108 --feed telenav --file /windows/D/arterial_data/telenav/dailydump.csv.gz --format paulb --start-time \"2012-04-29 16:30:00\" --end-time \"2012-04-29 17:30:00\" --use-geo-filter true"
 */
object ImportProbeData extends MMLogging {
  type ParseFun = String => Option[ProbeCoordinateRepr]

  /**
   * Takes lines of a CSV file of the form:
   * Time[YYY-MM-DD HH:MM:SS],id[String],lat[Double],lon[Double],speed[Float],heading[Short],...
   */
  def formatCSV1(line: String): Option[ProbeCoordinateRepr] = {
    val elts = line.split(",")
    if (elts.length >= 6) {
      val t_opt = TimeRepr.berkeleyFromString(elts(0))
      val id = elts(1)
      val lng_opt = try { Some(elts(2).toDouble) } catch { case _ => None }
      val lat_opt = try { Some(elts(3).toDouble) } catch { case _ => None }
      val speed_opt = try { Some(elts(4).toDouble) } catch { case _ => None }
      val heading_opt = try { Some(elts(5).toDouble) } catch { case _ => None }
      for (
        time <- t_opt;
        lat <- lat_opt;
        lng <- lng_opt
      ) {
        val c = CoordinateRepresentation(Some(4326), lat, lng)
        return Some(ProbeCoordinateRepr(id, time, c, speed=speed_opt, heading=heading_opt))
      }
      logWarning("Failed to parse lat/lng in line: %s" format line)
      return None
    } else {
      logWarning("Failed to parse line (not enough tokens): %s" format line)
      None
    }
  }

  def main(args: Array[String]) = {
    import Dates._
    var network_id: Int = 0
    var feed_name: String = ""
    var fname = ""
    var format = ""
    var geo_filter = false
    var start_time: Time = null
    var end_time: Time = null
    val parser = new OptionParser("ImportProbeData") {
      intOpt("nid", "the network id corresponding to this dataset", network_id = _)
      opt("feed", "The network feed", feed_name = _)
      opt("file", "the name of the file", fname = _)
      opt("format", "The format of the file (formats currently supported:paulb)", format = _)
      booleanOpt("use-geo-filter", "Use the bounding box of the network to discard points outside the network area", geo_filter = _)
      opt("start-time", "(optional) start time filter in YYYY-MM-DD HH:MM:SS format", s => start_time = Time.newTimeFromStringLikeBerkeleyDB("%s.000" format s))
      opt("end-time", "(optional) start time filter in YYYY-MM-DD HH:MM:SS format", s => end_time = Time.newTimeFromStringLikeBerkeleyDB("%s.000" format s))
    }
    parser.parse(args)

    logInfo("start time:" + start_time)
    logInfo("end time:" + end_time)

    val parsing_fun: ParseFun = format match {
      case "csv1" => formatCSV1 _
      case x => assert(false); null
    }
    val geo_filter_fun: ProbeCoordinateRepr => Option[ProbeCoordinateRepr] = {
      val f = (x: ProbeCoordinateRepr) => Some(x)
      f
    }

    def time_filter_fun(pcr: ProbeCoordinateRepr): Option[ProbeCoordinateRepr] = {
      val t = TimeRepr.fromRepr(pcr.time)
      val c1 = (null == start_time) || (t > start_time)
      val c2 = (null == end_time) || (t < end_time)
      if (c1 && c2) Some(pcr) else None
    }

    def fun(s: String): Option[ProbeCoordinateRepr] = {
      for (
        x <- parsing_fun(s);
        y <- geo_filter_fun(x);
        z <- time_filter_fun(y)
      ) {
        return Some(z)
      }
      None
    }

    mapFile(fname, network_id, feed_name, fun)
  }

  def mapFile(fname: String, network_id: Int, feed_name: String, parsing_fun: ParseFun): Unit = {
    logInfo("Opening file %s" format fname)
    val source: Iterable[Option[ProbeCoordinateRepr]] = StringSource.readLines(fname).map(parsing_fun)
    val files = MMap.empty[String, DataSink[ProbeCoordinateRepr]]
    def f(pcr_ : ProbeCoordinateRepr):String = generate(pcr_)
    for (
      pcr_opt <- source;
      pcr <- pcr_opt
    ) {
      val localDate = Dates.fromBerkeleyTime(TimeRepr.fromRepr(pcr.time))
      val this_fname = RawProbe.fileName(feed = feed_name, nid = network_id, date = localDate)
      val sink = files.getOrElseUpdate(this_fname, {
        val ssink = StringDataSink.writeableZippedFile(this_fname)
        DataSinks.map(ssink, f _)
      })
      sink.put(pcr)
    }
    for (sink <- files.values) {
      sink.close()
    }
  }
}