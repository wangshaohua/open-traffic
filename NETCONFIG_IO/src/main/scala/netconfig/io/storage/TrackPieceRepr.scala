package netconfig.io.storage
import netconfig.storage.RouteRepr
import netconfig.Datum.storage.ProbeCoordinateRepr

case class TrackPieceRepr(
    var firstConnections:Seq[(Int, Int)],
    var routes:Seq[RouteRepr],
    var secondConnections: Seq[(Int, Int)],
    var point:ProbeCoordinateRepr) {
}