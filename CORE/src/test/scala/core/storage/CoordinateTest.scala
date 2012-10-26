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

object CoordinateTestUtils {
  def assertEqual(c1: Coordinate, c2: Coordinate): Unit = {
    assertEquals(c1.srid, c2.srid)
    assertEquals(c1.lat, c2.lat, 0.0)
    assertEquals(c1.lon, c2.lon, 0.0)
  }
}

class CoordinateTest {
  import CoordinateTestUtils._

  @Test def test1(): Unit = {
    val c = new Coordinate(1234, 1.0, 2.0)
    val cr = CoordinateRepresentation.toRepr(c)
    val c2 = CoordinateRepresentation.fromRepr(cr)
    assertEqual(c, c2)
  }
  
  @Test def test2(): Unit = {
    val c = new Coordinate(null, 1.0, 2.0)
    val cr = CoordinateRepresentation.toRepr(c)
    val c2 = CoordinateRepresentation.fromRepr(cr)
    assertEqual(c, c2)
  }
  
  @Test def testPG(): Unit = {
    val s = "0101000020E6100000000000C0029B5EC0000000C0E0E54240"
    val res = new Coordinate(4326, 37.795921325683594, -122.42204284667969)
    val c2 = CoordinateRepresentation.fromPGGeometry(s)
    assert(c2!=None)
    assertEqual(c2.get, res)
  }

}