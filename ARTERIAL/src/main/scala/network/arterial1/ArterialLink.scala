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

package edu.berkeley.path.bots.network.arterial1

import edu.berkeley.path.bots.netconfig.Link
import edu.berkeley.path.bots.netconfig.storage.LinkIDRepr
import edu.berkeley.path.bots.core.GeoMultiLine

class ArterialLink(
  key: LinkIDRepr,
  val startNode: ArterialNode,
  val endNode: ArterialNode,
  val geoMultiLine: GeoMultiLine,
  val endFeature: LinkFeature,
  val speedLimit: Double,
  num_lanes: Short,
  length_ :Double) extends Link {

  def inLinks = startNode.incomingLinks

  def outLinks = endNode.outgoingLinks

  def numLanesAtOffset(off: Double) = num_lanes

  val length = length_

  override def toString = "ArterialLink[%s,%s]" format (key.primary.toString, key.secondary.toString)

  def id = key
}