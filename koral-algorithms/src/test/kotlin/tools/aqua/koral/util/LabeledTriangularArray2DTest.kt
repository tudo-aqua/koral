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

internal class LabeledTriangularArray2DTest {

  @Test
  fun `reported labels are correct`() {
    val array = LabeledArray2D.createTriangular(abIndex, "", "")
    assertEquals(setOf("a", "b"), array.columns)
    assertEquals(setOf("a", "b"), array.rows)
  }

  @Test
  fun `all non-diagonal cells initialized to initial value`() {
    val array = LabeledArray2D.createTriangular(abIndex, "d", "i")
    assertEquals("i", array["a", "b"])
    assertEquals("i", array["b", "a"])
  }

  @Test
  fun `all diagonal cells initialized to diagonal value`() {
    val array = LabeledArray2D.createTriangular(abIndex, "d", "i")
    assertEquals("d", array["a", "a"])
    assertEquals("d", array["b", "b"])
  }

  @Test
  fun `written values are read`() {
    val array = LabeledArray2D.createTriangular(abIndex, "d", "i").also { it["a", "b"] = "v" }
    assertEquals("v", array["a", "b"])
  }

  @Test
  fun `written values are read on inverted indices`() {
    val array = LabeledArray2D.createTriangular(abIndex, "d", "i").also { it["a", "b"] = "v" }
    assertEquals("v", array["b", "a"])
  }

  @Test
  fun `put reads correct value`() {
    val array = LabeledArray2D.createTriangular(abIndex, "d", "i")
    assertEquals("i", array.put("a", "b", "v"))
  }

  @Test
  fun `put writes correct value`() {
    val array = LabeledArray2D.createTriangular(abIndex, "d", "i").also { it.put("a", "b", "v") }
    assertEquals("v", array["a", "b"])
  }

  @Test
  fun `copying yields identical content`() {
    val array =
        LabeledArray2D.createTriangular(abcIndex, "d", "i").also { it["a", "c"] = "v" }.copyOf()

    assertEquals("d", array["a", "a"])
    assertEquals("i", array["a", "b"])
    assertEquals("v", array["a", "c"])
    assertEquals("i", array["b", "a"])
    assertEquals("d", array["b", "b"])
    assertEquals("i", array["b", "c"])
    assertEquals("v", array["c", "a"])
    assertEquals("i", array["c", "b"])
    assertEquals("d", array["c", "c"])
  }

  @Test
  fun `copying decouples storage`() {
    val array = LabeledArray2D.createTriangular(abIndex, "d", "i")
    array.copyOf()["a", "b"] = "v"
    assertEquals("i", array["a", "b"])
  }

  @Test
  fun `equal arrays compare equal`() {
    val array = LabeledArray2D.createTriangular(abcIndex, "d", "i").also { it["a", "b"] = "v" }
    val other = LabeledArray2D.createTriangular(abcIndex, "d", "i").also { it["a", "b"] = "v" }
    assertTrue(array == other)
  }

  @Test
  fun `different-index arrays do not compare equal`() {
    val array = LabeledArray2D.createTriangular(abIndex, "d", "i").also { it["a", "b"] = "v" }
    val other = LabeledArray2D.createTriangular(abcIndex, "d", "i").also { it["a", "b"] = "v" }
    assertTrue(array != other)
  }

  @Test
  fun `different-diagnoal arrays do not compare equal`() {
    val array = LabeledArray2D.createTriangular(abIndex, "d", "i").also { it["a", "b"] = "v" }
    val other = LabeledArray2D.createTriangular(abIndex, "not-d", "i").also { it["a", "b"] = "v" }
    assertTrue(array != other)
  }

  @Test
  fun `different-content arrays do not compare equal`() {
    val array = LabeledArray2D.createTriangular(abIndex, "d", "i").also { it["a", "b"] = "v" }
    val other = LabeledArray2D.createTriangular(abIndex, "d", "i").also { it["a", "b"] = "not-v" }
    assertTrue(array != other)
  }

  @Test
  fun `equal arrays have same hashCode`() {
    val array = LabeledArray2D.createTriangular(abIndex, "d", "i").also { it["a", "b"] = "v" }
    val other = LabeledArray2D.createTriangular(abIndex, "d", "i").also { it["a", "b"] = "v" }
    assertTrue(array.hashCode() == other.hashCode())
  }
}
