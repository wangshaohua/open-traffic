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

package path_inference.manager

import collection.mutable.HashMap
import collection.mutable.Queue
import core_extensions.MMLogging
import netconfig.Datum.ProbeCoordinate
import netconfig.Link
import path_inference.models.ObservationModel
import path_inference.models.TransitionModel
import path_inference.shortest_path.PathGenerator2
import path_inference.PathInferenceParameters2
import path_inference.VehicleFilter3
import path_inference.VehicleFilter
import java.util.concurrent.atomic.AtomicInteger

class DefaultManager(
  val parameters: PathInferenceParameters2,
  val obs_model: ObservationModel,
  val trans_model: TransitionModel,
  val common_path_discovery: PathGenerator2,
  val projection_hook: ProjectionHookInterface)
  extends PathInferenceManager with MMLogging {

  // Check the parameters here, will throw an exception if invalid.
  parameters.assertValidParameters

  /**
   * Filter for the individual vehicles.
   */
  private[this] var v_filters = Map.empty[String, VehicleFilter3]

  private[this] val internal_storage = new InternalStorage(parameters)

  private[this] var point_counter = 0

  private val printMessageCounter = 100000

  override def addPoint(point: ProbeCoordinate[Link]): Unit = synchronized {
    // This method is snchronized because it updates the state of the tracker.
    val t = point.time()
    // Remove all the filters with timeouts
    // TODO(?) this implementation is very naive (O(N))
    // Should be done using an auxiliary binary search tree on time stamps (O(logN)), much better when there is a log of vehicles.
    val (new_v_filters, old_v_filters) = v_filters.partition({
      case (key, filter) =>
        val last_seen = filter.last_seen_time
        val dt = (t - last_seen)
        dt < parameters.filterTimeoutWindow
    })
    v_filters = new_v_filters.toArray.toMap

    // Terminate the old filters
    for (filter <- old_v_filters.values) {
      logInfo("Evicting tracker for id %s due to timeout." format filter.id)
      filter.finalizeTracker
    }

    val id = point.id
    v_filters.get(id) match {
      case None =>
        logInfo("creating new tracker for id " + id)
        val filter = VehicleFilter.createVehicleFilter(parameters, point, obs_model, trans_model, internal_storage, projection_hook, common_path_discovery)
        v_filters += id -> filter
      // No need to add the point, it is already included in the constructor.
      case Some(filter) =>
        filter addProbeCoordinate point
      // No need to check the output of the filter
      // It will be automatically sent to the internal storage object.
      // Check how recent the point is and discard too old trackers
    }

    point_counter += 1
    if (point_counter > printMessageCounter) {
      val num_filters = v_filters.size
      val num_frames = v_filters.values.map(_.crf.numStoredFrames).sum
      logInfo("%d points processed, %d active tracks, %d active frames" format (point_counter, v_filters, num_frames))
      point_counter = 0
    }
  }

  override def getProbeCoordinates = internal_storage.getProbeCoordinates

  override def getPathInferences = internal_storage.getPathInferences

  override def getRouteTTs = internal_storage.getRouteTTs

  //  override def getTrajectories = internal_storage.getTrajectories

  override def getTSpots = internal_storage.getTSpots

  def finalizeManager: Unit = {
    // Tell all the filters to finalize their computations
    for (filter <- v_filters.values)
      filter.finalizeTracker
    // Discard all the filters, since we are done with them.
    v_filters = Map.empty
    // Make sure the cache is flushed to the disk, if necessary.
    common_path_discovery.finalizeOperations
  }
  internal_storage.finalizeComputations
}
