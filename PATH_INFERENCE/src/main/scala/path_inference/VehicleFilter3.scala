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

import collection.mutable.{ ArrayBuffer, HashMap => MMap, Queue => MQueue }
import collection.immutable.Queue
import core.Time
import core_extensions.MMLogging
import netconfig.{ Link, NetconfigException }
import netconfig.Datum.ProbeCoordinate
import java.util.Comparator
import path_inference.shortest_path.PathGenerator2
import path_inference.crf.ConditionalRandomField
import path_inference.models.ObservationModel
import path_inference.models.TransitionModel
import path_inference.manager.ProjectionHookInterface
import com.google.common.collect.Ordering
import scala.collection.JavaConversions._

class VehicleFilter3(
  val hmm: ConditionalRandomField,
  path_gen: PathGenerator2,
  val parameters: PathInferenceParameters2,
  first_point: ProbeCoordinate[Link],
  output: FilterOutputInterface,
  projection_hook: ProjectionHookInterface)
  extends MMLogging {

  /**
   * Last time stamp seen by the filter.
   */
  var last_seen_time = first_point.time

  /**
   * Minimum probability associated to a path.
   * TODO: DOC
   */
  val min_path_probability = parameters.minPathProbability // 0.15  //10% of the uniform probability, that should be small enough
  /**
   * Storage of points currently in the filter.
   */
  var lastPoints = Queue.empty[ProbeCoordinate[Link]]
  /**
   * ID of the vehicle.
   */
  val id = first_point.id

  // The previously computed paths, organized by (start_link,end_link) and then by (start_offset,end_offset)
  val previous_paths_regrouped: ShortestPaths.RegroupedPathsMap = MMap.empty[(Link, Link), Map[(Double, Double), Array[Path]]]

  private var reachable_links = Set.empty[Link]

  private val max_queue_size = 4

  // Constructor logic
  // Make sure it is put *after* the declaration of all the variables.
  {
    assert(hmm != null)
    assert(path_gen != null)
    assert(parameters != null)
    assert(first_point != null)
    /* Constructor logic */
    this addProbeCoordinate first_point
  }

  /**
   * Assumes points are already mapped.
   */
  def addProbeCoordinate(p: ProbeCoordinate[Link]): Unit = {
    val point = if (lastPoints.isEmpty) {
      projection_hook.projectPoint(p)
    } else {
      projection_hook.projectPointWithPreviousCoords(p, lastPoints.last)
    }
    // Keep track of the time last seen
    last_seen_time = point.time

    if (point.spots.isEmpty) {
      logWarning("Point has no projection:")
      logWarning(point.toString)
    } else {
      /**
       * Special case for the constructor
       */
      if (lastPoints.isEmpty) {
        lastPoints = lastPoints.enqueue(point)
        reachable_links = Set.empty[Link] ++ point.spots.map(_.link)
        hmm += point
      } else {
        assert(point.id.equals(this.id)) //Tracker works on one vehicle only
        if (lastPoints.last.time > point.time) {
          logWarning("Throwing out point because fo bad timing : " + point)
          return
        }
        lastPoints = lastPoints.enqueue(point)
        val (lastPoints_, reachable_links_) = performComputaitons(lastPoints, reachable_links, lastPoints.size - 2, max_queue_size)
        lastPoints = lastPoints_
        reachable_links = reachable_links_
        exportOutput
      }
    }
  }

  /**
   * Sends a hint that the the tracker should try to finish current computations
   * and return results.
   *
   * After calling finalizeTracker, this vehicle filter should be *discarded*.
   * The internal state is left undefined.
   */
  def finalizeTracker: Unit = {
    // Flush out everything we can
    performComputaitons(lastPoints, reachable_links, 0, 1)
    // Finish the computations on the CRF side
    hmm.finalizeComputations
    //And we see if anything got out
    exportOutput
  }

  // This is a bit more complicated since the first point may be
  // in limbo inside the pre hook.
  def lastMeasurementTime: Time = last_seen_time

  private def exportOutput: Unit = {
    // See if there is some new output to take care of
    // Send it to the output for processing
    for (frame <- hmm.outputQueue)
      output.addFrame(frame)
  }

  /**
   * This function does not mutate data, expect pushing points to the CRF.
   * Pay attention, this is the most complicated piece of logic for the tracker.
   * This code is covered by a number of unit tests, it is suggested to read them
   * first to understand how much cases are handled by this function.
   */
  private def performComputaitons(queue: Queue[ProbeCoordinate[Link]],
    reachable_links: Set[Link],
    attempted_so_far: Int,
    wanted_queue_size: Int): (Queue[ProbeCoordinate[Link]], Set[Link]) = {
    //    logInfo("Queue analyzis: "+ (queue.length, attempted_so_far, wanted_queue_size))
    if (attempted_so_far > queue.size) {
      throw new IllegalArgumentException("!!")
    }
    // The queue has size one: the last point we added to the CRF.
    // Nothing else to do for now.
    if (queue.size == 1) {
      //      logInfo("Queue size is 1, done")
      return (queue, reachable_links)
    }
    // Queue is not full and we cannot perform computation further: we stop.
    if (attempted_so_far >= queue.size - 1 &&
      queue.size < wanted_queue_size) {
      //      logInfo("Queue not full and nothing left to do")
      return (queue, reachable_links)
    }
    // We have tried to connect the first point in the queue with all the other
    // points and it never wored. Now we have too many points in the queue.
    // We assume there is a break between the first
    // point and some subsequent points.
    // Break the trajectory, and start again from the next point.
    if (attempted_so_far >= queue.size - 1 &&
      queue.size >= wanted_queue_size) {
      // Drop the oldest point
      //      logInfo("Queue full: dropping a point and starting again")
      val res_queue = queue.drop(1)
      val new_reachable_links = res_queue.head.spots.map(_.link).toSet
      assert(!new_reachable_links.isEmpty)
      val new_first_point = res_queue.head
      hmm.finalizeComputationsAndRestart(new_first_point)
      // Try again from there to reconnect the points.
      return performComputaitons(res_queue, new_reachable_links,
          0, wanted_queue_size)
    }
    // Check if we can create a set of path between the first point in the
    // queue and the current point that we are considuering.
    // If we can, we send the output to the filter, move in the queue and
    // attempt to process the rest.
    // If we cannot, we simply return, the queue will eventually spill over.
    // Compute the paths between the first and the last point in the buffer:
    val first_point = queue.head
    //    logInfo("First point:\n"+first_point)
    val current_point = queue(attempted_so_far + 1)
    //    logInfo("Current point:\n"+current_point)
    val best_paths: Array[Path] = ShortestPaths.getPathsBetweenFast(
      first_point, current_point, previous_paths_regrouped, parameters, path_gen)
    // Check if the paths repect the minimum length constraint
    // If at least one path is too short, do not add the delta and continue
    // The point will eventually get cleared out by timeout.
    if (best_paths.isEmpty) {
      //      logInfo("No path found.")
      // No good paths, let us try to go further in the queue.
      return performComputaitons(queue, reachable_links, attempted_so_far + 1, wanted_queue_size)
    }
    if (best_paths.exists(_.length < parameters.minTravelOffset)) {
      //      logInfo("One path has too small length.")
      // No good paths, let us try to go further in the queue.
      return performComputaitons(queue, reachable_links, attempted_so_far + 1, wanted_queue_size)
    }
    //    logInfo("New paths found:")
    //    logInfo(best_paths.mkString("\n"))
    // Some new paths could be found, this point is reachable
    // Add the delta and the point to the markov model
    assert(best_paths.length <= parameters.maxPaths)
    // Check if we still have a positive flow from the first point to the
    // last point, by computing the set of reachable links using the paths:
    val new_reachable_links = Set.empty[Link] ++ best_paths.filter(p => {
      reachable_links.contains(p.links.head)
    }).map(_.links.last)
    //    logInfo("new reachable set: "+new_reachable_links.mkString(", "))
    // If we do not, the track is broken: we finalize the ongoing computations
    // in the filter and we restart from the current point.
    // The paths we just computed are dropped.
    if (new_reachable_links.isEmpty) {
      logInfo("Logical flow break in track for driver " + id)
      hmm.finalizeComputationsAndRestart(current_point)
      // Evict the poinhts before the current point
      val new_queue = queue.drop(attempted_so_far + 1)
      // Restart the reachable set from the current point.
      val reachable_links = new_queue.head.spots.map(_.link).toSet
      performComputaitons(new_queue, reachable_links, 0, wanted_queue_size)
    } else {
      val delta_pcs = queue.take(attempted_so_far + 2).toArray
      val delta = new Delta(delta_pcs, best_paths)
      //      logInfo(" Adding to CRF: "+(delta, current_point))
      hmm += (delta, current_point)
      // Evict the poinhts before the current point
      val new_queue = queue.drop(attempted_so_far + 1)
      performComputaitons(new_queue, new_reachable_links, 0, wanted_queue_size)
    }
  }
}

