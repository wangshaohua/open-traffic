package netconfig.generic
import netconfig.storage.LinkIDRepr
import netconfig.storage.NodeIDRepr
import netconfig.storage.NodeIDRepr
import core.storage.GeoMultiLineRepr

case class GenericLinkRepresentation(
    val id:LinkIDRepr, 
    val startNodeId:NodeIDRepr, 
    val endNodeId:NodeIDRepr,
    val geom:Option[GeoMultiLineRepr],
    val length:Option[Double]) {
    // Add a few more options here...
}