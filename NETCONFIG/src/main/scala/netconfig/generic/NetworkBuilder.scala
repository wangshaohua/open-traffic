package netconfig.generic
import netconfig.Link
import netconfig.storage.NodeIDRepr
import netconfig.Node
import com.google.common.collect.ImmutableCollection
import netconfig_extensions.CollectionUtils._
import core.GeoMultiLine
import netconfig.NetconfigException
import core.storage.GeoMultiLineRepr
import collection.mutable.Map
import netconfig.storage.LinkIDRepr
import netconfig.storage.NodeIDRepr
import scala.collection.mutable.ArrayBuffer
import netconfig.storage.NodeIDRepr

class NetworkBuilder {

  def materializeLink(lr:GenericLinkRepresentation, start_node:GenericNode[GenericLink], end_node:GenericNode[GenericLink]):GenericLink = {
    val g:GeoMultiLine = lr.geom match {
      case None => null
      case Some(gr) => GeoMultiLineRepr.fromRepr(gr)
    }
    val l:Double = lr.length match {
      case Some(x) => x
      case None => if (g == null) {
        -1
      } else {
        g.getLength()
      }
    }
    val link = new GenericLink {
      def length:Double = {
        if (l <= 0) {
          throw new NetconfigException(null, "You did not provide link liegnth in the representation")
          0.0
        } else {
          l
        }
      }
      def numLanesAtOffset(d:Double):Short = 1
      val startNode = start_node
      val endNode = end_node
      // The lazy factor is important to delay initialization here.
      lazy val outLinks:ImmutableCollection[GenericLink] = end_node.outgoingLinks
      lazy val inLinks:ImmutableCollection[GenericLink] = start_node.incomingLinks
      def geoMultiLine:GeoMultiLine = {
        if (g == null) {
          throw new NetconfigException(null, "You are trying to use the link geometry but it was not provided in the representation")
          null
        } else {
          g
        }
      }
    }    
    link
  }
  
  def materializeNode(nid:NodeIDRepr, incomingLinks:Seq[GenericLink], outgoingLinks:Seq[GenericLink]):GenericNode[GenericLink] = {
    val n = new GenericNode(incomingLinks, outgoingLinks)
    n
  }
  
  def build(glrs:Iterable[GenericLinkRepresentation]):Seq[GenericLink] = {
    val links = Map.empty[LinkIDRepr, GenericLink]
    val nodes = Map.empty[NodeIDRepr, GenericNode[GenericLink]]
    val inLinks = Map.empty[NodeIDRepr, ArrayBuffer[GenericLink]]
    val outLinks = Map.empty[NodeIDRepr, ArrayBuffer[GenericLink]]
    for (glr <- glrs) {
      // Instantiate the nodes if they do not exist
      if (!inLinks.contains(glr.endNodeId)) {
        val in_ = new ArrayBuffer[GenericLink]
        
      }
      // Instantiate a link
      // Put the link in the nodes in/out links
    }
    links.values.toSeq
  }
}