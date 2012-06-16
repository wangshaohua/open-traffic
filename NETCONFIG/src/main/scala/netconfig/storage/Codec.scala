package netconfig.storage
import netconfig.Link
import netconfig.Spot
import netconfig.Route
import collection.JavaConversions._
import netconfig_extensions.CollectionUtils._
import com.google.common.collect.ImmutableList

trait Codec[L<:Link] {

  def fromLinkID(lid:LinkIDRepr):L
  
  def toLinkID(l:L):LinkIDRepr
  
  def toRepr(s:Spot[L]):SpotRepr = {
    new SpotRepr(toLinkID(s.link), s.offset)
  }
  
  def toRepr(r:Route[L]):RouteRepr = {
    new RouteRepr(r.links.map(toLinkID _), r.spots.map(toRepr _))
  }
  
  def fromRepr(sr:SpotRepr): Spot[L] = {
    Spot.from(fromLinkID(sr.linkId), sr.offset)
  }
  
  def fromRepr(rr:RouteRepr):Route[L] = {
    val links:ImmutableList[L] = rr.links.map(fromLinkID _)
    val spots:ImmutableList[Spot[L]] = rr.spots.map(fromRepr _)
    Route.from(spots, links)
  }
}