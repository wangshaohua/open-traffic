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

package netconfig_extensions.projection
import netconfig.Link

/**
 * A collection of methods to build projectors.
 *
 * '''Note''': this can be used directly from java, do not ask me some crap
 * about writing a wrapper. Case in point:
 * {{{
 *  import netconfig_extensions.projection.ProjectorFactory;
 *  import netconfig_extensions.projection.Projector;
 *
 *  Projector<Link> projector = ProjectorFactory.fromLinks(...)
 *
 *  // And we test
 *  Coordinate c = ...
 *  Spot<Link>[] spots = projector.getClosestLinks(c,10f,100)
 *
 * }}}
 *
 * @author tjhunter
 */
object ProjectorFactory {
  def fromLinks[L <: Link](links: Array[L]): Projector[L] =
    new KDProjector(links)
}
