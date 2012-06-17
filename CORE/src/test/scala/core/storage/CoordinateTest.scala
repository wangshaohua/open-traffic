package core.storage
import org.junit._
import org.junit.Assert._
import core.Coordinate

object CoordinateTestUtils {
  def assertEqual(c1:Coordinate, c2:Coordinate):Unit = {
    assertEquals(c1.srid, c2.srid)
    assertEquals(c1.lat, c2.lat, 0.0)
    assertEquals(c1.lon, c2.lon, 0.0)
  }
}

class CoordinateTest {
  import CoordinateTestUtils._
  
  @Test def test1(): Unit = {
    val c = new Coordinate(1234, 1.0, 2.0)
    val cr = CoordinateRepresentation.toRepr(c)
    val c2 = CoordinateRepresentation.fromRepr(cr)
    assertEqual(c, c2)
  }
  @Test def test2(): Unit = {
    val c = new Coordinate(null, 1.0, 2.0)
    val cr = CoordinateRepresentation.toRepr(c)
    val c2 = CoordinateRepresentation.fromRepr(cr)
    assertEqual(c, c2)
  }

}