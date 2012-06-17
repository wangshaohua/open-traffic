package netconfig.io.files
import org.joda.time.LocalDate
import java.io.File
import netconfig.io.Dates

object RawProbe {

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