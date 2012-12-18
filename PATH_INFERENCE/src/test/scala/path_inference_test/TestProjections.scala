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
package path_inference_test

import org.junit._
import org.junit.Assert._

import edu.berkeley.path.bots.netconfig._
import edu.berkeley.path.bots.netconfig.Datum._
import edu.berkeley.path.bots.netconfig.projection.ProjectorFactory
import edu.berkeley.path.bots.core._

object PIFUtils {
  def createPC(x: Double, y: Double, id: String = "fakeid"): ProbeCoordinate[Link] = {
    val c = new Coordinate(Coordinate.SRID_CARTESIAN, x, y)
    ProbeCoordinate.from(id, new Time, c)
  }
}

/**
 * Simple tests of the projector on a linaear network.
 *
 * @todo should be moved to netconfig, but somehow tests do not run
 * there...
 */
class BasicProjectionTest {

  @Test def simple: Unit = {
    val net = SyntheticNetworks.lineNetwork1
    val projector = ProjectorFactory.fromLinks(net.getLinks)
    val c = new Coordinate(Coordinate.SRID_CARTESIAN, 1, 1)
    val sps = projector.getClosestLinks(c, 10f, 100)
    assertEquals(1, sps.length)
  }

  @Test def farFromEnds: Unit = {
    val net = SyntheticNetworks.lineNetwork1
    val projector = ProjectorFactory.fromLinks(net.getLinks)
    val c = new Coordinate(Coordinate.SRID_CARTESIAN, 50, 1)
    val sps = projector.getClosestLinks(c, 10f, 100)
    assertEquals(1, sps.length)
  }
}
