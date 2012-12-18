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

import edu.berkeley.path.bots.netconfig.Datum.PathInference
import edu.berkeley.path.bots.netconfig.Datum.ProbeCoordinate
import edu.berkeley.path.bots.netconfig.Datum.RouteTT
//import edu.berkeley.path.bots.netconfig.Datum.Trajectory
import edu.berkeley.path.bots.netconfig.Datum.TSpot
import edu.berkeley.path.bots.netconfig.Link
import edu.berkeley.path.bots.netconfig.NetconfigException
import java.util.{ Collection => JCollection }
import scala.collection.JavaConversions._

class FeatureNotImplementedException extends NetconfigException(null, "")

/**
 * The interface that defines all the public functions provided by
 * the path inference filter.
 *
 * All the rest is implementaiton detail :)
 */
abstract class PathInferenceManager {

  /**
   * Adds points to the filter and launches some computations.
   *
   * Some (lengthy) computations may occur at this point depending on
   * the model.
   *
   * This is the only function that needs to be reimplemented
   * by client classes.
   *
   * @throws PathInferenceOutOfMemory
   */
  /**
   * Adds points to the filter and launches some computations.
   *
   * @param points some input points that may come from multiple vehicles. The points may be unordered in time in this list,
   * however they must be ordered in time as far as each vehicle is considered.
   *
   * This function is blocking. Some (lengthy) computations may occur at this point depending on
   * the model. When the function returns however, some points may be waiting
   * at different stages of the processing, waiting for more points to come.
   *
   * In order to get all the points back, look at finalizeComputations.
   *
   * @throws PathInferenceOutOfMemory if too much memory is being used. If this
   * is the case, look at EHCacheAstar.
   */
  def addPoint(point: ProbeCoordinate[Link]): Unit = {
    // Technical detail. An implementation has to br provided for
    // proper stacking of the traits. It is not a vlid implementation
    // though.
    throw new NetconfigException(null, "You need to implement this function.")
  }
  /**
   * Adds points to the filter and launches some computations.
   *
   * @param points some input points that may come from multiple vehicles. The points may be unordered in time in this list,
   * however they must be ordered in time as far as each vehicle is considered.
   *
   * This function is blocking. Some (lengthy) computations may occur at this point depending on
   * the model. When the function returns however, some points may be waiting
   * at different stages of the processing, waiting for more points to come.
   *
   * In order to get all the points back, look at finalizeComputations.
   *
   * @throws PathInferenceOutOfMemory if too much memory is being used. If this
   * is the case, look at EHCacheAstar.
   */
  def addPoints(points: Seq[ProbeCoordinate[Link]]): Unit =
    {
      assert(points != null)
      points.foreach(addPoint _)
    }

  /**
   * Adds points to the filter and launches some computations.
   *
   * @param points some input points that may come from multiple vehicles. The points may be unordered in time in this list,
   * however they must be ordered in time as far as each vehicle is considered.
   *
   * This function is blocking. Some (lengthy) computations may occur at this point depending on
   * the model. When the function returns however, some points may be waiting
   * at different stages of the processing, waiting for more points to come.
   *
   * In order to get all the points back, look at finalizeComputations.
   *
   * @throws PathInferenceOutOfMemory if too much memory is being used. If this
   * is the case, look at EHCacheAstar.
   */
  def addPoints(points: JCollection[ProbeCoordinate[Link]]): Unit = {
    assert(points != null)
    points.foreach(addPoint _)
  }

  /**
   * Adds points to the filter and launches some computations.
   *
   * @param points some input points that may come from multiple vehicles. The points may be unordered in time in this list,
   * however they must be ordered in time as far as each vehicle is considered.
   *
   * This function is blocking. Some (lengthy) computations may occur at this point depending on
   * the model. When the function returns however, some points may be waiting
   * at different stages of the processing, waiting for more points to come.
   *
   * In order to get all the points back, look at finalizeComputations.
   *
   * @throws PathInferenceOutOfMemory if too much memory is being used. If this
   * is the case, look at EHCacheAstar.
   */
  def addPoints(points: Array[ProbeCoordinate[Link]]): Unit =
    this.addPoints(points)

  /**
   * Attempts to use any data that is currently being stored in the filter to
   * generate some output.
   *
   * In the case of long-running scenarios (smoothing), some data may be
   * buffered for a while before being actually filtered. This forces the filtered
   * to use the data already available.
   *
   * This function works on a best effort basis, and may decide to
   * discard the data not processed so far.
   *
   * At the return of the function, the state of the filter is the same as when
   * the filter was created, with the addition that some data may be available
   * at the output.
   */
  def finalizeManager: Unit

  def getProbeCoordinates: Array[ProbeCoordinate[Link]] = {
    throw new FeatureNotImplementedException
  }

  def getPathInferences: Array[PathInference[Link]] = {
    throw new FeatureNotImplementedException
  }

  def getRouteTTs: Array[RouteTT[Link]] = {
    throw new FeatureNotImplementedException
  }

  def getTSpots: Array[TSpot[Link]] = {
    throw new FeatureNotImplementedException
  }

  //  def getTrajectories: Array[Trajectory[Link]] = {
  //    throw new FeatureNotImplementedException
  //  }

}