object VehicleFilter {

  def createVehicleFilter(params: PathInferenceParameters2,
    first_point: ProbeCoordinate[Link],
    obs_model: ObservationModel,
    trans_model: TransitionModel,
    output: FilterOutputInterface, projection_hook: ProjectionHookInterface): VehicleFilter3 = {
    val crf = createCRF(params, obs_model, trans_model)
    val path_gen = PathGenerator2.getDefaultPathGenerator(params)
    new VehicleFilter3(crf, path_gen, params, first_point, output, projection_hook)
  }

  import path_inference.crf._
  def createCRF(parameters: PathInferenceParameters2,
    obs_model: ObservationModel,
    trans_model: TransitionModel): ConditionalRandomField = {
    parameters.computingStrategy match {
      case ComputingStrategy.Online =>
        new OnlineHMM(obs_model, trans_model)
      case ComputingStrategy.Offline =>
        new OfflineHMM(obs_model, trans_model)
      case ComputingStrategy.LookAhead1 =>
        // Multiplying the window by 2 since there are two frames per added
        // point
        new LookAheadHMM(obs_model, trans_model, 2)
      case ComputingStrategy.LookAhead2 =>
        // Multiplying the window by 2 since there are two frames per added
        // point
        new LookAheadHMM(obs_model, trans_model, 4)
      case ComputingStrategy.LookAhead3 =>
        // Multiplying the window by 2 since there are two frames per added
        // point
        new LookAheadHMM(obs_model, trans_model, 6)
      case ComputingStrategy.LookAhead10 =>
        // Multiplying the window by 2 since there are two frames per added
        // point
        new LookAheadHMM(obs_model, trans_model, 20)
      case ComputingStrategy.Window8 =>
        new WindowHMM(obs_model, trans_model, 16)
      case ComputingStrategy.Viterbi =>
        new ViterbiDecoder(obs_model, trans_model)
      case s =>
        throw new NetconfigException(null, "Missing strategy:" + s)
        null
    }
  }
}

