/*
 * Copyright 2022 The Koral Authors
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

import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import tools.aqua.koral.util.Relation.*

internal class MergeTest {

  @Test
  fun `merging yields correct values`() {
    val constraint = LocationConstraint(xyzIndex).also { it[xSource, ySource] = EQUAL }
    val other = LocationConstraint(xyzIndex).also { it[ySource, zSource] = DISTINCT }
    constraint += other

    assertEquals(EQUAL, constraint[xSource, xSource])
    assertEquals(EQUAL, constraint[xSource, ySource])
    assertEquals(UNKNOWN, constraint[xSource, zSource])

    assertEquals(EQUAL, constraint[ySource, xSource])
    assertEquals(EQUAL, constraint[ySource, ySource])
    assertEquals(DISTINCT, constraint[ySource, zSource])

    assertEquals(UNKNOWN, constraint[zSource, xSource])
    assertEquals(DISTINCT, constraint[zSource, ySource])
    assertEquals(EQUAL, constraint[zSource, zSource])
  }

  @Test
  fun `overwriting unknown is permitted`() {
    val constraint = LocationConstraint(xyzIndex).also { it[xSource, ySource] = EQUAL }
    val other = LocationConstraint(xyzIndex).also { it[ySource, zSource] = DISTINCT }
    assertDoesNotThrow { constraint += other }
  }

  @Test
  fun `overwriting with same value is permitted`() {
    val constraint = LocationConstraint(xyzIndex).also { it[xSource, ySource] = EQUAL }
    val other = LocationConstraint(xyzIndex).also { it[xSource, ySource] = EQUAL }
    assertDoesNotThrow { constraint += other }
  }

  @Test
  fun `overwriting with new value is infeasible`() {
    val constraint = LocationConstraint(xyzIndex).also { it[xSource, ySource] = EQUAL }
    val other = LocationConstraint(xyzIndex).also { it[xSource, ySource] = DISTINCT }
    assertThrows<InfeasibleConstraintException> { constraint += other }
  }
}
