package netconfig.storage

case class RouteRepr(var links:Seq[LinkIDRepr], var spots:Seq[SpotRepr]) {
}