/**
 * This is one ugly mess that better get some explanations...
 */
object ShortestPaths extends MMLogging {

  // TODO: explain this data type...
  type RegroupedPathsMap = MMap[(Link, Link), Map[(Double, Double), Array[Path]]]

  private lazy val ord: Ordering[(Path, Double)] = {
    val comp = new Comparator[(Path, Double)] {
      def compare(o1: (Path, Double), o2: (Path, Double)): Int = o1._2.compare(o2._2)

//      override def equals(o: Any) = {
//        o == this
//      }
    }
    Ordering.from(comp)
  }

  /**
   * The search for simple path links, excepnt 1-link paths. (They are already comprised in the
   * forward path search.)
   *
   * Tries to compute a simple direct path between the two links.
   * Much faster since there only one possible path.
   */
  def getSimpleShortestPath(start_link: Link, end_link: Link): Option[Array[Link]] = {
    val path = new ArrayBuffer[Link]
    if (start_link == end_link)
      return None
    path += start_link
    var steps = 0
    while (path.last != end_link && path.last.outLinks.size == 1 && steps < 200) {
      path += path.last.outLinks.head
      steps += 1
    }
    if (path.last == end_link)
      Some(path.toArray)
    else
      None
  }

  /**Weird function to remove.*/
  private def getShortestBackwardPath(start_link: Link, end_link: Link): Option[Array[Link]] = {
    val path = new ArrayBuffer[Link]
    if (start_link == end_link)
      return None
    path += start_link
    var steps = 0
    while (path.last != end_link && path.last.inLinks.size == 1 && steps < 200) {
      path += path.last.inLinks.head
      steps += 1
    }
    if (path.last == end_link)
      Some(path.reverse.toArray)
    else
      None
  }

