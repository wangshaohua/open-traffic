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
import scala.reflect.BeanProperty
import java.io.Serializable
import edu.berkeley.path.bots.netconfig.NetconfigException
import path_inference.models._
import path_inference.crf.ComputingStrategy

/**
 * New version of the parameters class to go around some issues with
 * accessing the fields in non-scala interfaces.
 */
class PathInferenceParameters2 extends Serializable {
  /**
   * FIXME: explain projection grid operations.
   */
  @BeanProperty var projectionGridStep: Double = -1.0

  @BeanProperty var projectionRadius: Double = 100.0

  @BeanProperty var maxProjectionReturns: Int = 10

  /**
   * For each vehicle, maximum time disparity between timestamp of last record
   *  and timestamp from last record seen
   * for that particular vehicle. (Seconds.)
   *
   * <p>
   * If the disparity between the last seen record and the new record
   * exceeds this value, the filter corresponding to this vehicle is
   * asked to finalize its current computations, reset and the new record is added.
   * <p>
   * vehicleTimeout <= filterTimeout
   * <p>
   * Default value: 3 minutes
   */
  @BeanProperty var vehicleTimeout: Float = 60 * 3f;

  /**
   * Maximum time disparity between timestamp of last record
   * from any vehicle and timestamp from last record seen
   * for a particular vehicle before the prior record for that
   * vehicle is discarded. (Seconds.)
   * <p>
   * For each vehicle tracked, if the disparity between the last
   * seen record for that vehicle and the new record exceed this
   * value, the filter for that vehicle is reset and discarded. (i.e.
   * this vehicle will not be tracked until some new points coming
   * from this vehicle appear again)
   * <p>
   * For offline use (smoothing/historical settings), one may set this value to infinity.
   * For real time (tracking), set some value to a reasonable value (30 inutes to an hour is a good choice).
   * <p>
   * VehicleTimeout and filterTimeoutWindow differ in that the filter
   * discards some data with filterTimeWindow is used and finalizes the computations
   * with vehicleTimeout.
   * <p>
   * Default value: 10 minutes
   */
  @BeanProperty var filterTimeoutWindow = 60 * 10f;

  /**
   * Every path considered by the filter will last at least this time.
   *
   * FIXME: to implement.
   */
  @BeanProperty var minTravelTime = 0.0f;

  /**
   * Maximum size of the internal buffer (output,hmm).
   *
   * This is a safeguard to ensure the user periodically clears the
   * output buffer or that the filter is not just mindlessly keeping some
   * data around. Exceeding this threshold will trigger a PathInferenceOutOfMemory exception.
   * <p>
   * For offline use (smoothing), you can set to infinity if you think
   * it makes sense.
   * <p>
   * Default value: infinity
   */
  @BeanProperty var maxBufferSize = Integer.MAX_VALUE;
  /**
   * Maximum number of iterations in the A* search. (Each iteration
   * considers all paths including a link and its outlinks)
   * <p>
   * This is the number of path expansions performed for each pair of links.
   * Good values are between 20 and 200.
   * <p>
   * Default value: 50
   */
  @BeanProperty var maxSearchDepth = 50;
  /**
   * Maximum number of paths to search for (between each pair of points
   * on a road) and to return.
   * <p>
   * Default value: 10
   */
  @BeanProperty var maxPaths = 10;
  /**
   * Threshold ratio for accepting long paths.
   * <p>
   * Intuitively, the paths connecting two different points should not be too
   * long. This ratio limits the maximum length of accepted paths, as a proportion
   * of the shortest path found.
   * <p>
   * It has to be >=1
   * <p>
   * Default value: 2
   */
  @BeanProperty var pathLengthThresholdRatio = 2.0
  /**
   * The maximum number of vehicles to track.
   *
   * If the number of vehicles currently observed exceeds this value,
   * the filter will discard the individual filters of the vehicles
   * with the latest timestamps.
   * <p>
   * Default value: infinity
   */
  @BeanProperty var maxVehicles = Integer.MAX_VALUE;
  /**
   * Below this value, all paths will be kept.
   *
   * This threshold the filtering done by {@link #pathLengthThresholdRatio}
   */
  @BeanProperty var pathOffsetMinLength = 300.0

  /**
   * Minimum length for any path before it is considered by the
   * path inference algorithm (meters).
   *
   * For complicated driver models, it is be useful to only start
   * computing the paths between two points after the vehicle has
   * has started moving. This s especially useful in arterial models
   * in which a vehicle switches between a 0 speed model (waiting at
   * a red light) and a multi-link higher-speed model (when travelling).
   * Instead of having a more complicated model of the vehicle, setting a positive values ensures that only forward moving points will be passed
   * to the model.
   * <p>
   * Default value: 0 (feature disabled)
   */
  @BeanProperty var minTravelOffset = 0.0
  /**
   * Set it to true to have some routes returned.
   */
  @BeanProperty var returnRoutes = false
  /**
   * Set it to true to have some paths returned.
   */
  @BeanProperty var returnPoints = false;
  /**
   * Use this option if you want to get access to the raw data after
   * processing in the filter.
   *
   * Useful for research only.
   */
  @BeanProperty var returnFrames = false;

