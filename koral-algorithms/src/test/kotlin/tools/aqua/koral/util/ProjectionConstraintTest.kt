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

internal class ProjectionConstraintTest {

  private fun <Variable> assembleEmpty(registers: Index<Variable>, parameters: Index<Variable>) =
      ProjectionConstraint.assemble(
          LocationConstraint(registers),
          GuardConstraint(registers, parameters),
          AssignmentConstraint(registers, parameters),
          LocationConstraint(registers))

  @Test
  fun `reported labels are correct`() {
    val constraint = assembleEmpty(xyIndex, abIndex)
    assertEquals(xySourceSet + abParameterSet + xyDestinationSet, constraint.columns.toSet())
    assertEquals(xySourceSet + abParameterSet + xyDestinationSet, constraint.rows.toSet())
  }

  @Test
  fun `reported variables are correct`() {
    val constraint = assembleEmpty(xyIndex, abIndex)
    assertEquals(xyIndex, constraint.registers)
    assertEquals(abIndex, constraint.parameters)
  }

  @Test
  fun `all cells initialized correctly`() {
    val constraint = assembleEmpty(xyIndex, abIndex)

    assertEquals(EQUAL, constraint[xSource, xSource])
    assertEquals(UNKNOWN, constraint[xSource, ySource])
    assertEquals(UNKNOWN, constraint[xSource, aParameter])
    assertEquals(UNKNOWN, constraint[xSource, bParameter])
    assertEquals(UNKNOWN, constraint[xSource, xDestination])
    assertEquals(UNKNOWN, constraint[xSource, yDestination])

    assertEquals(UNKNOWN, constraint[ySource, xSource])
    assertEquals(EQUAL, constraint[ySource, ySource])
    assertEquals(UNKNOWN, constraint[ySource, aParameter])
    assertEquals(UNKNOWN, constraint[ySource, bParameter])
    assertEquals(UNKNOWN, constraint[ySource, xDestination])
    assertEquals(UNKNOWN, constraint[ySource, yDestination])

    assertEquals(UNKNOWN, constraint[aParameter, xSource])
    assertEquals(UNKNOWN, constraint[aParameter, ySource])
    assertEquals(EQUAL, constraint[aParameter, aParameter])
    assertEquals(UNKNOWN, constraint[aParameter, bParameter])
    assertEquals(UNKNOWN, constraint[aParameter, xDestination])
    assertEquals(UNKNOWN, constraint[aParameter, yDestination])

    assertEquals(UNKNOWN, constraint[bParameter, xSource])
    assertEquals(UNKNOWN, constraint[bParameter, ySource])
    assertEquals(UNKNOWN, constraint[bParameter, aParameter])
    assertEquals(EQUAL, constraint[bParameter, bParameter])
    assertEquals(UNKNOWN, constraint[bParameter, xDestination])
    assertEquals(UNKNOWN, constraint[bParameter, yDestination])

    assertEquals(UNKNOWN, constraint[xDestination, xSource])
    assertEquals(UNKNOWN, constraint[xDestination, ySource])
    assertEquals(UNKNOWN, constraint[xDestination, aParameter])
    assertEquals(UNKNOWN, constraint[xDestination, bParameter])
    assertEquals(EQUAL, constraint[xDestination, xDestination])
    assertEquals(UNKNOWN, constraint[xDestination, yDestination])

    assertEquals(UNKNOWN, constraint[yDestination, xSource])
    assertEquals(UNKNOWN, constraint[yDestination, ySource])
    assertEquals(UNKNOWN, constraint[yDestination, aParameter])
    assertEquals(UNKNOWN, constraint[yDestination, bParameter])
    assertEquals(UNKNOWN, constraint[yDestination, xDestination])
    assertEquals(EQUAL, constraint[yDestination, yDestination])
  }

  @Test
  fun `written relations are read`() {
    val constraint = assembleEmpty(xyIndex, abIndex).also { it[xSource, aParameter] = EQUAL }
    assertEquals(EQUAL, constraint[xSource, aParameter])
  }

  @Test
  fun `written relations are read on inverted indices`() {
    val constraint = assembleEmpty(xyIndex, abIndex).also { it[xSource, aParameter] = EQUAL }
    assertEquals(EQUAL, constraint[aParameter, xSource])
  }

  @Test
  fun `put reads correct value`() {
    val constraint = assembleEmpty(xyIndex, abIndex)
    assertEquals(UNKNOWN, constraint.put(xSource, aParameter, EQUAL))
  }