  def getPathsBetweenFast(
    first_point: ProbeCoordinate[Link],
    last_point: ProbeCoordinate[Link],
    previous_paths_regrouped: RegroupedPathsMap,
    parameters: PathInferenceParameters2,
    path_gen: PathGenerator2): Array[Path] =
    getPathsBetweenFast(first_point, last_point,
      previous_paths_regrouped, path_gen,
      parameters.maxPaths,
      parameters.pathOffsetMinLength,
      parameters.pathLengthThresholdRatio)

  /**
   * Computes all the paths between two points, based on all the parameters.
   *
   * Optimized function for memory and grided sampling.
   *
   * TODO: this is  one of the monster functions that deserves documentation...
   */
  def getPathsBetweenFast(
    first_point: ProbeCoordinate[Link],
    last_point: ProbeCoordinate[Link],
    previous_paths_regrouped: RegroupedPathsMap,
    path_gen: PathGenerator2,
    maxPaths: Int,
    pathOffsetMinLength: Double,
    pathLengthThresholdRatio: Double): Array[Path] = {
    // First generate all possible paths
    val paths = new MQueue[Path]
    // Regroup the spots by link
    var new_pairs = 0
    var all_pairs = 0
    val start_spots_by_link = first_point.spots.groupBy(_.link)
    val end_spots_by_link = last_point.spots.groupBy(_.link)
    val min_path_length = math.max(pathOffsetMinLength, pathLengthThresholdRatio * first_point.coordinate.distanceDefaultMethodInMeters(last_point.coordinate))
    // For each pair of links, compute the paths.
    val paths_regrouped = for (
      starts <- start_spots_by_link;
      ends <- end_spots_by_link
    ) yield {
      val (start_link, start_spots) = starts
      val (end_link, end_spots) = ends
      // Compute the link paths for this pair of links:
      val link_paths: Seq[Array[Link]] = {
        // Purely forward paths
        assert(end_link != null)
        assert(start_link != null)
        val forward_paths = path_gen.getShortestPaths(start_link, end_link, maxPaths).map(_.toArray)
        // We do not know
        val same_link_paths: Seq[Array[Link]] = if (start_link == end_link) {
          Seq(Array(start_link))
        } else {
          Seq.empty[Array[Link]]
        }
        (forward_paths ++ same_link_paths)
      }
      // If there is no paths, just return nothing
      val paths_by_offset: Seq[((Double, Double), Array[Path])] = if (link_paths.isEmpty) {
        Seq.empty[((Double, Double), Array[Path])]
      } else {
        // Attempt to convert these link paths to paths
        val previous_paths: Map[(Double, Double), Array[Path]] = previous_paths_regrouped.get((start_link, end_link)) match {
          case None => {
            Map.empty[(Double, Double), Array[Path]]
          }
          case Some(lps) => {
            // We alread computed some paths before that
            lps
          }
        }
        val new_paths: Seq[((Double, Double), Array[Path])] = for (
          start_spot <- start_spots;
          end_spot <- end_spots
        ) yield {
          all_pairs += 1
          val start_offset = start_spot.offset
          val end_offset = end_spot.offset
          val key = (start_offset, end_offset)
          val pair: ((Double, Double), Array[Path]) = previous_paths.get(key) match {
            case Some(paths) => {
              // We already computed it before
              (key, paths)
            }
            case None => {
              // We need to compute the paths here, this is a new case
              new_pairs += 1
              val paths: Array[Path] = link_paths.flatMap(lp => {
                if (lp.length == 1) {
                  if (start_offset <= end_offset)
                    Some(new Path(start_offset, end_offset, lp))
                  else
                    None
                } else {
                  Some(new Path(start_offset, end_offset, lp))
                }
              }).toArray
              (key, paths)
            }
          }
          pair
        }
        new_paths
      }
      val res1: Map[(Double, Double), Array[Path]] = Map.empty[(Double, Double), Array[Path]] ++ paths_by_offset
      ((start_link, end_link), res1)
    }
    previous_paths_regrouped.clear
    previous_paths_regrouped ++= paths_regrouped

    val filtered_paths: Seq[Path] = paths_regrouped.values.flatMap(_.values.toSeq.flatten.toSeq).toSeq
//    logInfo("fast paths computation: %d link pairs, %d paths found (%d/%d hits)" format (paths_regrouped.size, filtered_paths.size, new_pairs, all_pairs))
    // Somehow, the toArray is necessary to prevent some superslow conversions to java linkedlist
    val best_paths_with_length = ord.leastOf(filtered_paths.map(p => (p, p.length)).toArray.toSeq, maxPaths)
    val best_paths = best_paths_with_length.map(_._1)
    return best_paths.toArray
  }

