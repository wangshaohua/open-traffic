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
package edu.berkeley.path.bots.network.simple

import collection.immutable.{ Map => IMap }
import collection.mutable.{ Map => MMap, ArrayBuffer }
import edu.berkeley.path.bots.core.MMLogging
import edu.berkeley.path.bots.core.Coordinate

trait SimpleNetwork {

  def links: IMap[LinkKey, SimpleLink]

  def nodes: IMap[NodeKey, SimpleNode]

  def inLinks: IMap[NodeKey, Array[LinkKey]]

  def outLinks: IMap[NodeKey, Array[LinkKey]]

  def getLinks: Array[SimpleLink] = links.values.toArray

  def getNodes: Array[SimpleNode] = nodes.values.toArray
}

class SimpleNetworkBuilder extends MMLogging {

  var link_ids = 0

  var node_ids = 0

  val links_ = MMap.empty[LinkKey, SimpleLink]

  val nodes_ = MMap.empty[NodeKey, SimpleNode]

  val inLinks_ = MMap.empty[NodeKey, ArrayBuffer[LinkKey]]

  val outLinks_ = MMap.empty[NodeKey, ArrayBuffer[LinkKey]]

  // The lazy attributes will ensure the network gets populated correctly
  // when called only (not at creation)
  lazy val finished_network = new SimpleNetwork {
    lazy val links = IMap.empty[LinkKey, SimpleLink] ++ links_
    lazy val nodes = IMap.empty[NodeKey, SimpleNode] ++ nodes_
    lazy val inLinks = IMap.empty[NodeKey, Array[LinkKey]] ++ inLinks_.map(z => {
      val (key, ab) = z
      (key, ab.toArray)
    })
    lazy val outLinks = IMap.empty[NodeKey, Array[LinkKey]] ++ outLinks_.map(z => {
      val (key, ab) = z
      (key, ab.toArray)
    })
  }

  def addNode(c: Coordinate): SimpleNode = {
    node_ids += 1
    logInfo("New node with node ID=" + node_ids)
    val sn = new SimpleNode(c, node_ids)
    nodes_ += sn.id -> sn
    inLinks_ += sn.id -> new ArrayBuffer[LinkKey]()
    outLinks_ += sn.id -> new ArrayBuffer[LinkKey]()
    sn
  }

  def addLink(start: SimpleNode, end: SimpleNode): SimpleLink = {
    link_ids += 1
    logInfo("New link with link ID=" + link_ids)
    val sl = new SimpleLink(start, end, link_ids, finished_network)
    links_ += sl.id -> sl
    inLinks_(end.id) += link_ids
    outLinks_(start.id) += link_ids
    sl
  }

  def getNetwork: SimpleNetwork = finished_network
}