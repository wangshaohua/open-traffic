package core.storage
import org.junit._
import org.junit.Assert._
import core.Coordinate
import com.codahale.jerkson.Json._

class CoordinateTest {
  @Test def test1(): Unit = {
    val c = new Coordinate(1234, 1.0, 2.0)
    val cr = CoordinateRepresentation.toRepr(c)
    val s1 = """{"srid":1234,"lat":1.0,"lon":2.0}"""
    assertEquals(generate(cr), s1)
    assertEquals(parse[CoordinateRepresentation](s1),cr)
  }
  @Test def test2(): Unit = {
    val c = new Coordinate(null, 1.0, 2.0)
    val cr = CoordinateRepresentation.toRepr(c)
    val s1 = """{"lat":1.0,"lon":2.0}"""
    assertEquals(generate(cr), s1)
    assertEquals(parse[CoordinateRepresentation](s1),cr)
  }

}