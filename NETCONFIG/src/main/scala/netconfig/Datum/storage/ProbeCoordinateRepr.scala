package netconfig.Datum.storage
import core.storage.CoordinateRepresentation
import core.storage.TimeRepr
import netconfig.storage.SpotRepr

case class ProbeCoordinateRepr(
  var id: String,
  var time: TimeRepr,
  var coordinate: CoordinateRepresentation,
  var spots: Seq[SpotRepr],
  var probabilities: Seq[Double],
  var speed: Option[Double],
  var heading: Option[Double],
  var hired: Option[Boolean],
  var hdop: Option[Float]) {
}