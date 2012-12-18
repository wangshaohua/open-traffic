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

/**
 * @author tjhunter
 */
package path_inference

import edu.berkeley.path.bots.netconfig.Link
import path_inference.manager.PathInferenceManager
import path_inference.manager.ProjectionHookInterface
import path_inference.manager._
import path_inference.models.TransitionModel
import path_inference.models.ObservationModel
import path_inference.shortest_path.PathGenerator2
import scala.collection.JavaConversions._

/**
 * All the code used to create a path inference filter. This
 * is all you probably care about.
 *
 * Also take a look at the documentation of the parameters
 * in [[path_inference.PathInferenceParameters2]] and the
 * documentation of the filter in [[path_inference.manager.PathInferenceManger]]
 */
object PathInferenceFilter {

  def createManager(params: PathInferenceParameters2): PathInferenceManager = {
    create(params)
  }

  def createManager(params: PathInferenceParameters2, projector: ProjectionHookInterface): PathInferenceManager = {
    create(params, projector = projector)
  }

  def createManager(
    params: PathInferenceParameters2,
    projector: ProjectionHookInterface,
    path_discovery: PathGenerator2): PathInferenceManager = {
    create(params, projector = projector, path_discovery = path_discovery)
  }

  def createManager(params: PathInferenceParameters2, links: Array[Link]): PathInferenceManager = {
    create(params, links = links)
  }

  def create(params: PathInferenceParameters2,
    links: Array[Link] = null,
    filter_non_hired: Boolean = false,
    path_discovery: PathGenerator2 = null,
    transition_model: TransitionModel = null,
    observation_model: ObservationModel = null,
    projector: ProjectionHookInterface = null): PathInferenceManager = {

    val trans_model = if (transition_model == null) {
      TransitionModel.defaultModel
    } else {
      transition_model
    }

    val obs_model: ObservationModel = if (observation_model == null) {
      ObservationModel.defaultModel
    } else {
      observation_model
    }

    val path_disco = if (path_discovery == null) {
      PathGenerator2.getDefaultPathGenerator(params)
    } else {
      path_discovery
    }

    val net_links = links.toSeq

    val projection_hook: ProjectionHookInterface = if (projector == null) {
      if (net_links != null) {
        ProjectionHook.create(net_links, params)
      } else {
        new EmptyHook
      }
    } else { projector }

    if (filter_non_hired != false) {
      new DefaultManager(params, obs_model, trans_model, path_disco, projection_hook) with HiredFilter
    } else {
      new DefaultManager(params, obs_model, trans_model, path_disco, projection_hook)
    }
  }
}
