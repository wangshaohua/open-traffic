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
package netconfig.io

import netconfig.Link
import netconfig.Spot
import netconfig.Route
import netconfig.Datum.ProbeCoordinate
import netconfig.Datum.TrackPiece
import netconfig.Datum.PathInference
import core.Coordinate
import core.Time
import java.io.FileReader
import java.io.FileInputStream
import java.io.InputStreamReader
import java.util.{ Collection => JCollection }
import java.util.zip.GZIPInputStream
import com.codahale.jerkson.Json._
import scala.collection.JavaConversions._

/**
 * This is the main interface for the serializer which users should be using.
 * FIXME add some documentation.
 */
trait Serializer[L <: Link] {
  def readTrack(fname: String): Iterable[TrackPiece[L]]

  def readProbeCoordinates(fname: String): Iterable[ProbeCoordinate[L]]

  def readPathInferences(fname: String): Iterable[PathInference[L]]

  def writerProbeCoordinate(fname: String, extended_representation: Boolean): DataSink[ProbeCoordinate[L]]

  def writerPathInference(fname: String, extended_representation: Boolean): DataSink[PathInference[L]]

  def writerTrack(fname: String, extended_representation: Boolean): DataSink[TrackPiece[L]]

  // TODO(?) add other accessors for the other basic types in netconfig.

  /***************** Java wrappers **************/

  def readTrackJava(fname: String): JCollection[TrackPiece[L]]

  def readProbeCoordinateJava(fname: String): JCollection[ProbeCoordinate[L]]
}
