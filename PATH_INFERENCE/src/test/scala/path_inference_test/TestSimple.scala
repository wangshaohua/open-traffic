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
import edu.berkeley.path.bots.core._
import path_inference.PathInferenceFilter
import path_inference.PathInferenceParameters2
import path_inference.manager.PathInferenceManager

class BasicTest {

  import PIFUtils._

  /**
   * Simply checks that something runs...
   */
  @Test def basic: Unit = {
    val net = SyntheticNetworks.lineNetwork1

    val pc1 = createPC(1, 1)
    val pc2 = createPC(10, 1)

    val params = new PathInferenceParameters2
    params.returnPoints = true
    params.returnRoutes = true
    val filter: PathInferenceManager = PathInferenceFilter.createManager(params, net.getLinks.asInstanceOf[Array[Link]])
    filter addPoint pc1
    filter addPoint pc2
    filter.finalizeManager
    val out_pcs = filter.getProbeCoordinates
    val out_pis = filter.getPathInferences
    assertEquals(2, out_pcs.length)
    assertEquals(1, out_pis.length)

    filter.finalizeManager
  }

  /**
   * Tests if the framework handles well disconnected segments.
   */
  @Test def unconnected: Unit = {
    val net = SyntheticNetworks.unconnectedLines

    val params = new PathInferenceParameters2
    params.returnPoints = true
    params.returnRoutes = true
    val filter: PathInferenceManager = PathInferenceFilter.createManager(params, net.getLinks.asInstanceOf[Array[Link]])
    filter addPoint createPC(10, 1)
    filter addPoint createPC(40, 1)
    filter addPoint createPC(1030, 1)
    filter addPoint createPC(1050, 1)
    filter.finalizeManager
    val out_pcs = filter.getProbeCoordinates
    val out_pis = filter.getPathInferences
    assertEquals(4, out_pcs.length)
    assertEquals(2, out_pis.length)

    filter.finalizeManager
  }

  /**
   * One point is completely off (unreachable) but the other points are well
   * connected.
   */
  @Test def drop1Point: Unit = {
    val net = SyntheticNetworks.unconnectedLines

    val params = new PathInferenceParameters2
    params.returnPoints = true
    params.returnRoutes = true
    val filter: PathInferenceManager = PathInferenceFilter.createManager(params, net.getLinks.asInstanceOf[Array[Link]])
    filter addPoint createPC(10, 1)
    filter addPoint createPC(20, 1)
    filter addPoint createPC(1030, 1)
    filter addPoint createPC(40, 1)
    filter.finalizeManager
    val out_pcs = filter.getProbeCoordinates
    val out_pis = filter.getPathInferences
    assertEquals(3, out_pcs.length)
    assertEquals(2, out_pis.length)

    filter.finalizeManager
  }

  /**
   * Two points are completely off (unreachable) but the other points are well
   * connected.
   * Even if the outer points may still be connected, the buffer is too large
   * to consider this possibility.
   */
  @Test def drop2Points: Unit = {
    val net = SyntheticNetworks.unconnectedLines

    val params = new PathInferenceParameters2
    params.returnPoints = true
    params.returnRoutes = true
    val filter: PathInferenceManager = PathInferenceFilter.createManager(params, net.getLinks.asInstanceOf[Array[Link]])
    filter addPoint createPC(10, 1)
    filter addPoint createPC(20, 1)
    filter addPoint createPC(1030, 1)
    filter addPoint createPC(1040, 1)
    filter addPoint createPC(50, 1)
    filter.finalizeManager
    val out_pcs = filter.getProbeCoordinates
    val out_pis = filter.getPathInferences
    assertEquals(3, out_pcs.length)
    assertEquals(2, out_pis.length)

    filter.finalizeManager
  }

  /**
   * Tests if the framework handles well disconnected segments.
   */
  @Test def softUnconnected: Unit = {
    val net = SyntheticNetworks.unconnectedParallelLines
    val pc1 = createPC(100, 1)
    val pc2 = createPC(500, 1)
    val pc3 = createPC(800, 1)
    val pc4 = createPC(900, 1)

    val params = new PathInferenceParameters2
    //    params.fillDefaultFor1MinuteOffline
    params.returnPoints = true
    params.returnRoutes = true
    val filter: PathInferenceManager = PathInferenceFilter.createManager(params, net.getLinks.asInstanceOf[Array[Link]])
    filter addPoint pc1
    filter addPoint pc2
    filter addPoint pc3
    filter addPoint pc4
    filter.finalizeManager
    val out_pcs = filter.getProbeCoordinates
    val out_pis = filter.getPathInferences
    assertEquals(4, out_pcs.length)
    assertEquals(2, out_pis.length)

    filter.finalizeManager
  }
}
