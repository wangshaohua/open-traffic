package network.gen
import netconfig.storage.LinkIDRepr
import netconfig.storage.NodeIDRepr
import netconfig.storage.NodeIDRepr
import core.storage.GeoMultiLineRepr

/**
 * A representation of a link that should be compatible with just about any
 * link on a road network.
 * @param id: the identifier of the link
 * @param startNodeId: the identifier of the start node of the link
 * @param endNodeId: the identifier of the end node of the link
 * @param geom (optional) a representation of the geometry of the link
 * @param length (optional) the length of the link. If a geometry is provided and
 *  a length is not provided, the length of the geometry will be used instead.
 */
case class GenericLinkRepresentation(
    val id:LinkIDRepr, 
    val startNodeId:NodeIDRepr, 
    val endNodeId:NodeIDRepr,
    val geom:Option[GeoMultiLineRepr]=None,
    val length:Option[Double]=None) {
    // Add a few more options here...
}

object GenericLinkRepr {
  def toRepr(l:GenericLink):GenericLinkRepresentation = {
    GenericLinkRepresentation(
        l.idRepr,
        l.startNode.asInstanceOf[GenericNode[GenericLink]].idRepr,
        l.startNode.asInstanceOf[GenericNode[GenericLink]].idRepr)
  }
}