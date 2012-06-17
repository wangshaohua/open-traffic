/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package netconfig.io.json

import java.io.FileInputStream
import java.io.FileReader
import java.io.InputStreamReader
import java.util.zip.GZIPInputStream
import java.util.{Collection => JCollection}
import scala.collection.JavaConversions._
import com.codahale.jerkson.Json._
import com.google.common.collect.ImmutableList
import netconfig.Datum.storage.Codec
import netconfig.Datum.storage.PathInferenceRepr
import netconfig.Datum.storage.ProbeCoordinateRepr
import netconfig.Datum.PathInference
import netconfig.Datum.ProbeCoordinate
import netconfig.io.storage.TrackPieceRepr
import netconfig.io.Connection
import netconfig.io.DataSink
import netconfig.io.DataSinks
import netconfig.io.Serializer
import netconfig.io.StringDataSink
import netconfig.io.StringSource
import netconfig.io.TrackPiece
import netconfig.storage.LinkIDRepr
import netconfig_extensions.CollectionUtils.asImmutableList1
import netconfig.Link
import netconfig.Route
import network.gen.GenericLink
import network.gen.GenericLinkRepr
import network.gen.GenericLinkRepresentation
import network.gen.GenericLinkRepresentation

/**
 * New implementation for serializing data.
 * Offers improved speed and more features (like lazy loading of the collection)
 *
 * Depending on the network you are using, you should use one of the concrete classes.
 */
trait JsonSerializer[L <: Link] extends Serializer[L] with Codec[L] {

  def encodeExtensiveInfo = true

  // TODO(?) These functions should be moved to netconfig.Datum.Codec

  def toRepr(tp: TrackPiece[L]): TrackPieceRepr = {
    val first = tp.firstConnections.map(c => (c.from, c.to))
    val second = tp.firstConnections.map(c => (c.from, c.to))
    new TrackPieceRepr(
      first,
      tp.routes.map(toRepr _),
      second,
      toRepr(tp.point))
  }

  def fromRepr(tpr: TrackPieceRepr): TrackPiece[L] = {
    val first: ImmutableList[Connection] = tpr.firstConnections.map(z => new Connection(z._1, z._2))
    val second: ImmutableList[Connection] = tpr.firstConnections.map(z => new Connection(z._1, z._2))
    val p = probeCoordinateFromRepr(tpr.point)
    val routes_ : ImmutableList[Route[L]] = tpr.routes.map(fromRepr _)
    new TrackPiece[L] {
      def firstConnections = first
      def routes = routes_
      def secondConnections = second
      def point = p
    }
  }

  /*********** Reading functions *************/

  def readFlow(fname: String): Iterable[String] = {
    def istream_gen() = {
      if (fname.endsWith(".gz")) {
        val fileStream = new FileInputStream(fname);
        val gzipStream = new GZIPInputStream(fileStream);
        new InputStreamReader(gzipStream, "UTF-8");
      } else {
        new FileReader(fname)
      }
    }
    StringSource.strings(istream_gen)
  }

  def readTrack(fname: String): Iterable[TrackPiece[L]] =
    readFlow(fname).map(s => fromRepr(parse[TrackPieceRepr](s)))

  def readProbeCoordinates(fname: String): Iterable[ProbeCoordinate[L]] =
    readFlow(fname).map(s => probeCoordinateFromRepr(parse[ProbeCoordinateRepr](s)))

  def readPathInferences(fname: String): Iterable[PathInference[L]] =
    readFlow(fname).map(s => pathInferenceFromRepr(parse[PathInferenceRepr](s)))

  // TODO(?) add other accessors for the other basic types in netconfig.

  //******** WRITER FUNCTIONS *********/

  def writerProbeCoordinate(fname: String): DataSink[ProbeCoordinate[L]] = {
    def f(pc: ProbeCoordinate[L]): String = generate(toRepr(pc))
    DataSinks.map(StringDataSink.writeableZippedFile(fname), f _)
  }

  def writerPathInference(fname: String): DataSink[PathInference[L]] = {
    def f(pi: PathInference[L]): String = generate(toRepr(pi))
    DataSinks.map(StringDataSink.writeableZippedFile(fname), f _)
  }

  def writerTrack(fname: String): DataSink[TrackPiece[L]] = {
    def f(tp: TrackPiece[L]): String = generate(toRepr(tp))
    DataSinks.map(StringDataSink.writeableZippedFile(fname), f _)
  }

  /***************** Java wrappers **************/

  def readTrackJava(fname: String): JCollection[TrackPiece[L]] =
    readTrack(fname)

  def readProbeCoordinateJava(fname: String): JCollection[ProbeCoordinate[L]] =
    readProbeCoordinates(fname)
}

object JSonSerializer {
  def from[L <: Link](fromFun: LinkIDRepr => L, toFun: L => LinkIDRepr): JsonSerializer[L] = {
    new JsonSerializer[L] {
      def fromLinkID(lid: LinkIDRepr): L = fromFun(lid)
      def toLinkID(l: L): LinkIDRepr = toFun(l)
    }
  }
  def from[L <: Link](links: Map[LinkIDRepr, L]): JsonSerializer[L] = {
    val map2: Map[L, LinkIDRepr] = links.map(z => (z._2, z._1))
    from(links.apply _, map2.apply _)
  }
  
  def storeLinks(fname:String, links:Seq[GenericLink]):Unit = {
    
    def f(l: GenericLink): String = generate(GenericLinkRepr.toRepr(l))
    val sink = DataSinks.map(StringDataSink.writeableZippedFile(fname), f _)
    for (l <- links) {
      sink.put(l)
    }
    sink.close()
  }
  
  def getGenericLinks(fname:String):Iterable[GenericLinkRepresentation] = {
    StringSource.readLines(fname).map(s=>parse[GenericLinkRepresentation](s))
  }
}
