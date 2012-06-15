/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package netconfig_extensions.io

import netconfig.Network
import netconfig.NavteqLink
import org.codehaus.jackson.JsonNode
import org.codehaus.jackson.node.ObjectNode
import netconfig.ModelGraphLink
import netconfig.ModelGraphNode
import org.codehaus.jackson.node.ArrayNode
import collection.JavaConversions._

class ModelGraphSerializer(net: Network,
   override val encodeExtensiveInfo:Boolean = true) 
  extends BaseSerializer[ModelGraphLink] {

  
  val m:Manifest[ModelGraphLink] = scala.reflect.Manifest.singleType(net.getLinks.head)

  def link(node: JsonNode): ModelGraphLink = {
    val id = node.get("id").asInt
    net.getLinkWithID(id)
  }

  def encodeLink(l: ModelGraphLink): JsonNode = {
    val n = new ObjectNode(factory)
    n.put("id", l.id)
    n
  }
  
  def encodeLinkDescription(l: ModelGraphLink): JsonNode = {
    val n = new ObjectNode(factory)
    n.put("link_id", encodeLink(l))
    n.put("link_length", l.length)

    // Some optional features
    {
      val min_num_lanes = l.getMin_num_lanes()
      if (min_num_lanes != null)
        n.put("min_num_lanes", min_num_lanes.intValue())
    }

    {
      val max_num_lanes = l.getMax_num_lanes()
      if (max_num_lanes != null)
        n.put("max_num_lanes", max_num_lanes.intValue())
    }

    {
      val max_speed_limit = l.getMax_speed_limit()
      if (max_speed_limit != null)
        n.put("max_speed_limit", max_speed_limit.floatValue())
    }

    {
      val min_speed_limit = l.getMin_speed_limit()
      if (min_speed_limit != null)
        n.put("min_speed_limit", min_speed_limit.floatValue())
    }

    // The start node
    {
      val key = l.getStartNode().asInstanceOf[ModelGraphNode].id
      n.put("start_node", key)
    }
    // The end node
    {
      val key = l.getEndNode().asInstanceOf[ModelGraphNode].id
      n.put("end_node", key)
    }
    // The geometry
    {
      val latlngs = new ArrayNode(factory)
      latlngs.addAll(l.getGeoMultiLine().getCoordinates().map(encode _).toSeq)
      n.put("latlngs", latlngs)
    }
    
    // The end feature
    {
      val features = new ArrayNode(factory)
      for (offset <- l.getSignalLocations().map(_.offset)) {
        features.add(offset)
      }
      n.put("signals", features)      
    }
    
    {
      val features = new ArrayNode(factory)
      for (offset <- l.getStopSignLocations().map(_.offset)) {
        features.add(offset)
      }
      n.put("stops", features)      
    }
    
    n
  }

}
