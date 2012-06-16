package network.gen.test
import org.junit.Test
import org.junit.Assert._

import netconfig.storage.LinkIDRepr
import netconfig.storage.NodeIDRepr
import network.gen.GenericLinkRepresentation
import network.gen.NetworkBuilder

class GenericTest {

  @Test def test1: Unit = {
    val n1 = NodeIDRepr(0, 0)
    val n2 = NodeIDRepr(1, 0)
    val l1 = GenericLinkRepresentation(LinkIDRepr(0, 0), n1, n2, None, None)
    val glrs = Seq(l1)
    val builder = new NetworkBuilder()
    val links = builder.build(glrs)
    assertEquals(links.length, 1)
    assertTrue(links.first.inLinks().isEmpty())
    assertTrue(links.first.outLinks().isEmpty())
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
    assertTrue(links.first.toString, links.first.inLinks().isEmpty())
    assertTrue(links.last.outLinks().isEmpty())
    assertEquals(1, links.first.outLinks().size())
    assertEquals(1, links.last.inLinks().size())
    assert(links.first.endNode() == links.last.startNode())
  }
}