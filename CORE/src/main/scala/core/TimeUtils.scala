package core
import core_extensions.MMLogging
import org.joda.time.Duration

  object TimeUtils extends MMLogging {
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
      def main(seq: Seq[Time]): Seq[Seq[Time]] = {
        if (seq.isEmpty) {
          Seq.empty
        } else {
          inner(Seq(seq.head), seq.tail) match {
            case Some((s, rem)) => {
              logInfo("")
              Seq(s) ++ main(rem)
            }
            case None => main(seq.tail)
          }
        }
      }
      main(times)
    }
  }
