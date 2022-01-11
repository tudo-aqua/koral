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

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.RepeatedTest
import org.junit.jupiter.api.Test

internal class ConstantIntegerDistributionTest {
  @RepeatedTest(1000)
  fun `distribution gives constant samples`() {
    val distribution = ConstantIntegerDistribution(100)
    assertArrayEquals(IntArray(1000) { 100 }, IntArray(1000) { distribution.sample() })
  }

  @Test
  fun `distribution gives constant multi-samples`() {
    assertArrayEquals(IntArray(1000) { 100 }, ConstantIntegerDistribution(100).sample(1000))
  }

  @Test
  fun `distribution gives correct probability below value`() {
    assertEquals(
        0.0,
        ConstantIntegerDistribution(100).probability(99),
    )
  }

  @Test
  fun `distribution gives correct probability for value`() {
    assertEquals(
        1.0,
        ConstantIntegerDistribution(100).probability(100),
    )
  }

  @Test
  fun `distribution gives correct probability above value`() {
    assertEquals(
        0.0,
        ConstantIntegerDistribution(100).probability(101),
    )
  }

  @Test
  fun `distribution gives correct cumulative probability below value`() {
    assertEquals(
        0.0,
        ConstantIntegerDistribution(100).cumulativeProbability(99),
    )
  }

  @Test
  fun `distribution gives correct cumulative probability on value`() {
    assertEquals(
        1.0,
        ConstantIntegerDistribution(100).cumulativeProbability(100),
    )
  }

  @Test
  fun `distribution gives correct cumulative probability above value`() {
    assertEquals(
        1.0,
        ConstantIntegerDistribution(100).cumulativeProbability(101),
    )
  }

  @Test
  fun `distribution gives correct 0-percent inverse cumulative probability`() {
    assertEquals(
        100,
        ConstantIntegerDistribution(100).inverseCumulativeProbability(0.0),
    )
  }

  @Test
  fun `distribution gives correct 50-percent inverse cumulative probability`() {
    assertEquals(
        100,
        ConstantIntegerDistribution(100).inverseCumulativeProbability(0.5),
    )
  }

  @Test
  fun `distribution gives correct 100-percent inverse cumulative probability`() {
    assertEquals(
        100,
        ConstantIntegerDistribution(100).inverseCumulativeProbability(1.0),
    )
  }

  @Test
  fun `distribution gives correct 2-way cumulative probability below value`() {
    assertEquals(
        0.0,
        ConstantIntegerDistribution(100).cumulativeProbability(-1, 99),
    )
  }

  @Test
  fun `distribution gives correct 2-way cumulative probability on value`() {
    assertEquals(
        1.0,
        ConstantIntegerDistribution(100).cumulativeProbability(89, 109),
    )
  }

  @Test
  fun `distribution gives correct 2-way cumulative probability above value`() {
    assertEquals(
        0.0,
        ConstantIntegerDistribution(100).cumulativeProbability(109, 1000),
    )
  }

  @Test
  fun `distribution gives correct mean`() {
    assertEquals(
        100.0,
        ConstantIntegerDistribution(100).numericalMean,
    )
  }

  @Test
  fun `distribution gives correct variance`() {
    assertEquals(
        0.0,
        ConstantIntegerDistribution(100).numericalVariance,
    )
  }

  @Test
  fun `distribution gives correct support lower bound`() {
    assertEquals(
        100,
        ConstantIntegerDistribution(100).supportLowerBound,
    )
  }

  @Test
  fun `distribution gives correct support upper bound`() {
    assertEquals(
        100,
        ConstantIntegerDistribution(100).supportUpperBound,
    )
  }

  @Test
  fun `distribution gives correct support connectivity`() {
    assertTrue(
        ConstantIntegerDistribution(100).isSupportConnected,
    )
  }
}
