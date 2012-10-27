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
import org.junit._
import org.junit.Assert._

class TrajectoryViterbiTest extends JUnitSuite with Checkers {

  @Test
  def testF1: Unit = {
    val fname = "/dar1/dir2/source/viterbi_trajs_nid108_arterial/2010-10-10/"
    val TrajectoryViterbi.day_regex(base, ymd) = fname
    assertEquals(ymd, "2010-10-10")
  }

  @Test
  def testF2: Unit = {
    val fname = "/dar1/dir2/source/viterbi_trajs_nid108_arterial/2010-10-10"
    val TrajectoryViterbi.day_regex(base, ymd) = fname
    assertEquals(ymd, "2010-10-10")
  }
}