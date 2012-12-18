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

package edu.berkeley.path.bots.netconfig

import collection.JavaConversions
import com.google.common.collect.ImmutableList

/**
 * Provides some implicit conversions between scala Seq's and java (guava) ImmutableList's.
 *
 * TODO(tjh) This is so central is would deserve to move to CORE?
 */
object CollectionUtils {
  import JavaConversions._
  implicit def asImmutableList1[T](xs: Seq[T]): ImmutableList[T] = {
    ImmutableList.copyOf(JavaConversions.asJavaIterable(xs))
  }
  implicit def asImmutableList2[T: Manifest](xs: Array[T]): ImmutableList[T] = {
    ImmutableList.copyOf(JavaConversions.asJavaIterable(xs.toSeq))
  }
}