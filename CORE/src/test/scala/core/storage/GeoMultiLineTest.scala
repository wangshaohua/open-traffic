package core.storage
import org.junit._
import org.junit.Assert._
import core.Coordinate
import com.codahale.jerkson.Json._
import core.GeoMultiLine


class GeoMultiLineTest {

  @Test def test1():Unit = {
    val c1 = new Coordinate(4326, 1.0, 2.0)
    val c2 = new Coordinate(4326, 3.0, 4.0)
    val g = new GeoMultiLine(Array(c1, c2))
    val s1 = """{"points":[{"srid":4326,"lat":1.0,"lon":2.0},{"srid":4326,"lat":3.0,"lon":4.0}]}"""
    val gr = GeoMultiLineRepr.toRepr(g)
    assertEquals(generate(gr), s1)
    assertEquals(parse[GeoMultiLineRepr](s1),gr)
  }
}