  @Test
  fun `put writes correct value`() {
    val constraint = assembleEmpty(xyIndex, abIndex).also { it.put(xSource, aParameter, EQUAL) }
    assertEquals(EQUAL, constraint[xSource, aParameter])
  }

  @Test
  fun `assembling copies values`() {
    val constraint =
        ProjectionConstraint.assemble(
            LocationConstraint(xyIndex).also { it[xSource, ySource] = EQUAL },
            GuardConstraint(xyIndex, abIndex).also {
              it[ySource, bParameter] = EQUAL
              it[aParameter, bParameter] = DISTINCT
            },
            AssignmentConstraint(xyIndex, abIndex).also {
              it[xSource, xDestination] = EQUAL
              it[bParameter, yDestination] = EQUAL
            },
            LocationConstraint(xyIndex).also { it[xSource, ySource] = DISTINCT })

    assertEquals(EQUAL, constraint[xSource, xSource])
    assertEquals(EQUAL, constraint[xSource, ySource])
    assertEquals(UNKNOWN, constraint[xSource, aParameter])
    assertEquals(UNKNOWN, constraint[xSource, bParameter])
    assertEquals(EQUAL, constraint[xSource, xDestination])
    assertEquals(UNKNOWN, constraint[xSource, yDestination])

    assertEquals(EQUAL, constraint[ySource, xSource])
    assertEquals(EQUAL, constraint[ySource, ySource])
    assertEquals(UNKNOWN, constraint[ySource, aParameter])
    assertEquals(EQUAL, constraint[ySource, bParameter])
    assertEquals(UNKNOWN, constraint[ySource, xDestination])
    assertEquals(UNKNOWN, constraint[ySource, yDestination])

    assertEquals(UNKNOWN, constraint[aParameter, xSource])
    assertEquals(UNKNOWN, constraint[aParameter, ySource])
    assertEquals(EQUAL, constraint[aParameter, aParameter])
    assertEquals(DISTINCT, constraint[aParameter, bParameter])
    assertEquals(UNKNOWN, constraint[aParameter, xDestination])
    assertEquals(UNKNOWN, constraint[aParameter, yDestination])

    assertEquals(UNKNOWN, constraint[bParameter, xSource])
    assertEquals(EQUAL, constraint[bParameter, ySource])
    assertEquals(DISTINCT, constraint[bParameter, aParameter])
    assertEquals(EQUAL, constraint[bParameter, bParameter])
    assertEquals(UNKNOWN, constraint[bParameter, xDestination])
    assertEquals(EQUAL, constraint[bParameter, yDestination])

    assertEquals(EQUAL, constraint[xDestination, xSource])
    assertEquals(UNKNOWN, constraint[xDestination, ySource])
    assertEquals(UNKNOWN, constraint[xDestination, aParameter])
    assertEquals(UNKNOWN, constraint[xDestination, bParameter])
    assertEquals(EQUAL, constraint[xDestination, xDestination])
    assertEquals(DISTINCT, constraint[xDestination, yDestination])

    assertEquals(UNKNOWN, constraint[yDestination, xSource])
    assertEquals(UNKNOWN, constraint[yDestination, ySource])
    assertEquals(UNKNOWN, constraint[yDestination, aParameter])
    assertEquals(EQUAL, constraint[yDestination, bParameter])
    assertEquals(DISTINCT, constraint[yDestination, xDestination])
    assertEquals(EQUAL, constraint[yDestination, yDestination])
  }

  @Test
  fun `assembling decouples storage`() {
    val source = LocationConstraint(xyIndex)
    val guard = GuardConstraint(xyIndex, abIndex)
    val assignment = AssignmentConstraint(xyIndex, abIndex)
    val destination = LocationConstraint(xyIndex)
    ProjectionConstraint.assemble(source, guard, assignment, destination).also {
      it[xSource, ySource] = EQUAL
      it[aParameter, bParameter] = DISTINCT
      it[xSource, xDestination] = EQUAL
      it[xDestination, yDestination] = DISTINCT
    }

    assertEquals(UNKNOWN, source[xSource, ySource])
    assertEquals(UNKNOWN, guard[xSource, ySource])
    assertEquals(UNKNOWN, guard[aParameter, bParameter])
    assertEquals(UNKNOWN, assignment[xSource, xDestination])
    assertEquals(UNKNOWN, destination[xSource, ySource])
  }

