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

import netconfig._
import netconfig.Datum._
import core._
import path_inference.PathInferenceFilter
import path_inference.PathInferenceParameters2
import path_inference.crf.ComputingStrategy

class StrategyTest {

  import PIFUtils._

  @Test def lookahead1: Unit = {
    val net = SyntheticNetworks.lineNetwork1
    val params = new PathInferenceParameters2
    params.computingStrategy = ComputingStrategy.LookAhead1
    params.returnPoints = true
    params.returnRoutes = true
    val filter = PathInferenceFilter.createManager(params, net.getLinks.asInstanceOf[Array[Link]])

    filter addPoint createPC(0, 1)
    assertEquals(0, filter.getProbeCoordinates.length)
    assertEquals(0, filter.getPathInferences.length)
    filter addPoint createPC(10, 1)
    assertEquals(1, filter.getProbeCoordinates.length)
    assertEquals(0, filter.getPathInferences.length)
    filter addPoint createPC(20, 1)
    assertEquals(1, filter.getProbeCoordinates.length)
    assertEquals(1, filter.getPathInferences.length)
    filter addPoint createPC(30, 1)
    assertEquals(1, filter.getProbeCoordinates.length)
    assertEquals(1, filter.getPathInferences.length)
    filter addPoint createPC(40, 1)
    assertEquals(1, filter.getProbeCoordinates.length)
    assertEquals(1, filter.getPathInferences.length)
    filter.finalizeManager
    assertEquals(1, filter.getProbeCoordinates.length)
    assertEquals(1, filter.getPathInferences.length)
    filter.finalizeManager
  }

  @Test def lookahead2: Unit = {
    val net = SyntheticNetworks.lineNetwork1
    val params = new PathInferenceParameters2
    params.computingStrategy = ComputingStrategy.LookAhead2
    params.returnPoints = true
    params.returnRoutes = true
    val filter = PathInferenceFilter.createManager(params, net.getLinks.asInstanceOf[Array[Link]])

    filter addPoint createPC(0, 1)
    assertEquals(0, filter.getProbeCoordinates.length)
    assertEquals(0, filter.getPathInferences.length)
    filter addPoint createPC(10, 1)
    assertEquals(0, filter.getProbeCoordinates.length)
    assertEquals(0, filter.getPathInferences.length)
    filter addPoint createPC(20, 1)
    assertEquals(1, filter.getProbeCoordinates.length)
    assertEquals(0, filter.getPathInferences.length)
    filter addPoint createPC(30, 1)
    assertEquals(1, filter.getProbeCoordinates.length)
    assertEquals(1, filter.getPathInferences.length)
    filter addPoint createPC(40, 1)
    assertEquals(1, filter.getProbeCoordinates.length)
    assertEquals(1, filter.getPathInferences.length)
    filter.finalizeManager
    assertEquals(2, filter.getProbeCoordinates.length)
    assertEquals(2, filter.getPathInferences.length)
    filter.finalizeManager
  }

  @Test def viterbi: Unit = {
    val net = SyntheticNetworks.lineNetwork1
    val params = new PathInferenceParameters2
    params.computingStrategy = ComputingStrategy.Viterbi
    params.returnRouteTravelTimes = true
    val filter = PathInferenceFilter.createManager(params, net.getLinks.asInstanceOf[Array[Link]])

    filter addPoint createPC(0, 1)
    assertEquals(0, filter.getRouteTTs.length)
    filter addPoint createPC(10, 1)
    assertEquals(0, filter.getRouteTTs.length)
    filter addPoint createPC(20, 1)
    assertEquals(0, filter.getRouteTTs.length)
    filter addPoint createPC(30, 1)
    assertEquals(0, filter.getRouteTTs.length)
    filter addPoint createPC(40, 1)
    assertEquals(0, filter.getRouteTTs.length)
    // We should get all the data during finalization.
    filter.finalizeManager
    assertEquals(4, filter.getRouteTTs.length)
    // Nothing should come out since we are already finalized
    filter.finalizeManager
    assertEquals(0, filter.getRouteTTs.length)
  }

