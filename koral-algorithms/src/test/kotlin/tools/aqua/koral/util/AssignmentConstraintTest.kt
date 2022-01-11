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
import tools.aqua.koral.util.Relation.EQUAL
import tools.aqua.koral.util.Relation.UNKNOWN

internal class AssignmentConstraintTest {

  @Test
  fun `reported labels are correct`() {
    val constraint = AssignmentConstraint(xyIndex, abIndex)
    assertEquals(xySourceSet + abParameterSet, constraint.columns.toSet())
    assertEquals(xyDestinationSet, constraint.rows.toSet())
  }

  @Test
  fun `reported variables are correct`() {
    val constraint = AssignmentConstraint(xyIndex, abIndex)
    assertEquals(xyIndex, constraint.registers)
    assertEquals(abIndex, constraint.parameters)
  }

  @Test
  fun `all cells initialized to unknown`() {
    val constraint = AssignmentConstraint(xyIndex, abIndex)

    assertEquals(UNKNOWN, constraint[xSource, xDestination])
    assertEquals(UNKNOWN, constraint[xSource, yDestination])
    assertEquals(UNKNOWN, constraint[ySource, xDestination])
    assertEquals(UNKNOWN, constraint[ySource, yDestination])
    assertEquals(UNKNOWN, constraint[aParameter, xDestination])
    assertEquals(UNKNOWN, constraint[aParameter, yDestination])
    assertEquals(UNKNOWN, constraint[bParameter, xDestination])
    assertEquals(UNKNOWN, constraint[bParameter, yDestination])
  }

  @Test
  fun `written relations are read`() {
    val constraint =
        AssignmentConstraint(xyIndex, abIndex).also { it[xSource, yDestination] = EQUAL }
    assertEquals(EQUAL, constraint[xSource, yDestination])
  }

  @Test
  fun `written relations are read on inverted indices`() {
    val constraint =
        AssignmentConstraint(xyIndex, abIndex).also { it[xSource, yDestination] = EQUAL }
    assertEquals(EQUAL, constraint[yDestination, xSource])
  }

  @Test
  fun `put reads correct value`() {
    val constraint = AssignmentConstraint(xyIndex, abIndex)
    assertEquals(UNKNOWN, constraint.put(xSource, yDestination, EQUAL))
  }

  @Test
  fun `put writes correct value`() {
    val constraint =
        AssignmentConstraint(xyIndex, abIndex).also { it.put(xSource, yDestination, EQUAL) }
    assertEquals(EQUAL, constraint[xSource, yDestination])
  }

  @Test
  fun `copying yields identical content`() {
    val constraint =
        AssignmentConstraint(xyIndex, abIndex)
            .also {
              it[xSource, yDestination] = EQUAL
              it[aParameter, xDestination] = EQUAL
            }
            .copyOf()

    assertEquals(UNKNOWN, constraint[xSource, xDestination])
    assertEquals(EQUAL, constraint[xSource, yDestination])
    assertEquals(UNKNOWN, constraint[ySource, xDestination])
    assertEquals(UNKNOWN, constraint[ySource, yDestination])
    assertEquals(EQUAL, constraint[aParameter, xDestination])
    assertEquals(UNKNOWN, constraint[aParameter, yDestination])
    assertEquals(UNKNOWN, constraint[bParameter, xDestination])
    assertEquals(UNKNOWN, constraint[bParameter, yDestination])
  }

  @Test
  fun `copying decouples storage`() {
    val constraint = AssignmentConstraint(xyIndex, abIndex)
    constraint.copyOf()[xSource, yDestination] = EQUAL
    assertEquals(UNKNOWN, constraint[xSource, yDestination])
  }

  @Test
  fun `equal constraints compare equal`() {
    val constraint =
        AssignmentConstraint(xyIndex, abIndex).also { it.put(xSource, yDestination, EQUAL) }
    val other = AssignmentConstraint(xyIndex, abIndex).also { it.put(xSource, yDestination, EQUAL) }
    Assertions.assertTrue(constraint == other)
  }

  @Test
  fun `different-register constraints do not compare equal`() {
    val constraint =
        AssignmentConstraint(xyIndex, abIndex).also { it.put(xSource, yDestination, EQUAL) }
    val other =
        AssignmentConstraint(xyzIndex, abIndex).also { it.put(xSource, yDestination, EQUAL) }
    Assertions.assertTrue(constraint != other)
  }

  @Test
  fun `different-parameter constraints do not compare equal`() {
    val constraint =
        AssignmentConstraint(xyIndex, abIndex).also { it.put(xSource, yDestination, EQUAL) }
    val other =
        AssignmentConstraint(xyIndex, abcIndex).also { it.put(xSource, yDestination, EQUAL) }
    Assertions.assertTrue(constraint != other)
  }

  @Test
  fun `different-content constraints do not compare equal`() {
    val constraint =
        AssignmentConstraint(xyIndex, abIndex).also { it.put(xSource, yDestination, EQUAL) }
    val other = AssignmentConstraint(xyIndex, abIndex).also { it.put(ySource, xDestination, EQUAL) }
    Assertions.assertTrue(constraint != other)
  }

  @Test
  fun `equal constraints have same hashCode`() {
    val constraint =
        AssignmentConstraint(xyIndex, abIndex).also { it.put(xSource, yDestination, EQUAL) }
    val other = AssignmentConstraint(xyIndex, abIndex).also { it.put(xSource, yDestination, EQUAL) }
    Assertions.assertTrue(constraint.hashCode() == other.hashCode())
  }
}
