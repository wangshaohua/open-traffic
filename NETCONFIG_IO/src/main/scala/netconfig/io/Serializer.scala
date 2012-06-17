package netconfig.io

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
import com.codahale.jerkson.Json._
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
  
  // TODO(?) add other accessors for the other basic types in netconfig.

  /***************** Java wrappers **************/

  def readTrackJava(fname: String): JCollection[TrackPiece[L]] 

  def readProbeCoordinateJava(fname: String): JCollection[ProbeCoordinate[L]]   
}
