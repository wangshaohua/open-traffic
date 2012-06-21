package core.storage
import org.junit._
import org.junit.Assert._

class TimeReprTest {

  @Test def test1():Unit = {
    val x1 = "2012-03-05 10:11:12.120"
    val t1 = new TimeRepr("berkeley",2012,3,5, 10, 11, 12, 120)
    val t2 = TimeRepr.berkeleyFromString(x1)
    assertEquals(Some(t1), t2)
  }

}