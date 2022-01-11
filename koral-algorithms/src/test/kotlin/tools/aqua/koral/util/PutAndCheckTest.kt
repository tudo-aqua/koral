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
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import tools.aqua.koral.util.Relation.DISTINCT
import tools.aqua.koral.util.Relation.EQUAL

internal class PutAndCheckTest {

  @Test
  fun `overwriting unknown is permitted`() {
    val constraint = LocationConstraint(xyIndex)
    assertDoesNotThrow { constraint.putAndCheck(xSource, ySource, EQUAL) }
  }

  @Test
  fun `overwriting with same value is permitted`() {
    val constraint = LocationConstraint(xyIndex).also { it[xSource, ySource] = EQUAL }
    assertDoesNotThrow { constraint.putAndCheck(xSource, ySource, EQUAL) }
  }

  @Test
  fun `overwriting diagonal with equal is permitted`() {
    val constraint = LocationConstraint(xyIndex)
    assertDoesNotThrow { constraint.putAndCheck(xSource, xSource, EQUAL) }
  }

  @Test
  fun `overwriting with new value is infeasible`() {
    val constraint = LocationConstraint(xyIndex).also { it[xSource, ySource] = EQUAL }
    assertThrows<InfeasibleConstraintException> {
      constraint.putAndCheck(xSource, ySource, DISTINCT)
    }
  }

  @Test
  fun `overwriting diagonal with distinct is infeasible`() {
    val constraint = LocationConstraint(xyIndex)
    assertThrows<InfeasibleConstraintException> {
      constraint.putAndCheck(xSource, xSource, DISTINCT)
    }
  }
}
