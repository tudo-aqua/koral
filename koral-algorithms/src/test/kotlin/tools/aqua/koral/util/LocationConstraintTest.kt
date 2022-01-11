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

internal class LocationConstraintTest {

  @Test
  fun `reported labels are correct`() {
    val constraint = LocationConstraint(xyIndex)
    assertEquals(xySourceSet, constraint.columns.toSet())
    assertEquals(xySourceSet, constraint.rows.toSet())
  }

  @Test
  fun `reported variables are correct`() {
    val constraint = LocationConstraint(xyIndex)
    assertEquals(xyIndex, constraint.registers)
  }

  @Test
  fun `all cells initialized correctly`() {
    val constraint = LocationConstraint(xyIndex)

    assertEquals(EQUAL, constraint[xSource, xSource])
    assertEquals(UNKNOWN, constraint[xSource, ySource])
    assertEquals(UNKNOWN, constraint[ySource, xSource])
    assertEquals(EQUAL, constraint[ySource, ySource])
  }

  @Test
  fun `written relations are read`() {
    val constraint = LocationConstraint(xyIndex).also { it[xSource, ySource] = EQUAL }
    assertEquals(EQUAL, constraint[xSource, ySource])
  }

  @Test
  fun `written relations are read on inverted indices`() {
    val constraint = LocationConstraint(xyIndex).also { it[xSource, ySource] = EQUAL }
    assertEquals(EQUAL, constraint[ySource, xSource])
  }

  @Test
  fun `put reads correct value`() {
    val constraint = LocationConstraint(xyIndex)
    assertEquals(UNKNOWN, constraint.put(xSource, ySource, EQUAL))
  }

  @Test
  fun `put writes correct value`() {
    val constraint = LocationConstraint(xyIndex).also { it.put(xSource, ySource, EQUAL) }
    assertEquals(EQUAL, constraint[xSource, ySource])
  }

  @Test
  fun `copying yields identical content`() {
    val constraint =
        LocationConstraint(xyzIndex)
            .also {
              it[xSource, ySource] = EQUAL
              it[zSource, ySource] = DISTINCT
            }
            .copyOf()

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
  fun `copying decouples storage`() {
    val constraint = LocationConstraint(xyIndex)
    constraint.copyOf()[xSource, ySource] = EQUAL
    assertEquals(UNKNOWN, constraint[xSource, ySource])
  }

  @Test
  fun `equal constraints compare equal`() {
    val constraint = LocationConstraint(xyIndex).also { it.put(xSource, ySource, EQUAL) }
    val other = LocationConstraint(xyIndex).also { it.put(xSource, ySource, EQUAL) }
    Assertions.assertTrue(constraint == other)
  }

  @Test
  fun `different-register constraints do not compare equal`() {
    val constraint = LocationConstraint(xyIndex).also { it.put(xSource, ySource, EQUAL) }
    val other = LocationConstraint(xyzIndex).also { it.put(xSource, ySource, EQUAL) }
    Assertions.assertTrue(constraint != other)
  }

  @Test
  fun `different-content constraints do not compare equal`() {
    val constraint = LocationConstraint(xyzIndex).also { it.put(xSource, ySource, EQUAL) }
    val other = LocationConstraint(xyzIndex).also { it.put(zSource, xSource, DISTINCT) }
    Assertions.assertTrue(constraint != other)
  }

  @Test
  fun `equal constraints have same hashCode`() {
    val constraint = LocationConstraint(xyIndex).also { it.put(xSource, ySource, EQUAL) }
    val other = LocationConstraint(xyIndex).also { it.put(xSource, ySource, EQUAL) }
    Assertions.assertTrue(constraint.hashCode() == other.hashCode())
  }
}
