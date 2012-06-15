package netconfig_extensions.io
import scala.collection.mutable.ArrayBuffer
import core.Time
import org.joda.time.LocalDate
import scala.annotation.tailrec
import scala.sys.SystemProperties
import java.io.File
import core_extensions.MMLogging

object Dates {

  def dateStr(date: LocalDate): String = "%04d-%02d-%02d" format (date.getYear(), date.getMonthOfYear, date.getDayOfMonth)

  def convertInt(s: String): Option[Int] = {
    try {
      Some(s.toInt)
    } catch {
      case _: java.lang.NumberFormatException => None
    }
  }

  def toBerkeleyTime(ld: LocalDate) = Time.newTimeFromBerkeleyDateTime(ld.getYear, ld.getMonthOfYear, ld.getDayOfMonth, 0, 0, 0, 0)

  def fromBerkeleyTime(t: Time) = new LocalDate(t.getYear(), t.getMonth(), t.getDayOfMonth())

  def parseDate(s: String): Option[LocalDate] = {
    s.split("-").map(convertInt _).toList match {
      case Some(year) :: Some(month) :: Some(day) :: Nil => {
        Some(new LocalDate(year, month, day))
      }
      case _ => None
    }
  }

  def parseRange(s: String): Option[Seq[LocalDate]] = {
    s.split(":").map(parseDate _).toList match {
      case Some(from) :: Some(to) :: Nil => Some(dateRange(from, to))
      case _ => None
    }
  }

  def dateRange(from: LocalDate, to: LocalDate): List[LocalDate] = {
    if (from.compareTo(to) <= 0) {
      from :: dateRange(from.plusDays(1), to)
    } else {
      Nil
    }
  }
}

object Files {
  def dataDir() = {
    val s = new SystemProperties
    s.get("mm.data.dir") match {
      case Some(s) => s
      case _ => "/windows/D/arterial_data"
    }
  }

  def setDataDir(s: String): Unit = {
    val sp = new SystemProperties
    sp.put("mm.data.dir", s)
  }
}

object RawProbe extends MMLogging {

  case class FileIndex(val feed: String, val nid: Int, val date: LocalDate)

  def list(feed: String, nid: Int, dates: Seq[LocalDate] = null): Seq[FileIndex] = {
    val dic = if (dates == null) Set.empty[LocalDate] else dates.toSet
    val dir_name = "%s/%s/raw_nid%d/".format(Files.dataDir(), feed, nid)
    val dir = new File(dir_name)
    if (dir.exists()) {
      val regex = """(.*)/(\d\d\d\d-\d\d-\d\d).json.gz""".r
      dir.listFiles().map(_.getAbsolutePath()).flatMap(p => p match {
        case regex(base, ymd) => Dates.parseDate(ymd) match {
          case Some(date) => if (dic.isEmpty || dic.contains(date)) {
            Some(FileIndex(feed, nid, date))
          } else {
            None
          }
          case None => None
        }
        case _ => None
      }).toSeq.sortBy(_.date.toString)
      // Should work, maybe a bug in the pattern matcher?
      //      for (f <- dir.listFiles();
      //          regex(base, ymd) <- f.getCanonicalPath();
      //          date <- Dates.parseDate(ymd)) yield 
      //          FileIndex(feed, nid, date)
    } else {
      Nil
    }
  }

  def fileName(index: FileIndex): String = fileName(index.feed, index.nid, index.date)

  def fileName(feed: String, nid: Int, date: LocalDate): String = {
    "%s/%s/raw_nid%d/%s.json.gz".format(Files.dataDir(), feed, nid, Dates.dateStr(date))
  }
}

object ViterbiProbeCoordinate {
  case class FileIndex(val feed: String, val nid: Int, val date: LocalDate, val netType: String)

  def list(feed: String, nid: Int, dates: Seq[LocalDate] = Seq.empty[LocalDate], net_type: String): Seq[FileIndex] = {
    val dic = dates.toSet
    val dir_name = "%s/%s/viterbi_pcs_nid%d_%s/".format(Files.dataDir(), feed, nid, net_type)
    val dir = new File(dir_name)
    if (dir.exists()) {
      val regex = """(.*)/(\d\d\d\d-\d\d-\d\d).json.gz""".r
      dir.listFiles().map(_.getAbsolutePath()).flatMap(p => p match {
        case regex(base, ymd) => Dates.parseDate(ymd) match {
          case Some(date) => if (dic.isEmpty || dic.contains(date)) {
            Some(FileIndex(feed, nid, date, net_type))
          } else {
            None
          }
          case None => None
        }
        case _ => None
      }).toSeq.sortBy(_.date.toString)
    } else {
      Nil
    }
  }

