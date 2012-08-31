package network.arterial1

import netconfig.Link
import netconfig.storage.LinkIDRepr
import core.GeoMultiLine

class ArterialLink(
  key: LinkIDRepr,
  val startNode: ArterialNode,
  val endNode: ArterialNode,
  val geoMultiLine: GeoMultiLine,
  val endFeature: LinkFeature,
  val speedLimit: Double,
  num_lanes: Short) extends Link {

  def inLinks = startNode.incomingLinks

  def outLinks = endNode.outgoingLinks

  def numLanesAtOffset(off: Double) = num_lanes

  lazy val length = geoMultiLine.getLength()

  override def toString = "ArterialLink[%s,%s]" format (key.primary.toString, key.secondary.toString)

  def id = key
}