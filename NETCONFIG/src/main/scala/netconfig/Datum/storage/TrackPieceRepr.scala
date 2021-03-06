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

package netconfig.Datum.storage

import netconfig.storage.RouteRepr
import netconfig.Datum.TrackPiece.TrackPieceConnection

/**
 * Connection representation, since jerkson does not support tuples right now.
 */
case class ConnectionRepr(
  var from: Int,
  var to: Int)

/**
 */
case class TrackPieceRepr(
  var firstConnections: Seq[ConnectionRepr],
  var routes: Seq[RouteRepr],
  var secondConnections: Seq[ConnectionRepr],
  var point: ProbeCoordinateRepr) {
}

object ConnectionRepr {

  def fromRepr(r: ConnectionRepr): TrackPieceConnection = new TrackPieceConnection(r.from, r.to)

  def toRepr(c: TrackPieceConnection): ConnectionRepr = ConnectionRepr(c.from, c.to)
}