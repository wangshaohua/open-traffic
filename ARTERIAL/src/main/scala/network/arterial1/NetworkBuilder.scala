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

package network.arterial1

import network.gen.GenericLinkRepresentation
import core.GeoMultiLine
import core.storage.GeoMultiLineRepr
import netconfig.NetconfigException
import netconfig.storage.NodeIDRepr
import netconfig.storage.LinkIDRepr
import scala.collection.mutable.ArrayBuffer
import collection.mutable.{ Map => MMap }

/**
 * Builder for an arterial network.
 *
 * Uses the serialized representation encoded with GenericLinkRepresentation
 * to materialize an arterial network.
 */
object NetworkBuilder {
  /**
   * Creates a link for a start node, an end node and a representation.
   *
   * If the geometry is provided but not the length, will use the length of the geometry instead.
   */
  private def materializeLink(lr: GenericLinkRepresentation, start_node: ArterialNode, end_node: ArterialNode): ArterialLink = {
    val g: GeoMultiLine = lr.geom.map(GeoMultiLineRepr.fromRepr _).getOrElse(null)

    val l: Double = lr.length.getOrElse(if (g == null) { -1.0 } else { g.getLength() })

    val key = lr.id
    if (lr.endFeature == None) {
      throw new NetconfigException(null, "Trying to materialize an arterial link that lacks endFeature: " + lr)
    }
    val end_feature = LinkFeature.decode(lr.endFeature.get) match {
      case Some(x) => x
      case None => {
        throw new NetconfigException(null, "Could not parse endFeature: " + lr.endFeature.get + " in " + lr)
      }
    }

    if (lr.numLanes == None) {
      throw new NetconfigException(null, "Trying to materialize an arterial link that lacks numLanes: " + lr)
    }
    val num_lanes = lr.numLanes.get.toShort

    if (lr.speedLimit == None) {
      throw new NetconfigException(null, "Trying to materialize an arterial link that lacks speedLimit: " + lr)
    }
    val speed_limit = lr.speedLimit.get

    val link = new ArterialLink(key, start_node, end_node, g, end_feature, speed_limit, num_lanes, l)
    link
  }

  private def materializeNode(nid: NodeIDRepr, incomingLinks: Seq[ArterialLink], outgoingLinks: Seq[ArterialLink]): ArterialNode = {
    new ArterialNode(nid, incomingLinks, outgoingLinks)
  }

  def build(glrs: Iterable[GenericLinkRepresentation]): Seq[ArterialLink] = {
    val links = MMap.empty[LinkIDRepr, ArterialLink]
    val nodes = MMap.empty[NodeIDRepr, ArterialNode]
    val inLinks = MMap.empty[NodeIDRepr, ArrayBuffer[ArterialLink]]
    val outLinks = MMap.empty[NodeIDRepr, ArrayBuffer[ArterialLink]]
    for (glr <- glrs) {
      // Check that this link was not already specified:
      if (links.contains(glr.id)) {
        throw new NetconfigException(null, "You already specified before link with ID: %s" format glr.id.toString)
      }
      // Instantiate the nodes if they do not exist
      val start_node = nodes.getOrElseUpdate(glr.startNodeId, {
        val in = inLinks.getOrElseUpdate(glr.startNodeId, new ArrayBuffer[ArterialLink])
        val out = outLinks.getOrElseUpdate(glr.startNodeId, new ArrayBuffer[ArterialLink])
        materializeNode(glr.startNodeId, in, out)
      })
      val end_node = nodes.getOrElseUpdate(glr.endNodeId, {
        val in = inLinks.getOrElseUpdate(glr.endNodeId, new ArrayBuffer[ArterialLink])
        val out = outLinks.getOrElseUpdate(glr.endNodeId, new ArrayBuffer[ArterialLink])
        materializeNode(glr.endNodeId, in, out)
      })
      // Instantiate a link
      val l = materializeLink(glr, start_node, end_node)
      // Put the link in the nodes in/out links
      inLinks(glr.endNodeId) += l
      outLinks(glr.startNodeId) += l
      links += glr.id -> l
    }
    links.values.toSeq
  }
}

