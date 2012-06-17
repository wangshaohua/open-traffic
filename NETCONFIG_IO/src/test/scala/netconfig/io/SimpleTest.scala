package netconfig.io

import org.junit._
import org.junit.Assert._
import netconfig.storage.NodeIDRepr
import network.gen.GenericLinkRepresentation
import netconfig.storage.LinkIDRepr
import network.gen.NetworkBuilder
import netconfig.io.json.JSonSerializer
import netconfig.io.files.SerializedNetwork
import core_extensions.MMLogging

class SimpleTest extends MMLogging {

  @Test def test1:Unit = {
    val fname = SerializedNetwork.fileName(nid=0, net_type="generic")
    logInfo(fname)
    val n1 = NodeIDRepr(0, 0)
    val n2 = NodeIDRepr(1, 0)
    val l1 = GenericLinkRepresentation(LinkIDRepr(0, 0), n1, n2, None, None)
    val glrs = Seq(l1)
    val builder = new NetworkBuilder()
    val links = builder.build(glrs)
    JSonSerializer.storeLinks(fname, links)
    
    // Rebuilding the network
    val glrs_ = JSonSerializer.getGenericLinks(fname).toList
    val links_ = builder.build(glrs_)
    assertEquals(links_.length, links.length)
  }
}