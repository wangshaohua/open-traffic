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

import collection.mutable.Queue
import core.Time
import core_extensions.MMLogging
import netconfig.Datum.ProbeCoordinate
import netconfig.Link
import path_inference.Delta
import path_inference.models.ObservationModel
import path_inference.models.TransitionModel
import scalala.tensor.dense.DenseVectorCol
import scalala.tensor.dense.DenseVector
import scalala.operators.Implicits._

/**
 * A frame contains all the informations required to build the output,
 * and all the information for performing the computations of an HMM
 */
private[path_inference] class ViterbiFrame(
  payload: Any,
  time: Time,
  logObservations: DenseVector[Double],
  forward: DenseVector[Double],
  backward: DenseVector[Double],
  posterior: DenseVector[Double],
  sp_transition_next: Array[(Int, Int)],
  val most_likely_previous: Array[Int])
  extends CRFFrame(
    payload,
    time,
    logObservations,
    forward,
    backward,
    posterior,
    sp_transition_next) {
}

private[path_inference] class ViterbiDecoder(
  obs_model: ObservationModel,
  trans_model: TransitionModel)
  extends AbstractCRF(obs_model, trans_model)
  with MMLogging {

  /**
   * The main queue. New elements are pushed in and old elements are removed out.
   *
   * FIFO => head = oldest, last = most recent
   */
  val vqueue: Queue[ViterbiFrame] = new Queue[ViterbiFrame]() //private

  // Adding a point
  override def setFirstPoint(point: ProbeCoordinate[Link]) {
    val nspots = point.spots.size
    val payload = point
    val obs_log = obs_model.logObs(point)

    // Connect to the previous frame by the transition matrix
    if (!vqueue.isEmpty) {
      val delta: Delta = vqueue.last.payload.asInstanceOf[Delta]
      val transitions = CRFUtils.findTransitionsDeltaToPC2(delta, point)
      val old_frame = vqueue.last
      old_frame.sp_transition = transitions
    }

    // Wrap the array in a vector
    val wrapper_vec = new DenseVectorCol(point.probabilities.rawData)
    val wrapper_obs_log = new DenseVectorCol(obs_log)
    // Do not touch the values of the probabilities here
    // bacause we also reinject the same point later on during finalizeation
    // This would cause the prob vecbe reset
    // This is not necessary, but allows tomonitor changes when inserting frames
    //    wrapper_vec := wrapper_obs_log

    val frame = new ViterbiFrame(point,
      point.time,
      wrapper_obs_log,
      DenseVectorCol.zeros[Double](nspots),
      DenseVectorCol.zeros[Double](nspots),
      wrapper_vec,
      null,
      Array.fill(nspots)(0)) // Putting a default value for uniform probas

    //    logDebug("Adding point frame "+frame+" : "+point.probabilities)
    this addFrame frame
  }

  override def addPair(delta: Delta, point: ProbeCoordinate[Link]): Unit = {

    //empty paths => finalize computations and clear everything
    // It may happen also that the filter just got reset because of a model exception.
    if (vqueue.isEmpty) {
      //FIXME: this case should not happen
      logDebug("Empty queue, clearing computations")
      finalizeComputations
      this setFirstPoint point
      return
    }

    if (delta.paths.isEmpty) {
      logDebug("End-of-track received, clearing computations")
      finalizeComputations
      this setFirstPoint point
      return
    }

    //Otherwise, compute the new transition matrix and push the frame (this will
    // trigger computations)

    val npaths = delta.paths.length
    val previous_point = vqueue.last.payload.asInstanceOf[ProbeCoordinate[Link]]
    assert(previous_point.time <= point.time)
    assert(previous_point.time <= delta.points.head.time)
    assert(previous_point.time <= delta.points.last.time)
    assert(delta.points.head.time <= point.time)
    assert(delta.points.last.time <= point.time)

    val transitions = CRFUtils.findTransitionsPCToDelta(previous_point, delta)
    vqueue.last.sp_transition = transitions

    // Compute the vector of weights / probabilities of the paths
    val path_weights = delta.paths.map(trans_model.logTrans(_)).toArray
    val wrapper_trans_log = new DenseVectorCol(path_weights)

    // Wrap the array in a vector
    val wrapper_vec = new DenseVectorCol(delta.probabilities)
    // This is not necessary, but allows tomonitor changes when inserting frames
    // THis works here. Do not do it for points
    wrapper_vec := 1.0

    //Insert the new frame
    val frame = new ViterbiFrame(delta,
      delta.points.head.time,
      wrapper_trans_log,
      DenseVectorCol.zeros[Double](npaths),
      DenseVectorCol.zeros[Double](npaths),
      wrapper_vec,
      null, Array.fill(npaths)(0)) //Putting some default value, in case all probas are uniform

    //    logDebug("Adding frame to VHMM: "+frame)
    this addFrame frame
    // Insert the next point
    this setFirstPoint point
  }

  def addFrame(frame: ViterbiFrame): Unit = {
    vqueue += frame
    try {
      perform_computations
    } catch {
      case e: InternalMathException =>
        logError("Tracker reset due to internal exception")
        if (debugMode)
          throw e
        //We clear the current filter
        // Note that we still keep the last frame in the filter (if a point) to respect
        // the semantics. The next frame will be an empty path frame.
        val last_payload = vqueue.last.payload
        vqueue.clear
        // If the filter died when the last frame inserted was a point, reinsert
        // it. Otherwise, it was a path and a point will follow.
        last_payload match {
          // Is there a better way to do it due to type erasure?
          case pc: ProbeCoordinate[_] => this setFirstPoint pc.asInstanceOf[ProbeCoordinate[Link]]
          case _ => // Do nothing
        }
        num_forward = 0
      case e =>
        // Unknown exception, rethrowing
        throw e
    }
  }

  override def length = vqueue.length

  def perform_computations: Unit = {}

  /**
   * Gets called when an end-of track frame is received.
   *
   * After that, the filter is cleared.
   */
  def finalize_computations: Unit = {
    // Launch the computations
    forback
    push_all_out
  }

  override def clearQueue = { vqueue.clear }

  /**
   * Performs forward propagation.
   */
  override def forward: Unit = {
    if (vqueue.isEmpty)
      return
    // FIll the first element
    if (num_forward == 0) {
      val head = vqueue.head
      val n = head.forward.size
      head.forward := head.logObservations //Uniform start probability * obs
      num_forward = 1
      /**
       * FIXME: the values could be zero here already.
       * => add an internal exception
       * What to do with num_forward??
       */
    }

    for ((prev, next) <- (vqueue.slice(num_forward - 1, vqueue.length - 1) zip vqueue.slice(num_forward, vqueue.length))) {
      next.forward := this.init_empty_value

      {
        var i = prev.sp_transition.size - 1
        while (i >= 0) {
          val previdx = prev.sp_transition_from(i)
          val nextidx = prev.sp_transition_to(i)
          if (next.forward(nextidx) < prev.forward(previdx)) {
            next.most_likely_previous(nextidx) = previdx
            next.forward(nextidx) = prev.forward(previdx)
          }
          i -= 1
        }
      }

      //        for ((previdx, nextidx) <- prev.sp_transition) {
      //          next.forward(nextidx)
      //          prev.forward(previdx)
      //          if (next.forward(nextidx) < prev.forward(previdx)) {
      //            next.most_likely_previous(nextidx) = previdx
      //            next.forward(nextidx) = prev.forward(previdx)
      //          }
      //        }

      next.forward :+= next.logObservations

      // Sanity check if any value is at least real
      if (next.forward.data.max == Double.NegativeInfinity) {
        logDebug("zero prob in forward")
        throw new InternalMathException
      }
      if (next.forward.data.exists(_.isNaN)) {
        logDebug("NaN detected in forward") // This case should never happen
        throw new InternalMathException
      }

      // Normalize to prevent log numbers from going too far
      LogMath.logNormalizeInPlace(next.forward.data)

    }
    num_forward = vqueue.length
  }

  override def backward: Unit = {
    assert(false)
  }

  def argmax(xs: Array[Double]): Int = {
    if (xs.isEmpty)
      assert(false, "empty array")
    var i = 0
    for (j <- 0 until xs.length) {
      if (xs(j) > xs(i))
        i = j
    }
    i
  }

  override def forback: Unit = {
    if (vqueue.isEmpty) return

    forward
    // Mark the head of the most likely sequence
    var best_idx = argmax(vqueue.last.forward.data)

    for (idx <- (vqueue.length - 1) to 0 by -1) {
      val frame = vqueue(idx)
      (0 until frame.forward.size).map(frame.posterior(_) = 0)
      //      logDebug("Frame : idx = %d and best_idx = %d" format(idx,best_idx))
      frame.posterior(best_idx) = 1
      if (idx > 0)
        best_idx = frame.most_likely_previous(best_idx)
    }
  }

  override def push_all_out: Unit = {
    for (frame <- vqueue) {
      if (frame.posterior.data.exists(_.isNaN)) {
        logWarning(" Found a NaN in push_ll_out, this should not happen")
      } else {
        out_queue += frame
      }
    }
  }

  /**
   * Returns the payload of the last frame inserted.
   */
  override def lastPayload: Any = {
    assert(!vqueue.isEmpty)
    vqueue.last.payload
  }

  override def outputQueue = {
    val out = out_queue.dequeueAll(_ => true)
    out
  }
}
