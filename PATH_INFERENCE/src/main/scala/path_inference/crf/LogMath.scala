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

import math.{ log, exp }
import core_extensions.MMLogging

object LogMath extends MMLogging {
  /**
   * Given a vector in log numbers, normalizes the vector in place in
   * real domain.
   */
  def normalizeInPlace(v: Array[Double]) {
    if (v.length == 0)
      return
    assert(v.length > 0)
    var i = 0
    val n = v.length
    var max_val = v(0)
    // Find the maximum
    i = n - 1
    while (i >= 1) {
      if (v(i) > max_val) {
        max_val = v(i)
      }
      i -= 1
    }
    assert(max_val != Double.NaN)
    // Substract the maximum, exponify and compute the sum value
    i = n - 1
    var sum = 0.0
    while (i >= 0) {
      v(i) = math.exp(v(i) - max_val)
      assert(v(i) != Double.NaN)
      sum += v(i)
      i -= 1
    }
    // Normalize the vector in place
    i = n - 1
    while (i >= 0) {
      v(i) = v(i) / sum
      assert(v(i) != Double.NaN)
      i -= 1
    }
  }

  def normalized(in: Array[Double]): Array[Double] = {
    val out = in.clone
    normalizeInPlace(out)
    out
  }

  def normalize(in: Array[Double], out: Array[Double]) {
    assert(in.length == out.length, "in length is %d, out length is %d" format (in.length, out.length))
    if (in.length == 0)
      return
    assert(in.length > 0)
    var i = 0
    val n = in.length
    var max_val = in(0)
    assert(!max_val.isNaN)
    // Find the maximum
    i = n - 1
    while (i >= 1) {
      assert(!in(i).isNaN)
      if (in(i) > max_val) {
        max_val = in(i)
      }
      i -= 1
    }
    assert(!max_val.isNaN)
    // Substract the maximum, exponify and compute the sum value
    i = n - 1
    var sum = 0.0
    while (i >= 0) {
      out(i) = math.exp(in(i) - max_val)
      assert(!out(i).isNaN)
      sum += out(i)
      i -= 1
    }
    assert(!sum.isNaN)
    // Normalize the vector in place
    i = n - 1
    while (i >= 0) {
      out(i) = out(i) / sum
      assert(!out(i).isNaN)
      i -= 1
    }
  }

  def distanceInf(v1: Array[Double], v2: Array[Double]): Double = {
    assert(v1.length == v2.length)
    assert(v1.length > 0)
    var d = math.abs(v1(0) - v2(0))
    assert(d != Double.NaN)
    var i = v1.length - 1
    var d2 = 0.0
    while (i >= 1) {
      d2 = math.abs(v1(i) - v2(i))
      if (d2 > d) {
        d = d2
      }
      i -= 1
    }
    d
  }

  /**
   * Given a vector in log domain, returns the sum of the elements of the
   * vector, also in log domain.
   */
  def logSumExp(v: Array[Double]): Double = {
    assert(v.length > 0)
    val m = v.max
    assert(m != Double.NaN)

    if (m == Double.NegativeInfinity)
      return Double.NegativeInfinity

    if (m == Double.PositiveInfinity)
      return Double.PositiveInfinity

    var i = v.length - 1
    var res = 0.0
    while (i >= 0) {
      res += math.exp(v(i) - m)
      assert(res != Double.NaN)
      i -= 1
    }
    assert(res != Double.NaN)
    val out = m + math.log(res)
    assert(out != Double.NaN, (m, res))
    return out
  }

  val log2 = log(2.0)

  def logSumExp(x1: Double, x2: Double): Double = {
    assert(x1 != Double.NaN)
    assert(x2 != Double.NaN)
    if (x1 == x2) {
      return x1 + log2
    }
    if (x1 > x2) {
      if (x1 > x2 + 20)
        x1
      else {
        val res = x1 + log(1.0 + exp(x2 - x1))
        assert(res != Double.NaN)
        res
      }
    } else {
      if (x2 > x1 + 20)
        x2
      else {
        val res = x2 + log(1.0 + exp(x1 - x2))
        assert(res != Double.NaN)
        res
      }

    }
  }

  /**
   * Given a vector in log numbers, normalizes the vector in places also
   * in log numbers.
   */
  def logNormalizeInPlace(v: Array[Double]): Unit = {

    // Compute the normalization factor:
    val norm_fact = logSumExp(v)
    assert(norm_fact != Double.NaN, (v.mkString, norm_fact))
    if (norm_fact == Double.NegativeInfinity) {
      logWarning("Warning: normalization underflow")
      return ; // Nothing to do, this vector is already 0.
    }
    // Substract it to the vector
    var i = v.length - 1
    while (i >= 0) {
      v(i) -= norm_fact
      assert(v(i) <= 0, (v.mkString, norm_fact))
      i -= 1
    }
  }

  def logNormalize(in: Array[Double], out: Array[Double]): Unit = {

    // Compute the normalization factor:
    var norm_fact = logSumExp(in)
    assert(norm_fact != Double.NaN, (in.mkString, norm_fact))
    if (norm_fact == Double.NegativeInfinity) {
      logWarning("Warning: normalization underflow")
      norm_fact = 0;
    }
    // Substract it to the vector
    var i = in.length - 1
    while (i >= 0) {
      out(i) = in(i) - norm_fact
      assert(out(i) != Double.NaN, (i, out.mkString, norm_fact))
      assert(out(i) <= 0, (out.mkString, norm_fact))
      i -= 1
    }
  }

  def convertToLogDomainInPlace(v: Array[Double]): Unit = {
    var i = v.length - 1
    while (i >= 0) {
      if (v(i) == 0) {
        v(i) = Double.NegativeInfinity
      } else {
        v(i) = math.log(v(i))
      }
      i -= 1
    }
  }
}

