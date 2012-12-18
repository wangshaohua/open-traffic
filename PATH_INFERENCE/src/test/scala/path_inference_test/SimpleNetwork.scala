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

import edu.berkeley.path.bots.network.simple._
import edu.berkeley.path.bots.core.Coordinate
import edu.berkeley.path.bots.netconfig.projection.ProjectorFactory
import org.junit._
import org.junit.Assert._
import edu.berkeley.path.bots.network.simple.SimpleNetwork
import edu.berkeley.path.bots.network.simple.SimpleNetworkBuilder

object SyntheticNetworks {

  def lineNetwork1: SimpleNetwork = {
    val builder = new SimpleNetworkBuilder
    val c1 = new Coordinate(Coordinate.SRID_CARTESIAN, 0, 0)
    val c2 = new Coordinate(Coordinate.SRID_CARTESIAN, 100, 0)

    val n1 = builder.addNode(c1)
    val n2 = builder.addNode(c2)

    val l1 = builder.addLink(n1, n2)
    builder.getNetwork
  }

  /**
   * 2 disconnected lines, very far appart:
   * (0,100) and (1100, 1200)
   */
  def unconnectedLines: SimpleNetwork = {
    val builder = new SimpleNetworkBuilder

    {
      val c1 = new Coordinate(Coordinate.SRID_CARTESIAN, 0, 0)
      val c2 = new Coordinate(Coordinate.SRID_CARTESIAN, 100, 0)
      val n1 = builder.addNode(c1)
      val n2 = builder.addNode(c2)
      builder.addLink(n1, n2)
    }
    {
      val c1 = new Coordinate(Coordinate.SRID_CARTESIAN, 1000, 0)
      val c2 = new Coordinate(Coordinate.SRID_CARTESIAN, 1100, 0)
      val n1 = builder.addNode(c1)
      val n2 = builder.addNode(c2)
      builder.addLink(n1, n2)
    }
    builder.getNetwork
  }

  /**
   * Two parallel links, unconnected: forces smooth logical break.
   * (0,600) and (400,1000)
   */
  def unconnectedParallelLines: SimpleNetwork = {
    val builder = new SimpleNetworkBuilder

    {
      val c1 = new Coordinate(Coordinate.SRID_CARTESIAN, 0, 0)
      val c2 = new Coordinate(Coordinate.SRID_CARTESIAN, 600, 0)
      val n1 = builder.addNode(c1)
      val n2 = builder.addNode(c2)
      builder.addLink(n1, n2)
    }
    {
      val c1 = new Coordinate(Coordinate.SRID_CARTESIAN, 400, 1)
      val c2 = new Coordinate(Coordinate.SRID_CARTESIAN, 1000, 1)
      val n1 = builder.addNode(c1)
      val n2 = builder.addNode(c2)
      builder.addLink(n1, n2)
    }
    builder.getNetwork
  }

  /**
   * 2 disconnected lines, very far appart:
   * (0,100) and (1000, 1100) and (2000, 2100)
   */
  def unconnected3Lines: SimpleNetwork = {
    val builder = new SimpleNetworkBuilder

    {
      val c1 = new Coordinate(Coordinate.SRID_CARTESIAN, 0, 0)
      val c2 = new Coordinate(Coordinate.SRID_CARTESIAN, 100, 0)
      val n1 = builder.addNode(c1)
      val n2 = builder.addNode(c2)
      builder.addLink(n1, n2)
    }
    {
      val c1 = new Coordinate(Coordinate.SRID_CARTESIAN, 1000, 0)
      val c2 = new Coordinate(Coordinate.SRID_CARTESIAN, 1100, 0)
      val n1 = builder.addNode(c1)
      val n2 = builder.addNode(c2)
      builder.addLink(n1, n2)
    }
    {
      val c1 = new Coordinate(Coordinate.SRID_CARTESIAN, 2000, 0)
      val c2 = new Coordinate(Coordinate.SRID_CARTESIAN, 2100, 0)
      val n1 = builder.addNode(c1)
      val n2 = builder.addNode(c2)
      builder.addLink(n1, n2)
    }
    builder.getNetwork
  }

  /**
   * 2 lines running concurrently from (0,0) to (100,0) and from (100,0) to (0,0)
   * The two lines are not connected.
   */
  def concurrentLines: SimpleNetwork = {
    val builder = new SimpleNetworkBuilder
    val c1 = new Coordinate(Coordinate.SRID_CARTESIAN, 0, 0)
    val c2 = new Coordinate(Coordinate.SRID_CARTESIAN, 100, 0)

    {
      val n1 = builder.addNode(c1)
      val n2 = builder.addNode(c2)
      builder.addLink(n1, n2)
    }
    {
      val n1 = builder.addNode(c1)
      val n2 = builder.addNode(c2)
      builder.addLink(n2, n1)
    }
    builder.getNetwork

  }
}

class SimpleNetworkTest_ {

  @Test def test1: Unit = {

    val builder = new SimpleNetworkBuilder
    val c1 = new Coordinate(Coordinate.SRID_CARTESIAN, 0, 0)
    val c2 = new Coordinate(Coordinate.SRID_CARTESIAN, 0, 100)

    val n1 = builder.addNode(c1)
    val n2 = builder.addNode(c2)

    val l1 = builder.addLink(n1, n2)

    val net = builder.getNetwork

  }

  @Test def test3: Unit = {

    val builder = new SimpleNetworkBuilder
    val c1 = new Coordinate(Coordinate.SRID_CARTESIAN, 0, 0)
    val c2 = new Coordinate(Coordinate.SRID_CARTESIAN, 0, 100)

    val n1 = builder.addNode(c1)
    val n2 = builder.addNode(c2)

    val l1 = builder.addLink(n1, n2)

    val net = builder.getNetwork

    val projector = ProjectorFactory.fromLinks(Array(l1))

    val c3 = new Coordinate(Coordinate.SRID_CARTESIAN, 10, -20)
    projector.getClosestLinks(c3, 100, 10)

  }
}
