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

import org.junit._
import org.junit.Assert._
import org.joda.time.Duration
import edu.berkeley.path.bots.core.Time
import edu.berkeley.path.bots.core.TimeUtils;
import edu.berkeley.path.bots.core.MMLogging

class TimeUtilsTest extends MMLogging {

  @Test
  def test1(): Unit = {
    val t = new Time
    val t2 = t + 50
    assert(t <= t2)
  }

  @Test
  def test2(): Unit = {
    val t = new Time
    val ts = (0 until 4).map(i => t + (i * 1.0).toFloat)
    val d1 = new Duration(900)
    logInfo("d1 " + d1)
    val d2 = new Duration(1100)
    val ints = TimeUtils.splitByInterval(d1, d2, ts)
    assertEquals(2, ints.size)
  }

  @Test
  def test3(): Unit = {
    val t = new Time
    val ts = (0 until 10).map(i => t + (i * 1.0).toFloat)
    val d1 = new Duration(90000)
    val d2 = new Duration(110000)
    val ints = TimeUtils.splitByInterval(d1, d2, ts)
    assertEquals(0, ints.size)
  }

  @Test
  def test4(): Unit = {
    val t = new Time
    val ts = (0 until 10).map(i => t + (i * 1.0).toFloat)
    val d1 = new Duration(3900)
    val d2 = new Duration(4100)
    val ints = TimeUtils.splitByInterval(d1, d2, ts)
    assertEquals(2, ints.size)
  }
}