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
package network.simple

import edu.berkeley.path.bots.netconfig.Node
import java.io.Serializable
import edu.berkeley.path.bots.core.Coordinate

class SimpleNode(c: Coordinate, _id: NodeKey) extends Node with Serializable {
  val coordinate = c
  val id = _id

  override def hashCode: Int = id.hashCode

  override def equals(other: Any): Boolean = {
    other match {
      case sn: SimpleNode => sn.id == id
      case _ => assert(false); false
    }
  }

  override def toString: String = "SimpleNode[" + id + "]"

  def moved(dx: Double, dy: Double): SimpleNode = {
    val c2 = new core.Coordinate(c.srid, c.lat + dx, c.lon + dy)
    new SimpleNode(c2, id)
  }
}
