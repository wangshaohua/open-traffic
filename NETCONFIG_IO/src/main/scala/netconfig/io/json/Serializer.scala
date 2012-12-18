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

package netconfig.io.json

import java.io.FileInputStream
import java.io.FileReader
import java.io.InputStreamReader
import java.util.zip.GZIPInputStream
import java.util.{ Collection => JCollection }

import scala.collection.JavaConversions.asJavaCollection
import scala.collection.JavaConversions.asScalaBuffer

import com.codahale.jerkson.Json.generate
import com.codahale.jerkson.Json.parse
import com.google.common.collect.ImmutableList

import edu.berkeley.path.bots.netconfig.Datum.storage.Codec
import edu.berkeley.path.bots.netconfig.Datum.storage.PathInferenceRepr
import edu.berkeley.path.bots.netconfig.Datum.storage.ProbeCoordinateRepr
import edu.berkeley.path.bots.netconfig.Datum.PathInference
import edu.berkeley.path.bots.netconfig.Datum.ProbeCoordinate
import edu.berkeley.path.bots.netconfig.Datum.storage.ConnectionRepr
import edu.berkeley.path.bots.netconfig.Datum.storage.TrackPieceRepr
import edu.berkeley.path.bots.netconfig.Datum.TrackPiece
import edu.berkeley.path.bots.netconfig.Datum.TrackPiece.TrackPieceConnection
import edu.berkeley.path.bots.netconfig.io.DataSink
import edu.berkeley.path.bots.netconfig.io.DataSinks
import edu.berkeley.path.bots.netconfig.io.Serializer
import edu.berkeley.path.bots.netconfig.io.StringDataSink
import edu.berkeley.path.bots.netconfig.io.StringSource
import edu.berkeley.path.bots.netconfig.storage.LinkIDRepr
import edu.berkeley.path.bots.netconfig_extensions.CollectionUtils.asImmutableList1
import edu.berkeley.path.bots.netconfig.Link
import edu.berkeley.path.bots.netconfig.Route
import network.gen.GenericLink
import network.gen.GenericLinkRepr
import network.gen.GenericLinkRepresentation

/**
 * Represents a file as a sequence of lines.
 *
 * TODO(?) replace with scala-io at some point.
 */
object FileReading {
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
}

/**
 * New implementation for serializing data.
 * Offers improved speed and more features (like lazy loading of the collection)
 *
 * Depending on the network you are using, you should use one of the concrete classes.
 */
trait JsonSerializer[L <: Link] extends Serializer[L] with Codec[L] {

  def encodeExtensiveInfo = true

  // TODO(?) These functions should be moved to netconfig.Datum.Codec

  def toRepr(tp: TrackPiece[L], extended_representation: Boolean): TrackPieceRepr = {
    val first = tp.firstConnections.map(ConnectionRepr.toRepr _)
    val second = tp.secondConnections.map(ConnectionRepr.toRepr _)
    new TrackPieceRepr(
      first,
      tp.routes.map(r => (toRepr(r, extended_representation))),
      second,
      toRepr(tp.point, extended_representation))
  }

  def fromRepr(tpr: TrackPieceRepr): TrackPiece[L] = {
    val first: ImmutableList[TrackPieceConnection] = tpr.firstConnections.map(ConnectionRepr.fromRepr _)
    val second: ImmutableList[TrackPieceConnection] = tpr.secondConnections.map(ConnectionRepr.fromRepr _)
    val p = probeCoordinateFromRepr(tpr.point)
    val routes_ : ImmutableList[Route[L]] = tpr.routes.map(fromRepr _)
    TrackPiece.from(
      first,
      routes_,
      second,
      p)
  }

  /*********** Reading functions *************/

  def readTrack(fname: String): Iterable[TrackPiece[L]] =
    FileReading.readFlow(fname).map(s => fromRepr(parse[TrackPieceRepr](s)))

  def readProbeCoordinates(fname: String): Iterable[ProbeCoordinate[L]] =
    FileReading.readFlow(fname).map(s => probeCoordinateFromRepr(parse[ProbeCoordinateRepr](s)))

  def readPathInferences(fname: String): Iterable[PathInference[L]] =
    FileReading.readFlow(fname).map(s => pathInferenceFromRepr(parse[PathInferenceRepr](s)))

  // TODO(?) add other accessors for the other basic types in netconfig.

  //******** WRITER FUNCTIONS *********/

  def writerProbeCoordinate(fname: String, extended_representation: Boolean): DataSink[ProbeCoordinate[L]] = {
    def f(pc: ProbeCoordinate[L]): String = generate(toRepr(pc, extended_representation))
    DataSinks.map(StringDataSink.writeableZippedFile(fname), f _)
  }

  def writerPathInference(fname: String, extended_representation: Boolean): DataSink[PathInference[L]] = {
    def f(pi: PathInference[L]): String = generate(toRepr(pi, extended_representation))
    DataSinks.map(StringDataSink.writeableZippedFile(fname), f _)
  }

  def writerTrack(fname: String, extended_representation: Boolean): DataSink[TrackPiece[L]] = {
    def f(tp: TrackPiece[L]): String = generate(toRepr(tp, extended_representation))
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

  def storeLinks(fname: String, links: Seq[GenericLink]): Unit = {

    def f(l: GenericLink): String = generate(GenericLinkRepr.toRepr(l))
    val sink = DataSinks.map(StringDataSink.writeableZippedFile(fname), f _)
    for (l <- links) {
      sink.put(l)
    }
    sink.close()
  }

  def getGenericLinks(fname: String): Iterable[GenericLinkRepresentation] = {
    StringSource.readLines(fname).map(s => parse[GenericLinkRepresentation](s))
  }
}
