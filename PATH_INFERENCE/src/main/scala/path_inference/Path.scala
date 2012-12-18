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

/**
 * @author tjhunter
 */

package path_inference
import edu.berkeley.path.bots.netconfig.{ Link, Route, Spot }
import edu.berkeley.path.bots.core.MMLogging
import collection.JavaConversions._

/**
 * Internal representation of a route.
 * @todo replace by a route
 */
class Path(start_offset0: Double,
  end_offset0: Double,
  val links: IndexedSeq[Link]) extends MMLogging {
  val epsilon = 0.0000f //Well within the accuracy of any distance metric

  val start_offset = start_offset0
  val end_offset = end_offset0

  // Some validation checks
  assert(start_offset >= 0, this)
  assert(end_offset >= 0, this)
  assert(start_offset <= links.head.length, (start_offset, links.head.length, this))
  assert(end_offset <= links.last.length, (end_offset, links.last.length, this))
  assert(links.forall(_ != null))

  lazy val length: Double = {
      assert(start_offset <= links.head.length, (start_offset, links.head.length, this))
      var l = {
        val l0 = if (links.length == 1) {
          end_offset - start_offset
        } else {
          assert(start_offset <= links.head.length, (start_offset, links.head.length, this))
          links.head.length() - start_offset +
            links.drop(1).dropRight(1).map(_.length).sum +
            end_offset
        }
        assert(l0 >= 0, "Path[FAILURE][" + "," + start_offset + ":" + links + ":" + end_offset + "]" + l0)
        l0
      }
      l
    }

  def toRoute: Route[Link] = {
    Route.from(links, start_offset, end_offset)
  }

  def compatibleStart(spot: Spot[Link]): Boolean = {
    spot.link == links.head && spot.offset == start_offset
  }

  def compatibleEnd(spot: Spot[Link]): Boolean = {
    spot.link == links.last && spot.offset == end_offset
  }
  override def toString: String = "Path[" + "(" + length + ")," + start_offset + ":" + links + ":" + end_offset + "]"
}
