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

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import tools.aqua.koral.util.Relation.*

internal class CloseTransitiveTest {

  @Test
  fun `successful closure yields correct values`() {
    val constraint =
        LocationConstraint(xyzIndex).also {
          it[xSource, ySource] = EQUAL
          it[ySource, zSource] = DISTINCT
          it.closeTransitive()
        }

    assertEquals(EQUAL, constraint[xSource, xSource])
    assertEquals(EQUAL, constraint[xSource, ySource])
    assertEquals(DISTINCT, constraint[xSource, zSource])

    assertEquals(EQUAL, constraint[ySource, xSource])
    assertEquals(EQUAL, constraint[ySource, ySource])
    assertEquals(DISTINCT, constraint[ySource, zSource])

    assertEquals(DISTINCT, constraint[zSource, xSource])
    assertEquals(DISTINCT, constraint[zSource, ySource])
    assertEquals(EQUAL, constraint[zSource, zSource])
  }

  @Test
  fun `closure detects transitive infeasibility`() {
    val constraint =
        LocationConstraint(xyzIndex).also {
          it[xSource, ySource] = EQUAL
          it[xSource, zSource] = EQUAL
          it[ySource, zSource] = DISTINCT
        }

    assertThrows<InfeasibleConstraintException> { constraint.closeTransitive() }
  }
}
