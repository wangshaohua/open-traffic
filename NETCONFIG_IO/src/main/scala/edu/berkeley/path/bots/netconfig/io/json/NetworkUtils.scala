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

package edu.berkeley.path.bots.netconfig.io.json
import edu.berkeley.path.bots.core.MMLogging
import edu.berkeley.path.bots.netconfig.Link
import edu.berkeley.path.bots.netconfig.io.files.SerializedNetwork
import edu.berkeley.path.bots.network.gen.NetworkBuilder
import edu.berkeley.path.bots.netconfig.io.Serializer
import edu.berkeley.path.bots.netconfig.storage.LinkIDRepr
import edu.berkeley.path.bots.network.gen.GenericLink
import edu.berkeley.path.bots.netconfig.storage.LinkIDRepr

/**
 * A collection of utilities to materialize a network from a JSON representation on disk, using generic links.
 *
 * As long as a network is represented using a GenericLinkRepresentation, you can safely materialize this network
 * using the methods in this object.
 */
object NetworkUtils extends MMLogging {
  /**
   * Materializes links from a network ID and a network type.
   */
  def getLinks(network_id: Int, net_typesource_name: String): Map[LinkIDRepr, Link] = {
    val fname = SerializedNetwork.fileName(network_id, net_typesource_name)
    val glrs = JSonSerializer.getGenericLinks(fname)
    val builder = new NetworkBuilder
    logInfo("Building network source=%s, nid=%d" format (net_typesource_name, network_id))
    val links = builder.build(glrs)
    logInfo("Building network done")
    links.toMap
  }

  /**
   * Creates a serializer from a set of links.
   *
   * TODO(tjh) this function could be moved somewhere else.
   */
  def getSerializer(links: Map[LinkIDRepr, Link]): Serializer[Link] = {
    JSonSerializer.from(links)
  }

  /**
   * Creates a serializer for a network.
   */
  def getSerializer(net_type: String, net_id: Int): Serializer[Link] = {
    getSerializer(getLinks(net_id, net_type))
  }
}