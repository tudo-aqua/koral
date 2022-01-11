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

internal class QuadraticArray2DTest {

  @Test
  fun `reported labels are correct`() {
    val array = QuadraticArray2D.create(10, 20, "")
    assertEquals((0..9).toList(), array.columns)
    assertEquals((0..19).toList(), array.rows)
  }

  @Test
  fun `all quadratic cells initialized to initial value`() {
    val array = QuadraticArray2D.create(2, 2, "i")
    assertEquals("i", array[0, 0])
    assertEquals("i", array[0, 1])
    assertEquals("i", array[1, 0])
    assertEquals("i", array[1, 1])
  }

  @Test
  fun `written values are read`() {
    val array = QuadraticArray2D.create(2, 2, "i")
    array[1, 0] = "x"
    assertEquals("x", array[1, 0])
  }

  @Test
  fun `written values are not read on inverted indices`() {
    val array = QuadraticArray2D.create(2, 2, "i").also { it[1, 0] = "x" }
    assertNotEquals("x", array[0, 1])
  }

  @Test
  fun `put reads correct value`() {
    val array = QuadraticArray2D.create(2, 2, "i")
    assertEquals("i", array.put(0, 1, "x"))
  }

  @Test
  fun `put writes correct value`() {
    val array = QuadraticArray2D.create(2, 2, "i").also { it.put(1, 0, "x") }
    assertEquals("x", array[1, 0])
  }

  @Test
  fun `copying yields identical content`() {
    val array = QuadraticArray2D.create(2, 2, "i").also { it[1, 0] = "x" }.copyOf()

    assertEquals("i", array[0, 0])
    assertEquals("i", array[0, 1])
    assertEquals("x", array[1, 0])
    assertEquals("i", array[1, 1])
  }

  @Test
  fun `copying decouples storage`() {
    val array = QuadraticArray2D.create(2, 2, "i")
    array.copyOf()[1, 0] = "x"
    assertEquals("i", array[1, 0])
  }

  @Test
  fun `equal arrays compare equal`() {
    val array = QuadraticArray2D.create(2, 2, "i").also { it[1, 0] = "x" }
    val other = QuadraticArray2D.create(2, 2, "i").also { it[1, 0] = "x" }
    assertTrue(array == other)
  }

  @Test
  fun `different-column arrays do not compare equal`() {
    val array = QuadraticArray2D.create(2, 2, "i").also { it[1, 0] = "x" }
    val other = QuadraticArray2D.create(3, 2, "i").also { it[1, 0] = "x" }
    assertTrue(array != other)
  }

  @Test
  fun `different-row arrays do not compare equal`() {
    val array = QuadraticArray2D.create(2, 2, "i").also { it[1, 0] = "x" }
    val other = QuadraticArray2D.create(2, 3, "i").also { it[1, 0] = "x" }
    assertTrue(array != other)
  }

  @Test
  fun `different-content arrays do not compare equal`() {
    val array = QuadraticArray2D.create(2, 2, "i").also { it[1, 0] = "x" }
    val other = QuadraticArray2D.create(2, 2, "i").also { it[1, 0] = "not-x" }
    assertTrue(array != other)
  }

  @Test
  fun `equal arrays have same hashCode`() {
    val array = QuadraticArray2D.create(2, 2, "i").also { it[1, 0] = "x" }
    val other = QuadraticArray2D.create(2, 2, "i").also { it[1, 0] = "x" }
    assertTrue(array.hashCode() == other.hashCode())
  }
}
