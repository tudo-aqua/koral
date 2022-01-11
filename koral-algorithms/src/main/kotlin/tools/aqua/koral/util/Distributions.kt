/*
 * Copyright 2020-2022 The Koral Authors
 * SPDX-License-Identifier: Apache-2.0
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package tools.aqua.koral.util

import java.lang.Math.addExact
import java.lang.Math.subtractExact
import org.apache.commons.math3.distribution.AbstractIntegerDistribution
import org.apache.commons.math3.distribution.IntegerDistribution
import org.apache.commons.math3.exception.OutOfRangeException

/**
 * Apply a map function to every element of this array. This mutates this array's contents.
 *
 * @param transform the transformation function.
 * @return this object.
 *
 * @see [IntArray.map]
 */
fun IntArray.mapInPlace(transform: (Int) -> Int): IntArray = apply {
  indices.forEach { this[it] = transform(this[it]) }
}

/**
 * Perform overflow-safe addition, i.e., `this + [other]`
 *
 * @param other the second summand.
 * @return the sum.
 * @throws ArithmeticException the the result would overflow.
 *
 * @see addExact
 */
infix fun Int.safePlus(other: Int): Int = addExact(this, other)

/**
 * Perform saturated addition, i.e., `min(max([Int.MIN_VALUE], this + [other]), [Int.MAX_VALUE])`
 *
 * @param other the second summand.
 * @return the sum.
 */
infix fun Int.satPlus(other: Int): Int {
  val result = this + other
  return if (this < 0 && other < 0 && result > 0) {
    Int.MIN_VALUE
  } else if (this > 0 && other > 0 && result < 0) {
    Int.MAX_VALUE
  } else {
    result
  }
}

/**
 * Perform overflow-safe subtraction, i.e., `this - [other]`
 *
 * @param other the subtrahend.
 * @return the difference.
 * @throws ArithmeticException the the result would overflow.
 *
 * @see subtractExact
 */
infix fun Int.safeMinus(other: Int): Int = subtractExact(this, other)

/**
 * Perform saturated subtraction, i.e., `min(max([Int.MIN_VALUE], this - [other]), [Int.MAX_VALUE])`
 *
 * @param other the subtrahend.
 * @return the difference.
 */
infix fun Int.satMinus(other: Int): Int {
  val result = this - other
  return if (this < 0 && other > 0 && result > 0) {
    Int.MIN_VALUE
  } else if (this > 0 && other < 0 && result < 0) {
    Int.MAX_VALUE
  } else {
    result
  }
}

/**
 * Add a constant offset to this distribution's output.
 *
 * @param offset the offset.
 * @return this object.
 */
operator fun IntegerDistribution.plus(offset: Int): IntegerDistribution = let { inner ->
  object : IntegerDistribution by inner {
    override fun probability(x: Int): Double = inner.probability(x safeMinus offset)

    override fun cumulativeProbability(x: Int): Double =
        inner.cumulativeProbability(x safeMinus offset)

    override fun cumulativeProbability(x0: Int, x1: Int): Double =
        inner.cumulativeProbability(x0 safeMinus offset, x1 safeMinus offset)

    override fun inverseCumulativeProbability(p: Double): Int =
        inner.inverseCumulativeProbability(p) satPlus offset

    override fun getNumericalMean(): Double = inner.numericalMean + offset

    override fun getSupportLowerBound(): Int = inner.supportLowerBound satPlus offset

    override fun getSupportUpperBound(): Int = inner.supportUpperBound satPlus offset

    override fun sample(): Int = inner.sample() satPlus offset

    override fun sample(sampleSize: Int): IntArray =
        inner.sample(sampleSize).mapInPlace { it satPlus offset }
  }
}

/**
 * Subtract a constant offset from this distribution's output.
 *
 * @param offset the offset.
 * @return this object.
 */
operator fun IntegerDistribution.minus(offset: Int): IntegerDistribution = this + -offset

/** Implementation of the constant integer distribution. */
class ConstantIntegerDistribution(
    /** Constant value of the distribution. */
    private val value: Int,
) : AbstractIntegerDistribution(null) {

  override fun cumulativeProbability(x: Int): Double = if (x < value) 0.0 else 1.0

  override fun getNumericalMean(): Double = value.toDouble()

  override fun getNumericalVariance(): Double = 0.0

  override fun getSupportLowerBound(): Int = value

  override fun getSupportUpperBound(): Int = value

  override fun isSupportConnected(): Boolean = true

  override fun inverseCumulativeProbability(p: Double): Int =
      if (p < 0.0 || p > 1.0) throw OutOfRangeException(p, 0, 1) else value

  override fun probability(x: Int): Double = if (x == value) 1.0 else 0.0

  override fun sample(): Int = value

  /**
   * Override with no-op (there is no generator).
   * @param seed (ignored)
   */
  override fun reseedRandomGenerator(seed: Long) = Unit
}
