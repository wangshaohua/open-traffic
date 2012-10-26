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
import netconfig_extensions.projection.ProjectorFactory
import core._
import org.apache.commons.math.random.MersenneTwister
import org.apache.commons.math.random.RandomDataImpl
import path_inference.PathInferenceParameters2
import path_inference.manager.PathInferenceManager
import path_inference.PathInferenceFilter
import core_extensions.MMLogging

/**
 * Tests the high frequency grid strategy.
 */
class HighFreqTest extends MMLogging {

  val gen = new MersenneTwister(0)
  val rand = new RandomDataImpl(gen)
  def randomCoordinate(x:Double, y:Double, std_dev:Double):Coordinate = {
    val x1 = x + (if(std_dev > 0) {rand.nextGaussian(0, std_dev)} else {0.0})
    val y1 = y + (if(std_dev > 0) {rand.nextGaussian(0, std_dev)} else {0.0})
    new Coordinate(Coordinate.SRID_CARTESIAN, x1, y1)
  }

  @Test
  def testNoisy1:Unit = {
    val net = SyntheticNetworks.concurrentLines
    // Network has length 100
    val num_points = 10
    val std_dev = 0.0
    val dt = 2.0 //Seconds
    val dx = 10.0
    val t0 = new Time
    val points = (0 until num_points).map(i => {
      val x = dx * i
      val y = 0
      val c = randomCoordinate(x,y,std_dev)
      val t = t0 + (i * dt).toFloat
      // Casting is ncessary here due to java type missing covariance information.
      val pc:ProbeCoordinate[Link] = ProbeCoordinate.from("id",t,c)
      pc
    })
    logInfo("Input points:")
    logInfo(points.mkString("\n"))

    val params = new PathInferenceParameters2
    params.fillDefaultForHighFreqOnline
    params.returnPoints = true
    params.returnRoutes = true
    val filter: PathInferenceManager = PathInferenceFilter.createManager(params, net.getLinks.asInstanceOf[Array[Link]])
    for (pc <- points) {
      filter addPoint pc
    }
    filter finalizeManager
    val out_pcs = filter.getProbeCoordinates
    val out_pis = filter.getPathInferences
    logInfo("PCS:" + out_pcs.mkString("\n"))
    logInfo("PIS:" + out_pis.mkString("\n"))
    assertEquals(out_pcs.size,points.size)
    assertEquals(out_pis.size,points.size-1)
    // TODO: add more checks to make sure it is mapped on the right side of the road.
  }
}