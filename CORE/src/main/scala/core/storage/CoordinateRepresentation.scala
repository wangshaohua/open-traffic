package core.storage
import core.Coordinate

case class CoordinateRepresentation(
    var srid:Option[Int],
    var lat:Double,
    var lon:Double) {
}

object CoordinateRepresentation {
  def toRepr(c:Coordinate) = {
    val srid = if (c.srid==null) {
      None
    } else {
      Some(c.srid.intValue())
    }
    new CoordinateRepresentation(srid, c.lat, c.lon)
  }
  
  def fromRepr(cr:CoordinateRepresentation):Coordinate = {
    val srid:java.lang.Integer = cr.srid match {
      case None => null
      case Some(i) => i
    }
    new Coordinate(srid, cr.lat, cr.lon)
  }
}
