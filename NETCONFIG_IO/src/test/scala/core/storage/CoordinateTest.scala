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

package edu.berkeley.path.bots.core.storage
import org.junit._
import org.junit.Assert._
import edu.berkeley.path.bots.core.Coordinate
import com.codahale.jerkson.Json._

class CoordinateTest {
  @Test def test1(): Unit = {
    val c = new Coordinate(1234, 1.0, 2.0)
    val cr = CoordinateRepresentation.toRepr(c)
    val s1 = """{"srid":1234,"lat":1.0,"lon":2.0}"""
    assertEquals(generate(cr), s1)
    assertEquals(parse[CoordinateRepresentation](s1), cr)
  }
  @Test def test2(): Unit = {
    val c = new Coordinate(null, 1.0, 2.0)
    val cr = CoordinateRepresentation.toRepr(c)
    val s1 = """{"lat":1.0,"lon":2.0}"""
    assertEquals(generate(cr), s1)
    assertEquals(parse[CoordinateRepresentation](s1), cr)
  }

}