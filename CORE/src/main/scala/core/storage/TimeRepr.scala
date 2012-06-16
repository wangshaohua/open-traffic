package core.storage
import core.Time
import core_extensions.MMLogging

case class TimeRepr(
    var locale:String,
    var year:Int,
    var month:Int,
    var day:Int,
    var hour:Int,
    var minute:Int,
    var second:Int,
    var milli:Int) {
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
}