  @Test
  fun `copying yields identical content`() {
    val constraint =
        assembleEmpty(xyIndex, abIndex)
            .also {
              it[xSource, aParameter] = EQUAL
              it[xDestination, ySource] = DISTINCT
            }
            .copyOf()

    assertEquals(EQUAL, constraint[xSource, xSource])
    assertEquals(UNKNOWN, constraint[xSource, ySource])
    assertEquals(EQUAL, constraint[xSource, aParameter])
    assertEquals(UNKNOWN, constraint[xSource, bParameter])
    assertEquals(UNKNOWN, constraint[xSource, xDestination])
    assertEquals(UNKNOWN, constraint[xSource, yDestination])

    assertEquals(UNKNOWN, constraint[ySource, xSource])
    assertEquals(EQUAL, constraint[ySource, ySource])
    assertEquals(UNKNOWN, constraint[ySource, aParameter])
    assertEquals(UNKNOWN, constraint[ySource, bParameter])
    assertEquals(DISTINCT, constraint[ySource, xDestination])
    assertEquals(UNKNOWN, constraint[ySource, yDestination])

    assertEquals(EQUAL, constraint[aParameter, xSource])
    assertEquals(UNKNOWN, constraint[aParameter, ySource])
    assertEquals(EQUAL, constraint[aParameter, aParameter])
    assertEquals(UNKNOWN, constraint[aParameter, bParameter])
    assertEquals(UNKNOWN, constraint[aParameter, xDestination])
    assertEquals(UNKNOWN, constraint[aParameter, yDestination])

    assertEquals(UNKNOWN, constraint[bParameter, xSource])
    assertEquals(UNKNOWN, constraint[bParameter, ySource])
    assertEquals(UNKNOWN, constraint[bParameter, aParameter])
    assertEquals(EQUAL, constraint[bParameter, bParameter])
    assertEquals(UNKNOWN, constraint[bParameter, xDestination])
    assertEquals(UNKNOWN, constraint[bParameter, yDestination])

    assertEquals(UNKNOWN, constraint[xDestination, xSource])
    assertEquals(DISTINCT, constraint[xDestination, ySource])
    assertEquals(UNKNOWN, constraint[xDestination, aParameter])
    assertEquals(UNKNOWN, constraint[xDestination, bParameter])
    assertEquals(EQUAL, constraint[xDestination, xDestination])
    assertEquals(UNKNOWN, constraint[xDestination, yDestination])

    assertEquals(UNKNOWN, constraint[yDestination, xSource])
    assertEquals(UNKNOWN, constraint[yDestination, ySource])
    assertEquals(UNKNOWN, constraint[yDestination, aParameter])
    assertEquals(UNKNOWN, constraint[yDestination, bParameter])
    assertEquals(UNKNOWN, constraint[yDestination, xDestination])
    assertEquals(EQUAL, constraint[yDestination, yDestination])
  }

  @Test
  fun `source projection is correct`() {
    val constraint =
        assembleEmpty(xyIndex, abIndex)
            .also {
              it[xSource, ySource] = EQUAL
              it[aParameter, bParameter] = DISTINCT
              it[xSource, xDestination] = EQUAL
              it[xDestination, yDestination] = DISTINCT
            }
            .toSourceConstraint()

    assertEquals(EQUAL, constraint[xSource, xSource])
    assertEquals(EQUAL, constraint[xSource, ySource])

    assertEquals(EQUAL, constraint[ySource, xSource])
    assertEquals(EQUAL, constraint[ySource, ySource])
  }

  @Test
  fun `guard projection is correct`() {
    val constraint =
        assembleEmpty(xyIndex, abIndex)
            .also {
              it[xSource, ySource] = EQUAL
              it[aParameter, bParameter] = DISTINCT
              it[xSource, xDestination] = EQUAL
              it[xDestination, yDestination] = DISTINCT
            }
            .toGuardConstraint()

    assertEquals(EQUAL, constraint[xSource, xSource])
    assertEquals(EQUAL, constraint[xSource, ySource])
    assertEquals(UNKNOWN, constraint[xSource, aParameter])
    assertEquals(UNKNOWN, constraint[xSource, bParameter])

    assertEquals(EQUAL, constraint[ySource, xSource])
    assertEquals(EQUAL, constraint[ySource, ySource])
    assertEquals(UNKNOWN, constraint[ySource, aParameter])
    assertEquals(UNKNOWN, constraint[ySource, bParameter])

    assertEquals(UNKNOWN, constraint[aParameter, xSource])
    assertEquals(UNKNOWN, constraint[aParameter, ySource])
    assertEquals(EQUAL, constraint[aParameter, aParameter])
    assertEquals(DISTINCT, constraint[aParameter, bParameter])

    assertEquals(UNKNOWN, constraint[bParameter, xSource])
    assertEquals(UNKNOWN, constraint[bParameter, ySource])
    assertEquals(DISTINCT, constraint[bParameter, aParameter])
    assertEquals(EQUAL, constraint[bParameter, bParameter])
  }

