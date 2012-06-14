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

/**
 * The Path inference filter maps GPS data (from probes or vehicles) onto
 * the road network, and reconstructs paths and trajectories taken by vehicles
 * in a smart way.
 *
 * Here are the classes you should be looking at:
 *  - a the API of the path inference filter is defined in
 *    [[path_inference.manager.PathInferenceManager]]
 *  - static methods to create a filter and usage examples are defined in
 *    [[path_inference.PathInferenceFilter]]
 *  - all the configuration of a filter is done by an instance of
 *    [[path_inference.PathInferenceParameters2]]
 *
 * If you want more examples to see how to use it, you should look at the
 * unit tests (in the package ''path_inference_test'').
 */
package object path_inference {
}