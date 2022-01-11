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

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

internal class FlippedCollection2DTest {

  @Test
  fun `labels are flipped`() {
    val collection =
        LabeledArray2D.createQuadratic(listOf("a", "b", "c").index(), listOf("a", "b").index(), "")
            .flip()

    assertEquals(setOf("a", "b"), collection.columns)
    assertEquals(setOf("a", "b", "c"), collection.rows)
  }

  @Test
  fun `contents are flipped`() {
    val collection =
        QuadraticArray2D.create(2, 2, "i")
            .also {
              it[0, 0] = "a"
              it[0, 1] = "b"
              it[1, 0] = "c"
              it[1, 1] = "d"
            }
            .flip()

    assertEquals("a", collection[0, 0])
    assertEquals("c", collection[0, 1])
    assertEquals("b", collection[1, 0])
    assertEquals("d", collection[1, 1])
  }

  @Test
  fun `writes are flipped`() {
    val array = QuadraticArray2D.create(2, 2, "i")
    val collection = array.flip()
    collection[0, 1] = "x"
    assertEquals("x", array[1, 0])
  }

  @Test
  fun `put reads flipped`() {
    val array = QuadraticArray2D.create(2, 2, "i").also { it[1, 0] = "x" }
    assertEquals(array.flip().put(0, 1, "y"), "x")
  }

  @Test
  fun `put writes flipped`() {
    val array = QuadraticArray2D.create(2, 2, "i")
    val collection = array.flip()
    collection.put(0, 1, "x")
    assertEquals("x", array[1, 0])
  }

  @Test
  fun `equal wrapped collections compare equal`() {
    val array = TriangularArray2D.create(2, "d", "i")
    assertTrue(array.flip() == array.flip())
  }

  @Test
  fun `non-equal wrapped collections compare equal`() {
    val array1 = TriangularArray2D.create(2, "d", "i")
    val array2 = TriangularArray2D.create(2, "not-d", "not-i")
    assertTrue(array1.flip() != array2.flip())
  }

  @Test
  fun `equal wrapped collections have equal hash code`() {
    val array = TriangularArray2D.create(2, "d", "i")
    assertTrue(array.flip().hashCode() == array.flip().hashCode())
  }
}
