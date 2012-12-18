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

package netconfig.Datum.storage
import edu.berkeley.path.bots.netconfig.Link
import edu.berkeley.path.bots.netconfig.Datum.ProbeCoordinate
import edu.berkeley.path.bots.core.storage.TimeRepr
import edu.berkeley.path.bots.core.storage.CoordinateRepresentation
import collection.JavaConversions._
import edu.berkeley.path.bots.netconfig.Datum.RouteTT
import edu.berkeley.path.bots.netconfig.Datum.PathInference
import edu.berkeley.path.bots.netconfig.Datum.TSpot
import edu.berkeley.path.bots.math.ImmutableTensor1
import com.google.common.collect.ImmutableCollection
import edu.berkeley.path.bots.netconfig.Spot
import edu.berkeley.path.bots.netconfig_extensions.CollectionUtils._

trait Codec[L <: Link] extends netconfig.storage.Codec[L] {

  /**
   * @param extended_presentation: encodes redundant information like coordinates
   *   (useful for interacting with matlab or python).
   */
  def toRepr(pc: ProbeCoordinate[L], extended_representation: Boolean): ProbeCoordinateRepr = {
    val speed = if (pc.speed == null) {
      None
    } else {
      Some(pc.speed.doubleValue())
    }
    val heading = if (pc.heading == null) {
      None
    } else {
      Some(pc.heading.doubleValue())
    }
    val hired = if (pc.hired == null) {
      None
    } else {
      Some(pc.hired.booleanValue())
    }
    val hdop = if (pc.hdop == null) {
      None
    } else {
      Some(pc.hdop.floatValue())
    }
    new ProbeCoordinateRepr(
      pc.id,
      TimeRepr.toRepr(pc.time),
      CoordinateRepresentation.toRepr(pc.coordinate),
      pc.spots.map(sp => (toRepr(sp, extended_representation))),
      pc.probabilities.toSeq.map(_.doubleValue()),
      speed,
      heading,
      hired,
      hdop)
  }

  /**
   * @param extended_presentation: encodes redundant information like coordinates
   *   (useful for interacting with matlab or python).
   */
  def toRepr(tsp: TSpot[L], extended_representation: Boolean): ProbeCoordinateRepr = {
    toRepr(tsp.toProbeCoordinate(), extended_representation)
  }

  /**
   * @param extended_presentation: encodes redundant information like coordinates
   *   (useful for interacting with matlab or python).
   */
  def toRepr(rtt: RouteTT[L], extended_representation: Boolean): PathInferenceRepr = {
    toRepr(rtt.toPathInference(), extended_representation)
  }

  /**
   * @param extended_presentation: encodes redundant information like coordinates
   *   (useful for interacting with matlab or python).
   */
  def toRepr(pi: PathInference[L], extended_representation: Boolean): PathInferenceRepr = {
    new PathInferenceRepr(pi.id,
      TimeRepr.toRepr(pi.startTime),
      TimeRepr.toRepr(pi.endTime),
      pi.routes.map(r => (toRepr(r, extended_representation))),
      pi.probabilities.toSeq.map(_.doubleValue()))
  }

  def probeCoordinateFromRepr(pcr: ProbeCoordinateRepr): ProbeCoordinate[L] = {
    val speed: java.lang.Float = pcr.speed match {
      case None => null
      case Some(x) => x.floatValue()
    }
    val heading: java.lang.Short = pcr.heading match {
      case None => null
      case Some(x) => x.shortValue()
    }
    val hired: java.lang.Boolean = pcr.hired match {
      case None => null
      case Some(x) => x.booleanValue()
    }
    val hdop: java.lang.Float = pcr.hdop match {
      case None => null
      case Some(x) => x.floatValue()
    }
    val spots = pcr.spots.map(fromRepr _).toArray
    val probs: Array[Double] = pcr.probabilities.toArray
    ProbeCoordinate.from(
      pcr.id,
      TimeRepr.fromRepr(pcr.time),
      CoordinateRepresentation.fromRepr(pcr.coordinate),
      spots,
      probs,
      speed,
      heading,
      hired,
      hdop)
  }

  def tSpotFromRepr(pcr: ProbeCoordinateRepr): TSpot[L] = {
    probeCoordinateFromRepr(pcr).toTSpot()
  }

  def pathInferenceFromRepr(pir: PathInferenceRepr): PathInference[L] = {
    val routes = pir.routes.map(fromRepr _).toArray
    val probs = pir.probabilities.toArray
    PathInference.from(pir.id,
      TimeRepr.fromRepr(pir.startTime),
      TimeRepr.fromRepr(pir.endTime),
      routes,
      probs, null)
    // TODO(tjh) add the hired field
  }
}









