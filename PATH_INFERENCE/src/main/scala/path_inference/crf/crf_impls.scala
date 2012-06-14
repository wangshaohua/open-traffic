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

package path_inference.crf

import path_inference.models.ObservationModel
import path_inference.models.TransitionModel

class OfflineHMM(
  obs_model: ObservationModel,
  trans_model: TransitionModel)
  extends AbstractCRF(obs_model, trans_model) {
  def perform_computations = {
    // Do nothing, just accumulate
  }

  def finalize_computations = {
    // Launch the computations
    forback
    push_all_out
  }

}

class OnlineHMM(obs_model: ObservationModel,
  trans_model: TransitionModel)
  extends AbstractCRF(obs_model, trans_model) {
  def perform_computations {
    forward
    if (length > 1)
      push_out(0) //oldest point we received = first point in the queue
    keepAtMost(1)
    assert(length == 1)
  }

  def finalize_computations {
    // To ensure all the points go out, call a forward backward
    // This just pushes out whatever we have left in the filter
    forward
    push_all_out
  }

}

class LookAheadHMM(obs_model: ObservationModel,
  trans_model: TransitionModel, window_size: Int)
  extends AbstractCRF(obs_model, trans_model) {
  def perform_computations {
    assert(length <= window_size + 1) // we should only receive one record at a time
    if (length > window_size) {
      forback
      push_out(0) //oldest point we received = first point in the queue
      keepAtMost(window_size)
      assert(length == window_size)
    }
  }

  def finalize_computations {
    // Take whatever window we have and use it for full forward backward
    forback
    push_all_out
  }
}

class WindowHMM(obs_model: ObservationModel,
  trans_model: TransitionModel, window_size: Int)
  extends AbstractCRF(obs_model, trans_model) {
  def perform_computations {
    assert(length <= window_size + 1) // we should only receive one record at a time
    if (length > window_size) {
      forback
      push_out_last(window_size / 2) //oldest point we received = first point in the queue
      keepAtMost(length - window_size / 2) // Suppress half of the window.
    }
  }

  def finalize_computations {
    // Take whatever window we have and use it for full forward backward
    forback
    push_all_out
  }
}
