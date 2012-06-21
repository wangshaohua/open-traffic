package netconfig.Datum.storage
import core.storage.CoordinateRepresentation
import core.storage.TimeRepr
import netconfig.storage.SpotRepr

case class ProbeCoordinateRepr(
  var id: String,
  var time: TimeRepr,
  var coordinate: CoordinateRepresentation,
  var spots: Seq[SpotRepr]=Seq.empty,
  var probabilities: Seq[Double]=Seq.empty,
  var speed: Option[Double]=None,
  var heading: Option[Double]=None,
  var hired: Option[Boolean]=None,
  var hdop: Option[Float]=None) {
}