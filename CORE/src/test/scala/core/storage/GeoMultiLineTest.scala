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
import core.Coordinate
import core.GeoMultiLine

class GeoMultiLineTest {

  @Test def test1(): Unit = {
    val c1 = new Coordinate(4326, 1.0, 2.0)
    val c2 = new Coordinate(4326, 3.0, 4.0)
    val g = new GeoMultiLine(Array(c1, c2))
    //    val s1 = """{"points":[{"srid":4326,"lat":1.0,"lon":2.0},{"srid":4326,"lat":3.0,"lon":4.0}]}"""
    val gr = GeoMultiLineRepr.toRepr(g)
    //    assertEquals(generate(gr), s1)
    //    assertEquals(parse[GeoMultiLineRepr](s1),gr)
  }
}