  @Test
  fun `assignment projection is correct`() {
    val constraint =
        assembleEmpty(xyIndex, abIndex)
            .also {
              it[xSource, ySource] = EQUAL
              it[aParameter, bParameter] = DISTINCT
              it[xSource, xDestination] = EQUAL
              it[xDestination, yDestination] = DISTINCT
            }
            .toAssignmentConstraint()

    assertEquals(EQUAL, constraint[xSource, xDestination])
    assertEquals(UNKNOWN, constraint[xSource, yDestination])

    assertEquals(UNKNOWN, constraint[ySource, xDestination])
    assertEquals(UNKNOWN, constraint[ySource, yDestination])

    assertEquals(UNKNOWN, constraint[aParameter, xDestination])
    assertEquals(UNKNOWN, constraint[aParameter, yDestination])

    assertEquals(UNKNOWN, constraint[bParameter, xDestination])
    assertEquals(UNKNOWN, constraint[bParameter, yDestination])
  }

  @Test
  fun `destination projection is correct`() {
    val constraint =
        assembleEmpty(xyIndex, abIndex)
            .also {
              it[xSource, ySource] = EQUAL
              it[aParameter, bParameter] = DISTINCT
              it[xSource, xDestination] = EQUAL
              it[xDestination, yDestination] = DISTINCT
            }
            .toDestinationConstraint()

    assertEquals(EQUAL, constraint[xSource, xSource])
    assertEquals(DISTINCT, constraint[xSource, ySource])

    assertEquals(DISTINCT, constraint[ySource, xSource])
    assertEquals(EQUAL, constraint[ySource, ySource])
  }

  @Test
  fun `copying decouples storage`() {
    val constraint = assembleEmpty(xyIndex, abIndex)
    constraint.copyOf()[xSource, aParameter] = EQUAL
    assertEquals(UNKNOWN, constraint[xSource, aParameter])
  }

  @Test
  fun `equal constraints compare equal`() {
    val constraint = assembleEmpty(xyIndex, abIndex).also { it.put(xSource, aParameter, EQUAL) }
    val other = assembleEmpty(xyIndex, abIndex).also { it.put(xSource, aParameter, EQUAL) }
    Assertions.assertTrue(constraint == other)
  }

  @Test
  fun `different-register constraints do not compare equal`() {
    val constraint = assembleEmpty(xyIndex, abIndex).also { it.put(xSource, aParameter, EQUAL) }
    val other = assembleEmpty(xyzIndex, abIndex).also { it.put(xSource, aParameter, EQUAL) }
    Assertions.assertTrue(constraint != other)
  }

  @Test
  fun `different-parameter constraints do not compare equal`() {
    val constraint = assembleEmpty(xyIndex, abIndex).also { it.put(xSource, aParameter, EQUAL) }
    val other = assembleEmpty(xyIndex, abcIndex).also { it.put(xSource, aParameter, EQUAL) }
    Assertions.assertTrue(constraint != other)
  }

  @Test
  fun `different-content constraints do not compare equal`() {
    val constraint = assembleEmpty(xyIndex, abIndex).also { it.put(xSource, bParameter, EQUAL) }
    val other = assembleEmpty(xyIndex, abIndex).also { it.put(aParameter, xSource, EQUAL) }
    Assertions.assertTrue(constraint != other)
  }

  @Test
  fun `equal constraints have same hashCode`() {
    val constraint = assembleEmpty(xyIndex, abIndex).also { it.put(xSource, aParameter, EQUAL) }
    val other = assembleEmpty(xyIndex, abIndex).also { it.put(xSource, aParameter, EQUAL) }
    Assertions.assertTrue(constraint.hashCode() == other.hashCode())
  }
}
