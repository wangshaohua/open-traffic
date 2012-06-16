package netconfig.Datum.storage
import core.storage.TimeRepr
import netconfig.storage.RouteRepr

case class PathInferenceRepr(
    val id:String,
    val startTime:TimeRepr,
    val endTime:TimeRepr,
    val routes:Seq[RouteRepr],
    val probabilities:Seq[Double]) {
}