package netconfig_extensions.io

import scopt.OptionParser
import netconfig.Datum.ProbeCoordinate
import netconfig.Link
import collection.mutable.{Map=>MMap}
import core.Time
import core.Coordinate
import netconfig.Spot
import core_extensions.MMLogging
import netconfig_extensions.projection.ProjectorFactory
import netconfig.Network

/**
 * Imports a file containing probe data into the proper JSON structures and directories.
 * 
 * --nid 108 --feed telenav --file /windows/D/arterial_data/telenav/dailydump.csv.gz --format paulb --start-time "2012-04-29 16:30:00" --end-time "2012-04-29 17:30:00" --use-geo-filter true
 * 
 * mvn install
 * MAVEN_OPTS="-Xmx1g -verbose:gc -Dmm.data.dir=/windows/D/arterial_data" mvn exec:java -pl NETCONFIG -Dexec.mainClass="netconfig_extensions.io.ImportProbeData" -Dexec.args="--nid 108 --feed telenav --file /windows/D/arterial_data/telenav/dailydump.csv.gz --format paulb --start-time \"2012-04-29 16:30:00\" --end-time \"2012-04-29 17:30:00\" --use-geo-filter true"
 */
object ImportProbeData extends MMLogging {
  type ParseFun = String=>Option[ProbeCoordinate[Link]]
  
  def formatPaulB(line:String):Option[ProbeCoordinate[Link]] = {
    val elts = line.split(",")
    if (elts.length == 6) {
      val t_str = "%s.000" format elts(0)
      val t_opt = try { Some(Time.newTimeFromStringLikeBerkeleyDB(t_str)) } catch { case _ => 
        logWarning("Could not parse time string: %s" format elts(0))
        None }
      val id = elts(1)
      val speed_opt = try { Some(elts(2).toFloat) } catch { case _ => None }
      val heading_opt = try { Some(elts(3).toShort) } catch { case _ => None }
      val lng_opt = try { Some(elts(4).toDouble) } catch { case _ => None }
      val lat_opt = try { Some(elts(5).toDouble) } catch { case _ => None }
      for (time <- t_opt;
        lat <- lat_opt;
        lng <- lng_opt) {
          val c = new Coordinate(4326, lat, lng)
          val speed:java.lang.Float = if (speed_opt.isEmpty) null else speed_opt.get
          val heading:java.lang.Short = if (heading_opt.isEmpty) null else heading_opt.get
          return Some(new ProbeCoordinate(c, time, id, Array.empty[Spot[Link]], Array.empty[Double], speed, heading, null, null))
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
    var feed_name:String = ""
    var fname = ""
    var format = ""
    var geo_filter = false
    var start_time:Time = null
    var end_time:Time = null
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
    
    logInfo("start time:"+ start_time)
    logInfo("end time:"+ end_time)
    
    val parsing_fun:ParseFun = format match {
      case "paulb" => formatPaulB _
      case x => assert(false); null
    }
    val geo_filter_fun:ProbeCoordinate[Link]=>Option[ProbeCoordinate[Link]] = if (geo_filter) {
      val net = new Network(Network.MODEL_GRAPH, network_id)
      val max_lat = net.getLinks().map(_.getGeoMultiLine().getCoordinates().map(_.lat).max).max
      val max_lng = net.getLinks().map(_.getGeoMultiLine().getCoordinates().map(_.lon).max).max
      val min_lat = net.getLinks().map(_.getGeoMultiLine().getCoordinates().map(_.lat).min).min
      val min_lng = net.getLinks().map(_.getGeoMultiLine().getCoordinates().map(_.lon).min).min
      def f(x:ProbeCoordinate[Link]) = {
        if (x.coordinate.lat >= min_lat && x.coordinate.lat <= max_lat && x.coordinate.lon >= min_lng && x.coordinate.lon <= max_lng) {
          Some(x)
        } else None
      }
      f      
    } else {
      val f = (x:ProbeCoordinate[Link]) => Some(x)
      f
    }
    
    def time_filter_fun(pc:ProbeCoordinate[Link]):Option[ProbeCoordinate[Link]] = {
//      logInfo(pc.toString())
//      logInfo("start_time "+start_time)
      val c1 = (start_time == null) || (pc.time > start_time)
      val c2 = (end_time == null) || (pc.time < end_time)
      if (c1 && c2) Some(pc) else None
    }
    
    def fun(s:String):Option[ProbeCoordinate[Link]] = {
      for (x <- parsing_fun(s);
           y <- geo_filter_fun(x);
           z <- time_filter_fun(y)) {
        return Some(z)
      }
      None
    }
    
    mapFile(fname, network_id, feed_name, fun)
  }
  
  def mapFile(fname:String, network_id:Int, feed_name:String, parsing_fun:ParseFun):Unit = {
    logInfo("Opening file %s" format fname)
    val source = JSONSource.readLines(fname).map(parsing_fun)
    val files = MMap.empty[String,DataSink[ProbeCoordinate[Link]]]
    val serializer = new DummySerializer
    for (pc_opt <- source;
        pc <- pc_opt) {
      val localDate = Dates.fromBerkeleyTime(pc.time)
      val this_fname = RawProbe.fileName(feed=feed_name, nid=network_id, date=localDate)
      val sink = files.getOrElseUpdate(this_fname, serializer.writerProbeCoordinate(this_fname))
      sink.put(pc)
    }
    for (sink <- files.values) {
      sink.close()
    }
  }
}