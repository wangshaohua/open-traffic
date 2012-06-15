/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package netconfig_extensions.io

import netconfig.Network
import netconfig.Link
import netconfig.Spot
import netconfig.Route
import netconfig.Datum.ProbeCoordinate
import netconfig.Datum.PathInference
import core.Coordinate
import core.Time

import java.io.FileReader
import java.io.FileInputStream
import java.io.InputStreamReader
import java.util.{Collection => JCollection}
import java.util.zip.GZIPInputStream
import org.codehaus.jackson.JsonNode
import org.codehaus.jackson.JsonFactory
import org.codehaus.jackson.map.ObjectMapper
import org.codehaus.jackson.node.JsonNodeFactory
import org.codehaus.jackson.node.ObjectNode
import org.codehaus.jackson.node.ArrayNode
import scala.collection.JavaConversions._

/**
 * This is the main interface for the serializer which users should be using.
 * FIXME add some documentation.
 * Use of the concrete classes (ArterialSerializer, NavteqSerializer, ModelGraphSerializer)
 * for an implementation.
 */
trait Serializer[L<:Link] {
  def readTrack(fname: String): Iterable[TrackPiece[L]] 

  def readProbeCoordinates(fname: String): Iterable[ProbeCoordinate[L]] 
  
  def readPathInferences(fname: String): Iterable[PathInference[L]] 
  
  def writerProbeCoordinate(fname: String): DataSink[ProbeCoordinate[L]] 

  def writerPathInference(fname: String): DataSink[PathInference[L]] 

  def writerTrack(fname: String): DataSink[TrackPiece[L]] 

  /***************** Java wrappers **************/

  def readTrackJava(fname: String): JCollection[TrackPiece[L]] 

  def readProbeCoordinateJava(fname: String): JCollection[ProbeCoordinate[L]]   
}

/**
 * New implementation for serializing data.
 * Offers improved speed and more features (like lazy loading of the collection)
 *
 * Depending on the network you are using, you should use one of the concrete classes.
 */
trait BaseSerializer[L<:Link] extends Serializer[L] {

  def encodeExtensiveInfo = true

  val factory = JsonNodeFactory.instance
  val jfactory = (new JsonFactory)
  val mapper = new ObjectMapper

  def m: Manifest[L]

  def link(node: JsonNode): L

  def encodeLink(l: L): JsonNode

  def coordinate(node: JsonNode): Coordinate = {
    new Coordinate(4326, node.get("lat").asDouble, node.get("lng").asDouble)
  }

  def encode(c: Coordinate): JsonNode = {
    val node = new ObjectNode(factory)
    node.put("lat", c.lat)
    node.put("lng", c.lon)
    if (c.srid != null) {
      node.put("srid", c.srid.intValue)
    }
    node
  }

  def spot(node: JsonNode): Spot[L] = {
    val l = link(node.get("link"))
    val offset = node.get("offset").asDouble
    new Spot(l, offset.toFloat, 0) // FIXME: lane
  }

  def encode(sp: Spot[L]): JsonNode = {
    val node = new ObjectNode(factory)
    node.put("link", encodeLink(sp.link))
    node.put("offset", sp.offset)
    if (encodeExtensiveInfo) {
      node.put("gps_pos", encode(sp.toCoordinate))
    }
    node
  }

  def route(node: JsonNode): Route[L] = {
    val start = spot(node.get("start"))
    val end = spot(node.get("end"))
    val dcts: Iterable[JsonNode] = node.get("links")
    val links = dcts.map(link _).toArray(m)
    new Route(links, start.offset, end.offset)
  }

  def encode(r: Route[L]): JsonNode = {
    val node = new ObjectNode(factory)
    node.put("start", encode(r.getFirstSpot))
    node.put("end", encode(r.getLastSpot))
    val an = new ArrayNode(factory)
    an.addAll(r.getLinks.toSeq.map(encodeLink _))
    node.put("links", an)
    if (encodeExtensiveInfo) {
      val latlngs = new ArrayNode(factory)
      latlngs.addAll(r.spots.toSeq.map(_.toCoordinate).map(encode _))
      node.put("latlngs", latlngs)
    }
    node
  }

