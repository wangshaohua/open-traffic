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

package network.arterial1

import org.junit._
import org.junit.Assert._
import com.codahale.jerkson.Json._
import network.gen.GenericLinkRepresentation
import netconfig.Route
import netconfig.storage._
import netconfig.io.json.JSonSerializer
import core_extensions.MMLogging

class ProjectionTest extends MMLogging {

  // scalastyle:off
  private val string = """{"id":{"primary":93335,"secondary":0},"length":154.1519,"startNodeId":{"primary":73582,"secondary":-1},"endNodeId":{"primary":73595,"secondary":-1},"geom":{"points":[{"lat":37.77940800339694,"lon":-122.50014904738555,"srid":4326},{"lat":37.77942800339694,"lon":-122.50074904738554,"srid":4326},{"lat":37.77944788832373,"lon":-122.5009872401315,"srid":4326},{"lat":37.77954784525461,"lon":-122.5018868525094,"srid":4326}]},"endFeature":"nothing","numLanes":-1,"speedLimit":11}"""
  // scalastyle:on

  lazy val net = {
    val input = string.lines.toSeq.map(s => parse[GenericLinkRepresentation](s))
    val links = NetworkBuilder.build(input)
    links.map(l => (l.id, l)).toMap
  }

  @Test def test1 = {
    val lr = LinkIDRepr(93335, 0)
    logInfo("link length: " + net(lr).length)
    val r1r = RouteRepr(Seq(lr), Seq(SpotRepr(lr, 154.151993), SpotRepr(lr, 154.151993)))
    val r2r = RouteRepr(Seq(lr), Seq(SpotRepr(lr, 154.151993), SpotRepr(lr, 154.151993)))
    val codec = JSonSerializer.from(net)
    val r1 = codec.fromRepr(r1r)
    val r2 = codec.fromRepr(r2r)
    val r = r1.concatenate(r2)
  }
}