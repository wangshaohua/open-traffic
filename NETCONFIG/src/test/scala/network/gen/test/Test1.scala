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
package network.gen.test
import org.junit.Test
import org.junit.Assert._

import netconfig.storage.LinkIDRepr
import netconfig.storage.NodeIDRepr
import network.gen.GenericLinkRepresentation
import network.gen.NetworkBuilder

class GenericTest {

  @Test def test1: Unit = {
	val links = SimpleGen.line(1)
    assertEquals(links.length, 1)
    assertTrue(links.head.inLinks().isEmpty())
    assertTrue(links.head.outLinks().isEmpty())
  }

  @Test def test2: Unit = {
    val n1 = NodeIDRepr(1, 0)
    val n2 = NodeIDRepr(2, 0)
    val n3 = NodeIDRepr(3, 0)
    val l1 = GenericLinkRepresentation(LinkIDRepr(0, 0), n1, n2)
    val l2 = GenericLinkRepresentation(LinkIDRepr(1, 0), n2, n3)
    val glrs = Seq(l1, l2)
    val builder = new NetworkBuilder()
    val links = builder.build(glrs).sortBy(_.toString())
    assertEquals(2, links.length)
    assertTrue(links.head.toString, links.head.inLinks().isEmpty())
    assertTrue(links.last.outLinks().isEmpty())
    assertEquals(1, links.head.outLinks().size())
    assertEquals(1, links.last.inLinks().size())
    assert(links.head.endNode() == links.last.startNode())
  }
}