  def time(node: JsonNode):Time = {
    // Some older files had a different notation.
    val millis = if (node.has("millis")) {
      node.get("milli").asInt
    } else { 0 }
    Time.newTimeFromBerkeleyDateTime(node.get("year").asInt,
                                     node.get("month").asInt,
                                     node.get("day").asInt,
                                     node.get("hour").asInt,
                                     node.get("minute").asInt,
                                     node.get("second").asInt,
                                     millis)
  }

  def encode(t: Time): JsonNode = {
    val node = new ObjectNode(factory)
    node.put("year", t.getYear)
    node.put("month", t.getMonth)
    node.put("day", t.getDayOfMonth)
    node.put("hour", t.getHour)
    node.put("minute", t.getMinute)
    node.put("second", t.getSecond)
    node.put("milli", t.getMillisecond)
    node
  }

  def probeCoordinate(node: JsonNode): ProbeCoordinate[L] = {
    val id = node.get("id").asText
    val c = coordinate(node.get("latlng"))
    val t = time(node.get("time"))
    val spots = node.get("states").toSeq.map(spot _).toArray
    val hired:java.lang.Boolean = {
      if (node.has("hired")) {
        node.get("hired").asBoolean
      } else {
        null
      }
    }
    val speed:java.lang.Float = {
      if (node.has("speed")) {
        node.get("speed").asDouble().toFloat
      } else {
        null
      }
    }
    val heading:java.lang.Short = {
      if (node.has("heading")) {
        node.get("heading").asInt().toShort
      } else {
        null
      }
    }
    val hdop:java.lang.Float = {
      if (node.has("hdop")) {
        node.get("hdop").asDouble().toFloat
      } else {
        null
      }
    }
    new ProbeCoordinate(c, t, id, spots, speed, heading, hired, hdop)
  }

  def encode(pc: ProbeCoordinate[L]): JsonNode = {
    val node = new ObjectNode(factory)
    node.put("id", pc.id)
    node.put("latlng", encode(pc.coordinate))
    node.put("time", encode(pc.time))
    val spots = new ArrayNode(factory)
    spots.addAll(pc.spots.toSeq.map(encode _))
    node.put("states", spots)
    if (pc.speed != null) {
      node.put("speed", pc.speed.doubleValue())
    }
    if (pc.heading != null) {
      node.put("heading", pc.heading.intValue())
    }
    if (pc.hdop != null) {
      node.put("hdop", pc.hdop.doubleValue())
    }
    if (pc.hired != null) {
      node.put("hired", pc.hired.booleanValue)
    }
    node
  }

  def pathInference(node: JsonNode): PathInference[L] = {
    val id = node.get("id").asText
    val start = time(node.get("start_time"))
    val end = time(node.get("end_time"))
    val paths = node.get("routes").toSeq.map(route _).toArray
    val hired:java.lang.Boolean = {
      if (node.has("hired")) {
        node.get("hired").asBoolean
      } else {
        null
      }
    }
    // FIXME: incomplete decoding
    new PathInference(id, start, end, paths, null /*probas*/, hired)
  }

  def encode(pi: PathInference[L]): JsonNode = {
    val node = new ObjectNode(factory)
    node.put("id", pi.id)
    node.put("start_time", encode(pi.startTime))
    node.put("end_time", encode(pi.endTime))
    val routes = new ArrayNode(factory)
    routes.addAll(pi.routes.toSeq.map(encode _))
    node.put("routes", routes)
    if (pi.hired != null) {
      node.put("hired", pi.hired.booleanValue)
    }
    // FIXME: incomplete encoding
    node
  }

  def trackPiece(node: JsonNode):TrackPiece[L] =
    new JsonTrackPiece(node, this)

