package netconfig.io.files
import org.joda.time.LocalDate
import java.io.File
import netconfig.io.Dates

object ProbeCoordinateViterbi {
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