/**
 * Copyright 2012. The Regents of the University of California (Regents).
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy
 * of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package network.gen

import edu.berkeley.path.bots.netconfig.Link
import edu.berkeley.path.bots.netconfig.storage.NodeIDRepr
import edu.berkeley.path.bots.netconfig.Node
import com.google.common.collect.ImmutableList
import edu.berkeley.path.bots.netconfig_extensions.CollectionUtils._
import edu.berkeley.path.bots.core.GeoMultiLine
import edu.berkeley.path.bots.netconfig.NetconfigException
import edu.berkeley.path.bots.core.storage.GeoMultiLineRepr
import collection.mutable.Map
import edu.berkeley.path.bots.netconfig.storage.LinkIDRepr
import edu.berkeley.path.bots.netconfig.storage.NodeIDRepr
import scala.collection.mutable.ArrayBuffer
import edu.berkeley.path.bots.netconfig.storage.NodeIDRepr
import edu.berkeley.path.bots.netconfig.storage.LinkIDRepr

class NetworkBuilder {

  def materializeLink(lr: GenericLinkRepresentation, start_node: GenericNode[GenericLink], end_node: GenericNode[GenericLink]): GenericLink = {
    val g: GeoMultiLine = lr.geom match {
      case None => null
      case Some(gr) => GeoMultiLineRepr.fromRepr(gr)
    }
    val l: Double = lr.length match {
      case Some(x) => x
      case None => if (g == null) {
        -1
      } else {
        g.getLength()
      }
    }
    val link = new GenericLink {
      val idRepr = lr.id
      def length: Double = {
        if (l <= 0) {
          throw new NetconfigException(null, "You did not provide link liegnth in the representation")
          0.0
        } else {
          l
        }
      }
      def numLanesAtOffset(d: Double): Short = 1
      val startNode = start_node
      val endNode = end_node
      // The lazy factor is important to delay initialization here.
      lazy val outLinks: ImmutableList[GenericLink] = end_node.outgoingLinks
      lazy val inLinks: ImmutableList[GenericLink] = start_node.incomingLinks
      def geoMultiLine: GeoMultiLine = {
        if (g == null) {
          throw new NetconfigException(null, "You are trying to use the link geometry but it was not provided in the representation")
          null
        } else {
          g
        }
      }
      def partialGeoMultiLine(x: Double, y: Double): GeoMultiLine = null
      override def toString = "GenericLink(%s)" format lr.id.toString
    }
    link
  }

  def materializeNode(nid: NodeIDRepr, incomingLinks: Seq[GenericLink], outgoingLinks: Seq[GenericLink]): GenericNode[GenericLink] = {
    val n = new GenericNode(nid, incomingLinks, outgoingLinks)
    n
  }

  def build(glrs: Iterable[GenericLinkRepresentation]): Seq[(LinkIDRepr, GenericLink)] = {
    val links = Map.empty[LinkIDRepr, GenericLink]
    val nodes = Map.empty[NodeIDRepr, GenericNode[GenericLink]]
    val inLinks = Map.empty[NodeIDRepr, ArrayBuffer[GenericLink]]
    val outLinks = Map.empty[NodeIDRepr, ArrayBuffer[GenericLink]]
    for (glr <- glrs) {
      // Check that this link was not already specified:
      if (links.contains(glr.id)) {
        throw new NetconfigException(null, "You already specified before link with ID: %s" format glr.id.toString)
      }
      // Instantiate the nodes if they do not exist
      val start_node = nodes.getOrElseUpdate(glr.startNodeId, {
        val in = inLinks.getOrElseUpdate(glr.startNodeId, new ArrayBuffer[GenericLink])
        val out = outLinks.getOrElseUpdate(glr.startNodeId, new ArrayBuffer[GenericLink])
        materializeNode(glr.startNodeId, in, out)
      })
      val end_node = nodes.getOrElseUpdate(glr.endNodeId, {
        val in = inLinks.getOrElseUpdate(glr.endNodeId, new ArrayBuffer[GenericLink])
        val out = outLinks.getOrElseUpdate(glr.endNodeId, new ArrayBuffer[GenericLink])
        materializeNode(glr.endNodeId, in, out)
      })
      // Instantiate a link
      val l = materializeLink(glr, start_node, end_node)
      // Put the link in the nodes in/out links
      inLinks(glr.endNodeId) += l
      outLinks(glr.startNodeId) += l
      links += glr.id -> l
    }
    links.toSeq
  }
}