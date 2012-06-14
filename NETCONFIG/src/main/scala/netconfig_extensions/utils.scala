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

/**
 * Some documentaiton on methodoclogies:
 *
 * projectXXXToYYY[L]: tries to convert some data from data structure XXX to data
 * structure YYY. In the cases the conversion may fail because the data content
 * is not appropriate, an option is used as a return type.
 *
 * projectArrayXXXToYYY: array version: returned array my be empty
 *
 * projectXXXAAAToBBB: tries to convert structure expressed in links type AAA to the
 * same structure in links type BBB. Returns an option if the conversion may
 * fail.
 *
 * projectArrayXXXAAAToBBB: converts an array
 *
 * These conversions throw exceptions.
 *
 * mapXXXAAAToBBB
 *
 * map XXXToYYY
 *
 * map XXXAAAToYYYBBB
 *
 */
object NetconfigUtils {
  import ProbeCoordinateUtils._
  import PathInferenceUtils._
  import TSpotUtils._
  import RouteTTUtils._
  import SpotUtils._
  import RouteUtils._
  import LinkUtils._
}
