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

package core.storage
import org.junit._
import org.junit.Assert._
import core_extensions.MMLogging

class TimeReprTest extends MMLogging {

  @Test def test1(): Unit = {
    val x1 = "2012-03-05 10:11:12.120"
    val t1 = new TimeRepr("berkeley", 2012, 3, 5, 10, 11, 12, 120)
    val t2 = TimeRepr.berkeleyFromString(x1)
    logInfo("t2 = "+t2)
    assertEquals(Some(t1), t2)
  }

}