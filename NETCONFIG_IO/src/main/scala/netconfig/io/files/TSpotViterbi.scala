/**
 * Copyright 2012. The Regents of the University of California (Regents).
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy
 * of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

package netconfig.io.files
import org.joda.time.LocalDate
import java.io.File
import edu.berkeley.path.bots.netconfig.io.Dates

object TSpotViterbi {
  case class FileIndex(val feed: String, val nid: Int, val date: LocalDate, val netType: String)

  def list(feed: String, nid: Int, dates: Seq[LocalDate] = Seq.empty[LocalDate], net_type: String): Seq[FileIndex] = {
    val dic = dates.toSet
    val dir_name = "%s/%s/viterbi_tspots_nid%d_%s/".format(Files.dataDir(), feed, nid, net_type)
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
    "%s/%s/viterbi_tspots_nid%d_%s/%s.json.gz".format(Files.dataDir(), feed, nid, net_type, Dates.dateStr(date))
  }

  def fileName(index: FileIndex): String = {
    import index._
    fileName(feed, nid, date, netType)
  }

}