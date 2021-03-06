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

package core.storage
import core_extensions.MMLogging
import core.Coordinate
import org.postgis.PGgeometry
import org.postgis.Point

case class CoordinateRepresentation(
  var srid: Option[Int],
  var lat: Double,
  var lon: Double) {
}

object CoordinateRepresentation extends MMLogging {
  def toRepr(c: Coordinate) = {
    val srid = if (c.srid == null) {
      None
    } else {
      Some(c.srid.intValue())
    }
    new CoordinateRepresentation(srid, c.lat, c.lon)
  }

  def fromRepr(cr: CoordinateRepresentation): Coordinate = {
    val srid: java.lang.Integer = cr.srid match {
      case None => null
      case Some(i) => i
    }
    new Coordinate(srid, cr.lat, cr.lon)
  }

  /**
   * Parses a PostGres geometry object
   */
  def fromPGGeometry(s: String): Option[Coordinate] = {
    val geom = try { PGgeometry.geomFromString(s) } catch { case _ => logError("Could not convert string "); return None; null }
    geom match {
      case p: Point => {
        // Watch out!! Y<->lat X<->lon...
        Some(new Coordinate(p.getSrid, p.getY, p.getX))
      }
      case _ => logError("geom is not a point:%s" format geom); None
    }
  }
}
