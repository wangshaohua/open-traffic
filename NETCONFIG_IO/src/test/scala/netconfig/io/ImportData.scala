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

package netconfig.io

import org.scalatest.junit.JUnitSuite
import org.scalatest.prop.Checkers
import org.scalacheck.Arbitrary._
import org.scalacheck.Prop._
import _root_.org.junit.Test
import netconfig.test.RouteGen
import network.gen.test.SimpleGen
import org.scalacheck.Arbitrary
import netconfig.Route
import netconfig.Link
import netconfig.io.json.ImportProbeData

class ImportDataTest extends JUnitSuite with Checkers {

  @Test def test1(): Unit = {
    val data = """2010-10-09 22:03:22,ajwayd,0101000020E6100000000000E0169B5EC0000000A0DAE04240,t,2010-12-21 11:13:58.655833"""
    val res = ImportProbeData.formatCSVPG(data)
    assert(res!=None)
  }
}