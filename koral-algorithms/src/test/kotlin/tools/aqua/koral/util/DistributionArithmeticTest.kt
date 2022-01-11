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

import org.apache.commons.math3.distribution.UniformIntegerDistribution
import org.apache.commons.math3.random.JDKRandomGenerator
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.RepeatedTest
import org.junit.jupiter.api.Test

internal class DistributionAdditionSampleTest {
  private val random = JDKRandomGenerator()

  @BeforeEach
  fun reseedRandom() {
    random.setSeed(10000)
  }

  @RepeatedTest(1000)
  fun `addition gives correct samples`() {
    val distribution = UniformIntegerDistribution(random, -10, 10) + 10
    assertThat(distribution.sample()).isBetween(0, 20)
  }

  @RepeatedTest(1000)
  fun `addition gives correct multi-samples`() {
    val distribution = UniformIntegerDistribution(random, -10, 10) + 10
    distribution.sample(1000).forEachIndexed { _, sample -> assertThat(sample).isBetween(0, 20) }
  }
}

internal class DistributionAdditionTest {
  private val random = JDKRandomGenerator()

  @BeforeEach
  fun reseedRandom() {
    random.setSeed(10000)
  }

  @Test
  fun `addition gives correct probability below range`() {
    assertEquals(
        0.0,
        (UniformIntegerDistribution(random, 0, 9) + 10).probability(9),
    )
  }

  @Test
  fun `addition gives correct probability in range`() {
    assertEquals(
        0.1,
        (UniformIntegerDistribution(random, 0, 9) + 10).probability(10),
    )
  }

  @Test
  fun `addition gives correct probability above range`() {
    assertEquals(
        0.0,
        (UniformIntegerDistribution(random, 0, 9) + 10).probability(20),
    )
  }

  @Test
  fun `addition gives correct cumulative probability below range`() {
    assertEquals(
        0.0,
        (UniformIntegerDistribution(random, 0, 9) + 10).cumulativeProbability(9),
    )
  }

  @Test
  fun `addition gives correct cumulative probability in range`() {
    assertEquals(
        0.5,
        (UniformIntegerDistribution(random, 0, 9) + 10).cumulativeProbability(14),
    )
  }

  @Test
  fun `addition gives correct cumulative probability above range`() {
    assertEquals(
        1.0,
        (UniformIntegerDistribution(random, 0, 9) + 10).cumulativeProbability(20),
    )
  }

  @Test
  fun `addition gives correct inverse cumulative probability below range`() {
    assertEquals(
        10,
        (UniformIntegerDistribution(random, 0, 9) + 10).inverseCumulativeProbability(0.0),
    )
  }

  @Test
  fun `addition gives correct inverse cumulative probability in range`() {
    assertEquals(
        14,
        (UniformIntegerDistribution(random, 0, 9) + 10).inverseCumulativeProbability(0.5),
    )
  }

  @Test
  fun `addition gives correct inverse cumulative probability above range`() {
    assertEquals(
        19, (UniformIntegerDistribution(random, 0, 9) + 10).inverseCumulativeProbability(1.0))
  }

  @Test
  fun `addition gives correct 2-way cumulative probability below range`() {
    assertEquals(
        0.0,
        (UniformIntegerDistribution(random, 0, 9) + 10).cumulativeProbability(-1, 9),
    )
  }

  @Test
  fun `addition gives correct 2-way cumulative probability in range`() {
    assertEquals(
        1.0,
        (UniformIntegerDistribution(random, 0, 9) + 10).cumulativeProbability(9, 19),
    )
  }

  @Test
  fun `addition gives correct 2-way cumulative probability above range`() {
    assertEquals(
        0.0,
        (UniformIntegerDistribution(random, 0, 9) + 10).cumulativeProbability(19, 29),
    )
  }

  @Test
  fun `addition gives correct mean`() {
    assertEquals(
        14.5,
        (UniformIntegerDistribution(random, 0, 9) + 10).numericalMean,
    )
  }

  @Test
  fun `addition gives correct variance`() {
    assertEquals(
        2.0,
        (UniformIntegerDistribution(random, 0, 4) + 10).numericalVariance,
    )
  }

  @Test
  fun `addition gives correct support lower bound`() {
    assertEquals(
        10,
        (UniformIntegerDistribution(random, 0, 9) + 10).supportLowerBound,
    )
  }

  @Test
  fun `addition gives correct support upper bound`() {
    assertEquals(
        19,
        (UniformIntegerDistribution(random, 0, 9) + 10).supportUpperBound,
    )
  }

  @Test
  fun `addition gives correct support connectivity`() {
    assertTrue(
        (UniformIntegerDistribution(random, 0, 9) + 10).isSupportConnected,
    )
  }

  @Test
  fun `addition passes seed resets correctly`() {
    val distribution = UniformIntegerDistribution(random, 0, 9) + 10

    distribution.reseedRandomGenerator(123)
    val first = distribution.sample()
    distribution.reseedRandomGenerator(123)
    val second = distribution.sample()

    assertEquals(first, second)
  }
}
