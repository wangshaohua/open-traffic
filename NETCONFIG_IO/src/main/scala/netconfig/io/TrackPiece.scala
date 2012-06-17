package netconfig.io

import netconfig.Link
import netconfig.Spot
import netconfig.Route
import netconfig.Datum.ProbeCoordinate
import netconfig.Datum.PathInference
import core.Coordinate
import com.google.common.collect.ImmutableList

class Connection(val from:Int, val to:Int)

trait TrackPiece[L<:Link] {
  def firstConnections: ImmutableList[Connection]
  def routes: ImmutableList[Route[L]]
  def secondConnections: ImmutableList[Connection]
  def point: ProbeCoordinate[L]
}
