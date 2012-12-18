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

package edu.berkeley.path.bots.netconfig.test

import org.scalatest.prop.Checkers
import org.scalacheck.Arbitrary._
import org.scalacheck.Prop._
import _root_.org.junit.Test
import org.scalacheck.Arbitrary
import edu.berkeley.path.bots.netconfig.Route
import edu.berkeley.path.bots.netconfig.Link
import org.scalatest.junit.JUnitSuite
import edu.berkeley.path.bots.network.gen.test.SimpleGen
import collection.JavaConversions._
import org.scalacheck.Gen

trait RouteTests extends Checkers {

  // Base generator of routes
  val route_gen: Gen[Route[Link]]

  // Creates contiguous pairs of routes by splitting a route.
  lazy val route_pairs_gen = {
    for {
      route <- route_gen
      split_index <- Gen.choose(0, route.links().size() - 1)
      offset <- {
        val num_links = route.links().size()
        val start_offset = if (split_index == 0) {
          route.startOffset()
        } else {
          0.0
        }
        val end_offset = if (split_index == num_links - 1) {
          route.endOffset()
        } else {
          route.links().get(split_index).length()
        }
        Gen.choose(start_offset, end_offset)
      }
    } yield {
      val links1 = route.links.take(split_index + 1)
      val route1 = Route.from(links1, route.startOffset(), offset)
      val links2 = route.links.drop(split_index)
      val route2 = Route.from(links2, offset, route.endOffset())
      (route1, route2)
    }
  }

  implicit def routes = Arbitrary(route_gen)
  implicit def route_pairs = Arbitrary(route_pairs_gen)

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

  def testMergeFun(p: (Route[Link], Route[Link])): Boolean = {
    val (r1, r2) = p
    val r = r1.concatenate(r2)

    val l1 = r1.length()
    val l2 = r2.length()
    val l = r.length()
    math.abs(l - (l1 + l2)) <= Link.LENGTH_PRECISION
  }

  @Test
  def testMerge() {
    check(testMergeFun _)
  }
}

class Route1Test extends JUnitSuite with RouteTests {
  val numLinks = 5
  val net = SimpleGen.line(5)

  val route_gen = RouteGen.genSmallRoutes(net, numLinks)

  // Somehow, this dummy test seems necessary in eclipse??
  @Test
  def testBasic0() {}

  @Test
  def testZeroLength(): Unit = {
    val l = net.head
    // This case was encountered.
    // Not sure if it is a serialization issue or a computation issue.
    val r1 = Route.from(Seq(l), 3.0, 5.0 - 1e-10)
    val r2 = Route.from(Seq(l), 5.0, 5.0)
    assert(testMergeFun((r1, r2)))
  }

  @Test
  def testZeroLength2(): Unit = {
    val l = net.head
    // This case was encountered.
    // Not sure if it is a serialization issue or a computation issue.
    val x = net.head.length()
    val r1 = Route.from(Seq(l), x - 1e-10, x)
    val r2 = Route.from(Seq(l), x, x)
    assert(testMergeFun((r1, r2)))
  }
}
