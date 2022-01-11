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

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import tools.aqua.koral.util.Relation.*

internal class GuardConstraintTest {

  @Test
  fun `reported labels are correct`() {
    val constraint = GuardConstraint(xyIndex, abIndex)
    assertEquals(xySourceSet + abParameterSet, constraint.columns.toSet())
    assertEquals(xySourceSet + abParameterSet, constraint.rows.toSet())
  }

  @Test
  fun `reported variables are correct`() {
    val constraint = GuardConstraint(xyIndex, abIndex)
    assertEquals(xyIndex, constraint.registers)
    assertEquals(abIndex, constraint.parameters)
  }

  @Test
  fun `all cells initialized correctly`() {
    val constraint = GuardConstraint(xyIndex, abIndex)

    assertEquals(EQUAL, constraint[xSource, xSource])
    assertEquals(UNKNOWN, constraint[xSource, ySource])
    assertEquals(UNKNOWN, constraint[xSource, aParameter])
    assertEquals(UNKNOWN, constraint[xSource, bParameter])

    assertEquals(UNKNOWN, constraint[ySource, xSource])
    assertEquals(EQUAL, constraint[ySource, ySource])
    assertEquals(UNKNOWN, constraint[ySource, aParameter])
    assertEquals(UNKNOWN, constraint[ySource, bParameter])

    assertEquals(UNKNOWN, constraint[aParameter, xSource])
    assertEquals(UNKNOWN, constraint[aParameter, ySource])
    assertEquals(EQUAL, constraint[aParameter, aParameter])
    assertEquals(UNKNOWN, constraint[aParameter, bParameter])

    assertEquals(UNKNOWN, constraint[bParameter, xSource])
    assertEquals(UNKNOWN, constraint[bParameter, ySource])
    assertEquals(UNKNOWN, constraint[bParameter, aParameter])
    assertEquals(EQUAL, constraint[bParameter, bParameter])
  }

  @Test
  fun `written relations are read`() {
    val constraint = GuardConstraint(xyIndex, abIndex).also { it[xSource, aParameter] = EQUAL }
    assertEquals(EQUAL, constraint[xSource, aParameter])
  }

  @Test
  fun `written relations are read on inverted indices`() {
    val constraint = GuardConstraint(xyIndex, abIndex).also { it[xSource, aParameter] = EQUAL }
    assertEquals(EQUAL, constraint[aParameter, xSource])
  }

  @Test
  fun `put reads correct value`() {
    val constraint = GuardConstraint(xyIndex, abIndex)
    assertEquals(UNKNOWN, constraint.put(xSource, aParameter, EQUAL))
  }

  @Test
  fun `put writes correct value`() {
    val constraint = GuardConstraint(xyIndex, abIndex).also { it.put(xSource, aParameter, EQUAL) }
    assertEquals(EQUAL, constraint[xSource, aParameter])
  }

  @Test
  fun `copying yields identical content`() {
    val constraint =
        GuardConstraint(xyIndex, abIndex)
            .also {
              it[xSource, aParameter] = EQUAL
              it[aParameter, ySource] = DISTINCT
            }
            .copyOf()

    assertEquals(EQUAL, constraint[xSource, xSource])
    assertEquals(UNKNOWN, constraint[xSource, ySource])
    assertEquals(EQUAL, constraint[xSource, aParameter])
    assertEquals(UNKNOWN, constraint[xSource, bParameter])

    assertEquals(UNKNOWN, constraint[ySource, xSource])
    assertEquals(EQUAL, constraint[ySource, ySource])
    assertEquals(DISTINCT, constraint[ySource, aParameter])
    assertEquals(UNKNOWN, constraint[ySource, bParameter])

    assertEquals(EQUAL, constraint[aParameter, xSource])
    assertEquals(DISTINCT, constraint[aParameter, ySource])
    assertEquals(EQUAL, constraint[aParameter, aParameter])
    assertEquals(UNKNOWN, constraint[aParameter, bParameter])

    assertEquals(UNKNOWN, constraint[bParameter, xSource])
    assertEquals(UNKNOWN, constraint[bParameter, ySource])
    assertEquals(UNKNOWN, constraint[bParameter, aParameter])
    assertEquals(EQUAL, constraint[bParameter, bParameter])
  }

  @Test
  fun `copying decouples storage`() {
    val constraint = GuardConstraint(xyIndex, abIndex)
    constraint.copyOf()[xSource, aParameter] = EQUAL
    assertEquals(UNKNOWN, constraint[xSource, aParameter])
  }

  @Test
  fun `equal constraints compare equal`() {
    val constraint = GuardConstraint(xyIndex, abIndex).also { it.put(xSource, aParameter, EQUAL) }
    val other = GuardConstraint(xyIndex, abIndex).also { it.put(xSource, aParameter, EQUAL) }
    Assertions.assertTrue(constraint == other)
  }

  @Test
  fun `different-register constraints do not compare equal`() {
    val constraint = GuardConstraint(xyIndex, abIndex).also { it.put(xSource, aParameter, EQUAL) }
    val other = GuardConstraint(xyzIndex, abIndex).also { it.put(xSource, aParameter, EQUAL) }
    Assertions.assertTrue(constraint != other)
  }

  @Test
  fun `different-parameter constraints do not compare equal`() {
    val constraint = GuardConstraint(xyIndex, abIndex).also { it.put(xSource, aParameter, EQUAL) }
    val other = GuardConstraint(xyIndex, abcIndex).also { it.put(xSource, aParameter, EQUAL) }
    Assertions.assertTrue(constraint != other)
  }

  @Test
  fun `different-content constraints do not compare equal`() {
    val constraint = GuardConstraint(xyIndex, abIndex).also { it.put(xSource, bParameter, EQUAL) }
    val other = GuardConstraint(xyIndex, abIndex).also { it.put(aParameter, xSource, EQUAL) }
    Assertions.assertTrue(constraint != other)
  }

  @Test
  fun `equal constraints have same hashCode`() {
    val constraint = GuardConstraint(xyIndex, abIndex).also { it.put(xSource, aParameter, EQUAL) }
    val other = GuardConstraint(xyIndex, abIndex).also { it.put(xSource, aParameter, EQUAL) }
    Assertions.assertTrue(constraint.hashCode() == other.hashCode())
  }
}
