package netconfig_extensions.io
import netconfig.Link
import org.codehaus.jackson.JsonNode

class DummySerializer extends BaseSerializer[Link] {
  val m:Manifest[Link] = scala.reflect.Manifest.singleType(null)
  
  def link(node: JsonNode):Link = {
    assert(false)
    null
  }
  
  def encodeLink(l: Link): JsonNode = {
    assert(false)
    null
  }
  
  def encodeLinkDescription(l: Link): JsonNode = {
    assert(false)
    null
  }
}