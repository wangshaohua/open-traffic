package core
import org.junit._
import org.junit.Assert._
import org.joda.time.Duration
import core.TimeUtils
import core_extensions.MMLogging

class TimeUtilsTest extends MMLogging {

  @Test
  def test1():Unit = {
    val t = new Time
    val t2 = t + 50
    assert(t <= t2)
  }
  
  @Test
  def test2():Unit = {
    val t = new Time
    val ts = (0 until 4).map(i => t + (i * 1.0).toFloat)
    val d1 = new Duration(900)
    logInfo("d1 "+d1)
    val d2 = new Duration(1100)
    val ints = TimeUtils.splitByInterval(d1,d2,ts)
    assertEquals(2, ints.size)
  }

  @Test
  def test3():Unit = {
    val t = new Time
    val ts = (0 until 10).map(i => t + (i * 1.0).toFloat)
    val d1 = new Duration(90000)
    val d2 = new Duration(110000)
    val ints = TimeUtils.splitByInterval(d1,d2,ts)
    assertEquals(0, ints.size)
  }

  @Test
  def test4():Unit = {
    val t = new Time
    val ts = (0 until 10).map(i => t + (i * 1.0).toFloat)
    val d1 = new Duration(3900)
    val d2 = new Duration(4100)
    val ints = TimeUtils.splitByInterval(d1,d2,ts)
    assertEquals(2,ints.size)
  }
  }