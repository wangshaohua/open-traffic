package core.storage
import core.Time
import core_extensions.MMLogging

case class TimeRepr  (
    var locale:String,
    var year:Int,
    var month:Int,
    var day:Int,
    var hour:Int=0,
    var minute:Int=0,
    var second:Int=0,
    var milli:Int=0) extends MMLogging {
}

object TimeRepr extends MMLogging {
  def fromRepr(r:TimeRepr):Time = {
    if (r.locale != "berkeley") {
      logError("Only berkeley time supported for now")
      throw new IllegalArgumentException()
    }
    Time.newTimeFromBerkeleyDateTime(r.year, r.month, r.day, r.hour, r.minute, r.second, r.milli)
  }
  
  def toRepr(t:Time):TimeRepr = {
    new TimeRepr(
        "berkeley",
        t.getYear(),
        t.getMonth(),
        t.getDayOfMonth(),
        t.getHour(),
        t.getMinute(),
        t.getSecond(),
        t.getMillisecond())
  }
  
  /**
   * Format is YYYY-MM-DD [HH:[MM:[SS[.SSS]]]]
   */
  def berkeleyFromString(s:String):Option[TimeRepr] = {
    val r1 = """([0-9]+)\-([0-9]*[1-9]+)\-([0-9]*[1-9]+)(.*)""".r
    s match {
      case r1(y,m,d,o) => {
        val year = y.toInt
        val month = m.toInt
        val day = d.toInt
        val os = o.replace(" ","").split(":")
        val hour = if(os.length>=1) {
          os(0).toInt
        } else {
          0
        }
        val minute = if(os.length>=2) {
          os(1).toInt
        } else {
          0
        }
        val second = if(os.length>=3) {
          math.floor(os(2) toDouble) toInt
        } else {
          0
        }
        val milli = if(os.length>=3) {
          math.floor(1 + 1000*((os(2) toDouble)%1)) toInt
        } else {
          0
        }
        Some(TimeRepr("berkeley",year, month, day, hour, minute, second, milli))
      }
      case _ => None
    }
  }
}