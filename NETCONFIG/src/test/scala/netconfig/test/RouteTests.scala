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

package netconfig.test

import org.scalatest.prop.Checkers
import org.scalacheck.Arbitrary._
import org.scalacheck.Prop._
import _root_.org.junit.Test
import org.scalacheck.Arbitrary
import netconfig.Route
import netconfig.Link
import org.scalatest.junit.JUnitSuite
import network.gen.test.SimpleGen
import collection.JavaConversions._

trait RouteTests extends Checkers {

  implicit def routes: Arbitrary[Route[Link]]

  @Test
  def testBasic() {
    check((r: Route[Link]) => {
      r.length() >= 0
    })
    check((r: Route[Link]) => {
      r.spots.size() >= 2
    })
    check((r: Route[Link]) => {
      r.links.size() >= 1
    })
    check((r: Route[Link]) => {
      val links1 = r.links.toSet
      val links2 = r.spots.map(_.link).toSet
      links1 == links2
    })
  }

  @Test
  def testGeo() {
    check((r: Route[Link]) => {
      val geom = r.geoMultiLine
      geom != null
    })
    check((r: Route[Link]) => {
      val geom = r.geoMultiLine
      geom.getLength() >= 0
    })
  }
}

class Route1Test extends JUnitSuite with RouteTests {
  val numLinks = 5
  lazy val net = SimpleGen.line(5)
  def routes = Arbitrary(RouteGen.genSmallRoutes(net, numLinks))

  // Somehow, this dummy test seems necessary in eclipse??
  @Test
  def testBasic0() {}
}