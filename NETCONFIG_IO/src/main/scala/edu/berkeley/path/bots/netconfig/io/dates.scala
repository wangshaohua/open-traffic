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
package edu.berkeley.path.bots.netconfig.io
import edu.berkeley.path.bots.core.Time
import org.joda.time.LocalDate

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