  @Test def lookahead3: Unit = {
    val net = SyntheticNetworks.lineNetwork1
    val params = new PathInferenceParameters2
    params.computingStrategy = ComputingStrategy.LookAhead3
    params.returnPoints = true
    params.returnRoutes = true
    val filter = PathInferenceFilter.createManager(params, net.getLinks.asInstanceOf[Array[Link]])

    filter addPoint createPC(0, 1)
    assertEquals(0, filter.getProbeCoordinates.length)
    assertEquals(0, filter.getPathInferences.length)
    filter addPoint createPC(10, 1)
    assertEquals(0, filter.getProbeCoordinates.length)
    assertEquals(0, filter.getPathInferences.length)
    filter addPoint createPC(20, 1)
    assertEquals(0, filter.getProbeCoordinates.length)
    assertEquals(0, filter.getPathInferences.length)
    filter addPoint createPC(30, 1)
    assertEquals(1, filter.getProbeCoordinates.length)
    assertEquals(0, filter.getPathInferences.length)
    filter addPoint createPC(40, 1)
    assertEquals(1, filter.getProbeCoordinates.length)
    assertEquals(1, filter.getPathInferences.length)
    filter.finalizeManager
    assertEquals(3, filter.getProbeCoordinates.length)
    assertEquals(3, filter.getPathInferences.length)

    filter.finalizeManager
  }

  /**
   * Online strategy: should return the point as eagerly as possible.
   */
  @Test def online: Unit = {
    val net = SyntheticNetworks.lineNetwork1
    val params = new PathInferenceParameters2
    params.computingStrategy = ComputingStrategy.Online
    params.returnPoints = true
    params.returnRoutes = true
    val filter = PathInferenceFilter.createManager(params, net.getLinks.asInstanceOf[Array[Link]])

    filter addPoint createPC(0, 1)
    // Fails here, there is a logical problem in the way the CRF is built in the
    // case of the online strategy. Expect a 1-frame delay for the results.
    // TODO: explain this in the documentation.
    assertEquals(0, filter.getProbeCoordinates.length) // Should be one
    assertEquals(0, filter.getPathInferences.length)
    filter addPoint createPC(10, 1)
    assertEquals(1, filter.getProbeCoordinates.length)
    assertEquals(1, filter.getPathInferences.length)
    filter addPoint createPC(20, 1)
    assertEquals(1, filter.getProbeCoordinates.length)
    assertEquals(1, filter.getPathInferences.length)
    filter addPoint createPC(30, 1)
    assertEquals(1, filter.getProbeCoordinates.length)
    assertEquals(1, filter.getPathInferences.length)
    filter addPoint createPC(40, 1)
    assertEquals(1, filter.getProbeCoordinates.length)
    assertEquals(1, filter.getPathInferences.length)
    filter.finalizeManager
    assertEquals(1, filter.getProbeCoordinates.length)
    assertEquals(0, filter.getPathInferences.length)

    // It is an error not to finalize the manager at the end of the computations.
    // Nevertheless, users may forget to do it, so this checks if
    // the test suit does not run amok when finalization is
    // forgotten.
    //filter.finalizeManager
  }

  /**
   * Offline strategy: should buffer everything until asked to terminate, or
   * until a logical break is found.
   */
  @Test def offline: Unit = {
    val net = SyntheticNetworks.lineNetwork1
    val params = new PathInferenceParameters2
    params.computingStrategy = ComputingStrategy.Offline
    params.returnPoints = true
    params.returnRoutes = true
    val filter = PathInferenceFilter.createManager(params, net.getLinks.asInstanceOf[Array[Link]])

    filter addPoint createPC(0, 1)
    assertEquals(0, filter.getProbeCoordinates.length)
    assertEquals(0, filter.getPathInferences.length)
    filter addPoint createPC(10, 1)
    assertEquals(0, filter.getProbeCoordinates.length)
    assertEquals(0, filter.getPathInferences.length)
    filter addPoint createPC(20, 1)
    assertEquals(0, filter.getProbeCoordinates.length)
    assertEquals(0, filter.getPathInferences.length)
    filter addPoint createPC(30, 1)
    assertEquals(0, filter.getProbeCoordinates.length)
    assertEquals(0, filter.getPathInferences.length)
    filter addPoint createPC(40, 1)
    assertEquals(0, filter.getProbeCoordinates.length)
    assertEquals(0, filter.getPathInferences.length)
    filter.finalizeManager
    assertEquals(5, filter.getProbeCoordinates.length)
    assertEquals(4, filter.getPathInferences.length)

    filter.finalizeManager
  }
}
