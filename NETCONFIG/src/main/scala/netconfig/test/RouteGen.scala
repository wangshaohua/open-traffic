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
import org.scalacheck.Gen
import org.scalacheck.Choose._
import netconfig.Link
import netconfig.Route
import collection.JavaConversions._

/**
 * A route generator, for use with scalacheck.
 */
object RouteGen {
  private def buildRouteInner[L <: Link](already_built: List[L], choices: List[Int]): Option[List[L]] = {
    if (choices.isEmpty) {
      Some(already_built)
    } else {
      val current_link = already_built.head
      if (current_link.outLinks.isEmpty) {
        None
      } else {
        val num_choices = current_link.outLinks.size
        val next_choice = choices.head % num_choices
        // We need to cast here, unfortunately.
        val next_link = current_link.outLinks().get(next_choice).asInstanceOf[L]
        buildRouteInner(next_link :: already_built, choices.tail)
      }
    }
  }

  /**
   * Creates a sequence of links based on turning indexes.
   * Will fail if one link has no outgoing link.
   * If it succeeds, guaranteed to be of the requested length.
   */
  private def buildRoute[L <: Link](start_link: L, turn_choices: Seq[Int]): Option[IndexedSeq[L]] = {
    for (l <- buildRouteInner(List(start_link), turn_choices.toList)) yield l.reverse.toIndexedSeq
  }

  /**
   * Creates a random generate route of given length with given turns.
   */
  def genRoute[L <: Link](links: IndexedSeq[L], g: Gen[Int], route_length: Int): Gen[Route[L]] = {
    val idxs_gen = Gen.listOfN(route_length, g)
    val start_link_gen = Gen.pick(1, links).map(_.head)
    val start_gen = Gen.choose(0.0, 1.0)
    val end_gen = Gen.choose(0.0, 1.0)
    val r_gen0 = for {
      idxs <- idxs_gen
      start_link <- start_link_gen
      route_links <- buildRoute(start_link, idxs)
    } yield {
      route_links
    }
    val r_gen = r_gen0 suchThat (_ != None) map (_.get)
    val r_gen2 = for {
      route_links <- r_gen
      start_offset <- Gen.choose(0.0, route_links.head.length())
      end_offset <- Gen.choose({
        if (route_links.size > 1) 0.0 else start_offset
      }, route_links.last.length())
    } yield {
      Route.from(route_links, start_offset, end_offset)
    }
    r_gen2
  }

  def genSmallRoutes[L <: Link](links: IndexedSeq[L], max_length: Int): Gen[Route[L]] = {
    val g = Gen.choose(0, 100)
    val gens = for (route_length <- 0 to (max_length + 1)) yield {
      genRoute(links, g, route_length)
    }
    for (i <- Gen.choose(1, max_length); r <- gens(i)) yield r
  }
}