  /**
   * Computes all the paths between two points, based on all the parameters.
   */
  private def getPathsBetween(
    first_point: ProbeCoordinate[Link],
    last_point: ProbeCoordinate[Link],
    path_gen: PathGenerator2,
    maxPaths: Int,
    pathOffsetMinLength: Double,
    pathLengthThresholdRatio: Double): Array[Path] = {
    // First generate all possible paths
    val paths = new MQueue[Path]
    // Normal way
    for (
      start_spot <- first_point.spots.toList;
      end_spot <- last_point.spots.toList
    ) {
      val start_offset = start_spot.offset
      val end_offset = end_spot.offset

      // If on the same link, there is only one path to be added.
      // This is wrong: it misses loops (going around the block paths)
      if (start_spot.link == end_spot.link) {
        if (start_spot.offset <= end_spot.offset)
          paths += new Path(start_spot.offset, end_spot.offset, Array(start_spot.link))
      } else {
        val forward_paths = path_gen.getShortestPaths(start_spot.link, end_spot.link, maxPaths)
        // Searching backward
        val backward_paths = getShortestBackwardPath(start_spot.link, end_spot.link)
        // Now we fuse the data together
        val new_paths = forward_paths.map(path => new Path(start_spot.offset, end_spot.offset, path))
        for (p <- new_paths)
          assert(p.compatibleStart(start_spot))
        for (p <- new_paths)
          assert(p.compatibleEnd(end_spot))
        paths ++= new_paths
      }
    }

    // A lot is going on here...
    // Filter the paths that are too backward
    // sort by length and trim the list to the 'shortest' paths (includes backward paths short enough)
    val min_path_length = math.max(pathOffsetMinLength, pathLengthThresholdRatio * first_point.coordinate.distanceDefaultMethodInMeters(last_point.coordinate))
    val filtered_paths = paths.filter(p => p.length.abs <= min_path_length)
    //    val filtered_paths = paths.filter(p=>p.length>=parameters.minBackwardOffset && p.length.abs<=min_path_length).
    //    sortBy(_.length).take(parameters.maxPaths)

    // Somehow, the toArray is necessary to prevent some superslow conversions to java linkedlist
    val best_paths_with_length = ord.leastOf(filtered_paths.map(p => (p, p.length)).toArray.toSeq, maxPaths)
    val best_paths = best_paths_with_length.map(_._1)
    logInfo("All paths found:%d , best filtered paths: %d".format(paths.length, best_paths.length))
    return best_paths.toArray
  }

}
