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

package core
import core_extensions.MMLogging
import org.joda.time.Duration

object TimeUtils extends MMLogging {

  implicit object TimeOrdering extends Ordering[Time] {

    def compare(t1: Time, t2: Time) =
      t1.getTimeInMillis.compare(t2.getTimeInMillis)

  }

  /**
   * Splits an ordered sequence of times by respecting some duration constraints between each time.
   * Reutnrs a sequence of splits.
   */
  def splitByInterval(start_interval: Duration, end_interval: Duration, times: Seq[Time]): Seq[Seq[Time]] = {
    // This code is complicated (functional programming + recusrion) but correct.
    // TODO(tjh) explain a bit what is going on...
    // Complexity: worst case is quadratic
    def seq_duration(seq: Seq[Time]): Duration = {
      val start = seq.head.toDateTime()
      val end = seq.last.toDateTime()
      new Duration(start, end)
    }
    def inner(acc: Seq[Time], rem: Seq[Time]): Option[(Seq[Time], Seq[Time])] = {
      //      logInfo("Called inner: acc=%s, rem=%s" format (acc.toString, rem.toString))
      if (rem.isEmpty) {
        if (acc.isEmpty) {
          None
        } else {
          val seq_dur = seq_duration(acc)
          //          logInfo("seq duration is "+seq_dur)
          if (seq_dur.isShorterThan(end_interval) && seq_dur.isLongerThan(start_interval)) {
            //            logInfo("seq: "+acc)
            Some((acc, rem))
          } else {
            None
          }
        }
      } else {
        val x = rem.head
        inner(acc ++ Seq(x), rem.tail) match {
          case None => {
            //            logInfo("Failed to add "+x)
            inner(acc, Seq.empty).map(z => (z._1, rem))
          }
          case x => x
        }
      }
    }
    def main_fun(seq: Seq[Time]): Seq[Seq[Time]] = {
      if (seq.isEmpty) {
        Seq.empty
      } else {
        inner(Seq(seq.head), seq.tail) match {
          case Some((s, rem)) => {
            Seq(s) ++ main_fun(rem)
          }
          case None => main_fun(seq.tail)
        }
      }
    }
    main_fun(times)
  }
}