  def encode(tp: TrackPiece[L]): JsonNode = {
    val node = new ArrayNode(factory)
    val connex_1 = new ArrayNode(factory)
    connex_1.addAll(tp.firstConnections.toSeq.map(z => {
          val n = new ArrayNode(factory)
          n.add(z._1)
          n.add(z._2)
          n
        }))
    node.add(connex_1)
    val paths = new ArrayNode(factory)
    paths.addAll(tp.routes.toSeq.map(encode _))
    node.add(paths)
    val connex_2 = new ArrayNode(factory)
    connex_2.addAll(tp.secondConnections.toSeq.map(z => {
          val n = new ArrayNode(factory)
          n.add(z._1)
          n.add(z._2)
          n
        }))
    node.add(connex_2)
    node.add(encode(tp.point))
    node
  }

  /*********** Reading functions *************/

  def readFlow(fname: String): Iterable[JsonNode] = {
    def istream_gen() = {
      if (fname.endsWith(".gz")) {
        val fileStream = new FileInputStream(fname);
        val gzipStream = new GZIPInputStream(fileStream);
        new InputStreamReader(gzipStream, "UTF-8");
      } else {
        new FileReader(fname)
      }
    }
    JSONSource.strings(istream_gen).map(s => {
        val jparser = jfactory.createJsonParser(s)
        mapper.readTree(jparser)
      })
  }

  def readTrack(fname: String): Iterable[TrackPiece[L]] =
    readFlow(fname).map(trackPiece _)

  def readProbeCoordinates(fname: String): Iterable[ProbeCoordinate[L]] =
    readFlow(fname).map(probeCoordinate _)
  
  def readPathInferences(fname: String): Iterable[PathInference[L]] =
    readFlow(fname).map(pathInference _)
  
  def writerProbeCoordinate(fname: String): DataSink[ProbeCoordinate[L]] = {
    DataSinks.map(JsonDataSink.writeableZippedFile(fname), encode _)
  }

  def writerPathInference(fname: String): DataSink[PathInference[L]] = {
    DataSinks.map(JsonDataSink.writeableZippedFile(fname), encode _)
  }

  def writerTrack(fname: String): DataSink[TrackPiece[L]] = {
    DataSinks.map(JsonDataSink.writeableZippedFile(fname), encode _)
  }

  /***************** Java wrappers **************/

  def readTrackJava(fname: String): JCollection[TrackPiece[L]] =
    readTrack(fname)

  def readProbeCoordinateJava(fname: String): JCollection[ProbeCoordinate[L]] =
    readProbeCoordinates(fname)
}

trait TrackPiece[L<:Link] {
  def firstConnections: Array[(Int, Int)]
  def routes: Array[Route[L]]
  def secondConnections: Array[(Int, Int)]
  def point: ProbeCoordinate[L]
}

class SimpleTrackPiece[L<:Link] (first_connections:Array[Array[Int]],
 val routes: Array[Route[L]], second_connections:Array[Array[Int]], val point:ProbeCoordinate[L]) extends TrackPiece[L] {
  val firstConnections = first_connections.map(arr => (arr(0), arr(1)))
  val secondConnections = second_connections.map(arr => (arr(0), arr(1)))
}

class JsonTrackPiece[L<:Link](
  private[this] val node:JsonNode,
  private[this] val serializer: BaseSerializer[L]) extends TrackPiece[L] {

  lazy val firstConnections: Array[(Int, Int)] = {
    node.get(0).toSeq.map(n => (n.get(0).asInt, n.get(1).asInt)).toArray
  }

  lazy val routes: Array[Route[L]] = {
    node.get(1).toSeq.map(serializer.route _).toArray
  }

  lazy val secondConnections: Array[(Int, Int)] = {
    node.get(2).toSeq.map(n => (n.get(0).asInt, n.get(1).asInt)).toArray
  }

  lazy val point: ProbeCoordinate[L] = {
    serializer.probeCoordinate(node.get(3))
  }
}