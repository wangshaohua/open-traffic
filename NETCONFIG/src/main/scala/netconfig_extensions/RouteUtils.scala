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
package netconfig_extensions

import collection.JavaConversions._
import collection.mutable.{ ArrayBuffer, Map }
import edu.berkeley.path.bots.core.MMLogging
import edu.berkeley.path.bots.netconfig.Link
import edu.berkeley.path.bots.netconfig.Route

object RouteUtils extends MMLogging {

  import SpotUtils._

  /**
   * Takes a route and segments it into a sequence of NT link / start offset / end offset
   */
  def convertRouteToSegments[L <: Link](r: Route[L]): Seq[(L, Double, Double)] = {
    // Conversion to a vector to use the (cleaner) collection syntax
    val links: Vector[L] = Vector.empty[L] ++ r.links
    if (links.length == 1)
      List((links.head, r.startOffset, r.endOffset))
    else {
      List((links.head, r.startOffset.toDouble, links.head.length.toDouble)) ++
        links.slice(1, links.length).map(l => (l, 0.0, l.length.toDouble)) ++
        List((links.last, 0.0, r.endOffset.toDouble))
    }
  }
}
