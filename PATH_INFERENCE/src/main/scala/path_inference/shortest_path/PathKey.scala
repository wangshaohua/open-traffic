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

package path_inference.shortest_path
import netconfig.Link

/**
 * The paths are indexed using this key.
 *
 * @author tjhunter
 */
final case class PathKey(
  start_link: Link,
  end_link: Link) {
  override lazy val toString = "PathKey(" +
    start_link.toString +
    "->" + end_link.toString + ")"
}
