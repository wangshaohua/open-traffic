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

package edu.berkeley.path.bots.netconfig.io

import org.junit._
import org.junit.Assert._
import edu.berkeley.path.bots.netconfig.storage.NodeIDRepr
import edu.berkeley.path.bots.network.gen.GenericLinkRepresentation
import edu.berkeley.path.bots.netconfig.storage.LinkIDRepr
import edu.berkeley.path.bots.network.gen.NetworkBuilder
import edu.berkeley.path.bots.netconfig.io.json.JSonSerializer
import edu.berkeley.path.bots.netconfig.io.files.SerializedNetwork
import edu.berkeley.path.bots.core.MMLogging

class SimpleTest extends MMLogging {

  @Test def test1: Unit = {
    val fname = SerializedNetwork.fileName(nid = 0, net_type = "generic")
    logInfo(fname)
    val n1 = NodeIDRepr(0, 0)
    val n2 = NodeIDRepr(1, 0)
    val l1 = GenericLinkRepresentation(LinkIDRepr(0, 0), n1, n2, None, None)
    val glrs = Seq(l1)
    val builder = new NetworkBuilder()
    val links = builder.build(glrs).map(_._2)
    JSonSerializer.storeLinks(fname, links)

    // Rebuilding the network
    val glrs_ = JSonSerializer.getGenericLinks(fname).toList
    val links_ = builder.build(glrs_)
    assertEquals(links_.length, links.length)
  }
}