package netconfig_extensions.io

import netconfig.Network
import netconfig.NavteqLink
import netconfig.NavteqNode
import org.codehaus.jackson.JsonNode
import org.codehaus.jackson.node.ObjectNode
import org.codehaus.jackson.node.ArrayNode
import collection.JavaConversions._


class NavteqSerializer(net: Network, 
    override val encodeExtensiveInfo:Boolean = true) 
  extends BaseSerializer[NavteqLink] {
  
  
  val m:Manifest[NavteqLink] = scala.reflect.Manifest.singleType(net.getNavteqLinks.head)

  def link(node: JsonNode): NavteqLink = {
    val id = node.get("id").asLong
    val direction = node.get("direction").asBoolean
    val l = net.getNavteqLink(id, direction)
    assert(l!=null)
    l
  }

  def encodeLink(l: NavteqLink): JsonNode = {
    val n = new ObjectNode(factory)
    n.put("id", l.id)
    n.put("direction", l.direction)
    n
  }

  def encodeLinkDescription(l: NavteqLink): JsonNode = {
    val n = new ObjectNode(factory)
    n.put("link_id", encodeLink(l))
    n.put("link_length", l.length)

    // The start node
    {
      val key = l.getStartNode().asInstanceOf[NavteqNode].node_id
      n.put("start_node", key)
    }
    // The end node
    {
      val key = l.getEndNode().asInstanceOf[NavteqNode].node_id
      n.put("end_node", key)
    }
    // The geometry
    {
      val latlngs = new ArrayNode(factory)
      latlngs.addAll(l.getGeoMultiLine().getCoordinates().map(encode _).toSeq)
      n.put("latlngs", latlngs)
    }
    
    // The end feature
    n.put("end_stop",l.stopsign)
    n.put("signal",l.signal)
    n.put("speed_limit",l.speed_limit)
    n.put("ramp",l.ramp)
    n.put("lanes",l.lanes)
    n
  }
}
