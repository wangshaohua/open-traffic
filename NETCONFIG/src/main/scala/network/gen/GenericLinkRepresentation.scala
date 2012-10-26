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
  var id: LinkIDRepr,
  var startNodeId: NodeIDRepr,
  var endNodeId: NodeIDRepr,
  var geom: Option[GeoMultiLineRepr] = None,
  var length: Option[Double] = None,
  var endFeature: Option[String] = None,
  var speedLimit: Option[Double] = None,
  var numLanes: Option[Int] = None) {
  // Add a few more options here...
}

object GenericLinkRepr {
  def toRepr(l: GenericLink): GenericLinkRepresentation = {
    GenericLinkRepresentation(
      l.idRepr,
      l.startNode.asInstanceOf[GenericNode[GenericLink]].idRepr,
      l.startNode.asInstanceOf[GenericNode[GenericLink]].idRepr)
  }
}