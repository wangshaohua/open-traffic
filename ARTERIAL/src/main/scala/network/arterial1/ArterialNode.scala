package network.arterial1
import netconfig.storage.NodeIDRepr
import netconfig.Node
import com.google.common.collect.ImmutableList
import collection.JavaConversions._
import netconfig_extensions.CollectionUtils._

class ArterialNode(id_ : NodeIDRepr,
    incoming: Seq[ArterialLink],
    outgoing:Seq[ArterialLink]) extends Node {

  lazy val incomingLinks: ImmutableList[ArterialLink] = incoming
  
  lazy val outgoingLinks: ImmutableList[ArterialLink] = outgoing
  
  override def toString = "ArterialNode[%s,%s]" format (id.primary.toString, id.secondary.toString)
  
  def id = id_
}