  def fileName(feed: String, nid: Int, date: LocalDate, net_type: String) = {
    "%s/%s/viterbi_pcs_nid%d_%s/%s.json.gz".format(Files.dataDir(), feed, nid, net_type, Dates.dateStr(date))
  }

  def fileName(index: FileIndex): String = {
    import index._
    fileName(feed, nid, date, netType)
  }
}

object ViterbiPathInference {
  case class FileIndex(val feed: String, val nid: Int, val date: LocalDate, val netType: String)

  def list(feed: String, nid: Int, dates: Seq[LocalDate] = Seq.empty[LocalDate], net_type: String): Seq[FileIndex] = {
    val dic = dates.toSet
    val dir_name = "%s/%s/viterbi_pis_nid%d_%s/".format(Files.dataDir(), feed, nid, net_type)
    val dir = new File(dir_name)
    if (dir.exists()) {
      val regex = """(.*)/(\d\d\d\d-\d\d-\d\d).json.gz""".r
      dir.listFiles().map(_.getAbsolutePath()).flatMap(p => p match {
        case regex(base, ymd) => Dates.parseDate(ymd) match {
          case Some(date) => if (dic.isEmpty || dic.contains(date)) {
            Some(FileIndex(feed, nid, date, net_type))
          } else {
            None
          }
          case None => None
        }
        case _ => None
      }).toSeq.sortBy(_.date.toString)
    } else {
      Nil
    }
  }

  def fileName(feed: String, nid: Int, date: LocalDate, net_type: String) = {
    "%s/%s/viterbi_pis_nid%d_%s/%s.json.gz".format(Files.dataDir(), feed, nid, net_type, Dates.dateStr(date))
  }

  def fileName(index: FileIndex): String = {
    import index._
    fileName(feed, nid, date, netType)
  }
}

object ViterbiTrajectory {
  def sanitizeVehicleId(s:String):String = s.filter(_.isLetterOrDigit)

case class FileIndex(val feed: String,
    val nid: Int,
    val date: LocalDate,
    val netType: String,
    val vehicle: String,
    val trajIndex: Int)

  def list(feed: String, nid: Int, net_type: String,
    dates: Seq[LocalDate] = Seq.empty[LocalDate],
    drivers: Seq[String] = Seq.empty[String],
    traj_indexes: Seq[Int] = Seq.empty[Int]): Seq[FileIndex] = {
    val dic_dates = dates.toSet
    val dic_drivers = drivers.toSet
    val dic_tidxs = traj_indexes.toSet
    val dir_name = "%s/%s/viterbi_pcs_nid%d_%s/".format(Files.dataDir(), feed, nid, net_type)
    val dir = new File(dir_name)
    if (dir.exists()) {
      val day_regex = """(.*)/(\d\d\d\d-\d\d-\d\d)/""".r
      val regex = """(.*)(\d\d\d\d-\d\d-\d\d)/(\w*)_(\d\d\d\d).json.gz""".r
      val good_dates = dir.listFiles().map(_.getAbsolutePath()).flatMap(p => p match {
        case day_regex(base, ymd) => Dates.parseDate(ymd) match {
          case Some(date) if (dic_dates.isEmpty || dic_dates.contains(date)) => {
            val f = new File(p)
            if (f.isDirectory())
            	Some(f)
            else
              None
          }
          case None => None
        }
        case _ => None
      })
      good_dates.flatMap(f => {
        f.listFiles().map(_.getAbsolutePath()).flatMap(p => p match {
                case regex(base, ymd, vehicle, tr_idx) => (Dates.parseDate(ymd), Dates.convertInt(tr_idx)) match {
                  case (Some(date), Some(idx)) => {
                    if ((dic_drivers.isEmpty || dic_drivers.contains(vehicle)) &&
                      (dic_tidxs.isEmpty || dic_tidxs.contains(idx))) {
                      Some(FileIndex(feed, nid, date, net_type, vehicle, idx))
                    } else None
                  }
                  case _ => None
                }
                case _ => None
          
        }).toSeq.sortBy(_.vehicle)
      }).toSeq.sortBy(_.date.toString)
    } else {
      Nil
    }
  }

  def fileName(feed: String, nid: Int, date: LocalDate, net_type: String, vehicle: String, traj_idx: Int) = {
    "%s/%s/viterbi_trajs_nid%d_%s/%s/%s_%04d.json.gz"
      .format(Files.dataDir(), feed, nid, net_type, Dates.dateStr(date),
        sanitizeVehicleId(vehicle), traj_idx)
  }
}

object SerializedNetwork {
  def fileName(nid:Int, net_type:String):String = {
    "%s/network_nid%d_%s.json.gz".format(Files.dataDir(), nid, net_type)
    
  }
}

object Test {
  import netconfig_extensions.io.RawProbe._
  import netconfig_extensions.io.Files._
  setDataDir("//home/tjhunter/tmp/data/")
  list("navteq", 1)
}
