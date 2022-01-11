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
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows

internal class TriangularArray2DTest {

  @Test
  fun `reported labels are correct`() {
    val array = TriangularArray2D.create(10, "", "")
    assertEquals((0..9).toList(), array.columns)
    assertEquals((0..9).toList(), array.rows)
  }

  @Test
  fun `all non-diagonal cells initialized to initial value`() {
    val array = TriangularArray2D.create(2, "d", "i")
    assertEquals("i", array[0, 1])
    assertEquals("i", array[1, 0])
  }

  @Test
  fun `all diagonal cells initialized to diagonal value`() {
    val array = TriangularArray2D.create(2, "d", "i")
    assertEquals("d", array[0, 0])
    assertEquals("d", array[1, 1])
  }

  @Test
  fun `written values are read`() {
    val array = TriangularArray2D.create(2, "d", "i").also { it[1, 0] = "x" }
    assertEquals("x", array[1, 0])
  }

  @Test
  fun `written values are read on inverted indices`() {
    val array = TriangularArray2D.create(2, "d", "i").also { it[1, 0] = "x" }
    assertEquals("x", array[0, 1])
  }

  @Test
  fun `diagonal value can be written to diagonal`() {
    val array = TriangularArray2D.create(2, "d", "i")
    assertDoesNotThrow { array[0, 0] = "d" }
  }

  @Test
  fun `non-diagonal value can not be written to diagonal`() {
    val array = TriangularArray2D.create(2, "d", "i")
    assertThrows<IllegalArgumentException> { array[0, 0] = "x" }
  }

  @Test
  fun `put reads correct value`() {
    val array = TriangularArray2D.create(2, "d", "i")
    assertEquals("i", array.put(0, 1, "x"))
  }

  @Test
  fun `put writes correct value`() {
    val array = TriangularArray2D.create(2, "d", "i").also { it.put(1, 0, "x") }
    assertEquals("x", array[1, 0])
  }

  @Test
  fun `copying yields identical content`() {
    val array = TriangularArray2D.create(3, "d", "i").also { it[2, 0] = "x" }.copyOf()

    assertEquals("d", array[0, 0])
    assertEquals("i", array[0, 1])
    assertEquals("x", array[0, 2])
    assertEquals("i", array[1, 0])
    assertEquals("d", array[1, 1])
    assertEquals("i", array[1, 2])
    assertEquals("x", array[2, 0])
    assertEquals("i", array[2, 1])
    assertEquals("d", array[2, 2])
  }

  @Test
  fun `copying decouples storage`() {
    val array = TriangularArray2D.create(2, "d", "i")
    array.copyOf()[1, 0] = "x"
    assertEquals("i", array[1, 0])
  }

  @Test
  fun `equal arrays compare equal`() {
    val array = TriangularArray2D.create(3, "d", "i").also { it[2, 0] = "x" }
    val other = TriangularArray2D.create(3, "d", "i").also { it[2, 0] = "x" }
    assertTrue(array == other)
  }

  @Test
  fun `different-size arrays do not compare equal`() {
    val array = TriangularArray2D.create(3, "d", "i").also { it[2, 0] = "x" }
    val other = TriangularArray2D.create(4, "d", "i").also { it[2, 0] = "x" }
    assertTrue(array != other)
  }

  @Test
  fun `different-diagonal arrays do not compare equal`() {
    val array = TriangularArray2D.create(3, "d", "i").also { it[2, 0] = "x" }
    val other = TriangularArray2D.create(3, "not-d", "i").also { it[2, 0] = "x" }
    assertTrue(array != other)
  }

  @Test
  fun `different-non-diagonal arrays do not compare equal`() {
    val array = TriangularArray2D.create(3, "d", "i").also { it[2, 0] = "x" }
    val other = TriangularArray2D.create(3, "d", "i").also { it[2, 0] = "not-x" }
    assertTrue(array != other)
  }

  @Test
  fun `equal arrays have same hashCode`() {
    val array = TriangularArray2D.create(3, "d", "i").also { it[2, 0] = "x" }
    val other = TriangularArray2D.create(3, "d", "i").also { it[2, 0] = "x" }
    assertTrue(array.hashCode() == other.hashCode())
  }
}