  @BeanProperty var returnTrajectories = false;

  @BeanProperty var returnTimedSpots = false;

  @BeanProperty var returnRouteTravelTimes = false;

  /**
   * The paths with a probability lower than this value will be
   * discarded from the output.
   * Default value: 1e-8
   */
  @BeanProperty var minPathProbability = 1e-8;

  /**
   * The point projections with a probability lower than this value will be
   * discarded from the output.
   * Set this value to 0 if you want to keep all the projections.
   * Default value: 0 (feature disabled)
   */
  @BeanProperty var minProjectionProbability = 0.0;

  /**
   * The size of the cache for the paths.
   * TODO(tjh) document
   * A reasonable value for a very large network should be 1-10M
   */
  @BeanProperty var pathsCacheSize: Int = 100000

  /**
   * If set to true, the projections in the output probe coordinates will be
   * sorted by decreasing order of probability.
   *
   * Default value: false (the order of the projection is left unchanged).
   */
  @BeanProperty var shuffleProbeCoordinateSpots = false;

  @BeanProperty var computingStrategy = ComputingStrategy.LookAhead2;

  def assertValidParameters {
    if (maxVehicles < 1) {
      throw new NetconfigException(null, "The filter needs to accept at least on vehicle");
    }

    if (pathLengthThresholdRatio < 1) {
      throw new NetconfigException(null, "The length threshold ratio on the paths has to be greater than 1.");
    }
  }

  /**
   * Warning: does not fill returnXXXX
   */
  def fillDefaultForHighFreqOffline {
    computingStrategy = ComputingStrategy.Viterbi;
    maxSearchDepth = 10;
    pathLengthThresholdRatio = 2;
    vehicleTimeout = 120;
    minTravelOffset = -20;
    pathOffsetMinLength = 400;
    maxPaths = 50;
    projectionGridStep = 2.0
    maxProjectionReturns = 1000
  }

  def fillDefaultForHighFreqOnline {
    computingStrategy = ComputingStrategy.LookAhead10;
    maxSearchDepth = 3;
    pathLengthThresholdRatio = 2;
    vehicleTimeout = 120;
    minTravelOffset = -20;
    pathOffsetMinLength = 400;
    maxPaths = 1000;
    projectionGridStep = 2.0
    maxProjectionReturns = 200
    shuffleProbeCoordinateSpots = true
    minPathProbability = 1e-3
    minProjectionProbability = 1e-3
  }

  def fillDefaultForHighFreqOnlineFast {
    computingStrategy = ComputingStrategy.LookAhead10;
    maxSearchDepth = 3;
    pathLengthThresholdRatio = 2;
    vehicleTimeout = 120;
    minTravelOffset = -20;
    pathOffsetMinLength = 400;
    maxPaths = 1000;
    maxProjectionReturns = 100
    shuffleProbeCoordinateSpots = true
    minPathProbability = 1e-3
    minProjectionProbability = 1e-3
  }
  /**
   * Use this if:
   * - you run the filter offline
   * - you get some data points approximately every minute
   * - you care about paths and not points
   * Warning: fills the returnXXXX
   * Warning: these parameters are for high quality reconstruction:
   * they will use a lot of memory.
   */
  def fillDefaultFor1MinuteOffline {
    computingStrategy = ComputingStrategy.Offline;
    maxSearchDepth = 60;
    pathLengthThresholdRatio = 3;
    pathOffsetMinLength = 300;
    vehicleTimeout = 130;
    minTravelOffset = -20;
    maxPaths = 100;
    //    returnFrames = false;
    //    returnPoints = true;
    //    returnRoutes = false;
  }

  def fillDefaultFor1MinuteOnline {
    computingStrategy = ComputingStrategy.LookAhead1;
    maxSearchDepth = 60;
    pathLengthThresholdRatio = 3;
    pathOffsetMinLength = 300;
    vehicleTimeout = 130;
    minTravelOffset = -20;
    maxPaths = 40;
    //    returnFrames = false;
    //    returnPoints = true;
    //    returnRoutes = false;
  }

  def getGoodTransitionModelForCabspotting = {
    val weights = Array(-1.0 / 30.0, 0.0)
    new SimpleFeatureModel(new FeatureTransitionModelParameters(weights));
  }

  def getGoodObservationModelForCabspotting = {
    val std_dev = 10.0;
    new IsoGaussianObservationModel(new IsoGaussianObservationParameters(std_dev * std_dev));
  }

  def getGoodTransitionModelForGPSLogger = {
    val weights = Array(-1.0 / 10)
    new BasicFeatureModel(new FeatureTransitionModelParameters(weights));
  }

  def getGoodObservationModelForGPSLogger = {
    val std_dev = 10.0;
    new IsoGaussianObservationModel(new IsoGaussianObservationParameters(std_dev * std_dev));
  }
}
