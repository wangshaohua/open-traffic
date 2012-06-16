package core.storage
import core.GeoMultiLine

case class GeoMultiLineRepr(val points:Seq[CoordinateRepresentation]) {
}

object GeoMultiLineRepr {
  def fromRepr(gr:GeoMultiLineRepr):GeoMultiLine = {
    new GeoMultiLine(gr.points.map(CoordinateRepresentation.fromRepr _).toArray)
  }
  
  def toRepr(g:GeoMultiLine):GeoMultiLineRepr = {
    new GeoMultiLineRepr(g.getCoordinates().map(CoordinateRepresentation.toRepr _))
  }
}
