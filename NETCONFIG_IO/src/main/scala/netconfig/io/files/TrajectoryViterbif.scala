package netconfig.io.files
import org.joda.time.LocalDate
import java.io.File
import netconfig.io.Dates

object TrajectoryViterbif {
  def sanitizeVehicleId(s: String): String = s.filter(_.isLetterOrDigit)

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