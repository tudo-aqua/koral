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

internal class LabeledQuadraticArray2DTest {

  @Test
  fun `reported labels are correct`() {
    val array = LabeledArray2D.createQuadratic(abcIndex, abIndex, "")
    assertEquals(setOf("a", "b", "c"), array.columns)
    assertEquals(setOf("a", "b"), array.rows)
  }

  @Test
  fun `all cells initialized to initial value`() {
    val array = LabeledArray2D.createQuadratic(abIndex, xyIndex, "i")
    assertEquals("i", array["a", "x"])
    assertEquals("i", array["a", "y"])
    assertEquals("i", array["b", "x"])
    assertEquals("i", array["b", "y"])
  }

  @Test
  fun `written values are read`() {
    val array = LabeledArray2D.createQuadratic(abIndex, xyIndex, "i").also { it["a", "x"] = "v" }
    assertEquals("v", array["a", "x"])
  }

  @Test
  fun `written values are not read on inverted indices`() {
    val array = LabeledArray2D.createQuadratic(abIndex, abIndex, "i").also { it["a", "b"] = "v" }
    assertNotEquals("v", array["b", "a"])
  }

  @Test
  fun `put reads correct value`() {
    val array = LabeledArray2D.createQuadratic(abIndex, xyIndex, "i")
    assertEquals("i", array.put("a", "y", "v"))
  }

  @Test
  fun `put writes correct value`() {
    val array = LabeledArray2D.createQuadratic(abIndex, xyIndex, "i").also { it.put("a", "y", "v") }
    assertEquals("v", array["a", "y"])
  }

  @Test
  fun `copying yields identical content`() {
    val array =
        LabeledArray2D.createQuadratic(abIndex, xyIndex, "i").also { it["a", "y"] = "v" }.copyOf()

    assertEquals("i", array["a", "x"])
    assertEquals("v", array["a", "y"])
    assertEquals("i", array["b", "x"])
    assertEquals("i", array["b", "y"])
  }

  @Test
  fun `copying decouples storage`() {
    val array = LabeledArray2D.createQuadratic(abIndex, xyIndex, "i")
    array.copyOf()["a", "y"] = "v"
    assertEquals("i", array["a", "y"])
  }

  @Test
  fun `equal arrays compare equal`() {
    val array = LabeledArray2D.createQuadratic(abIndex, abcIndex, "i").also { it["a", "b"] = "v" }
    val other = LabeledArray2D.createQuadratic(abIndex, abcIndex, "i").also { it["a", "b"] = "v" }
    assertTrue(array == other)
  }

  @Test
  fun `different-column arrays do not compare equal`() {
    val array = LabeledArray2D.createQuadratic(abIndex, abIndex, "i").also { it["a", "b"] = "v" }
    val other = LabeledArray2D.createQuadratic(abcIndex, abIndex, "i").also { it["a", "b"] = "v" }
    assertTrue(array != other)
  }

  @Test
  fun `different-row arrays do not compare equal`() {
    val array = LabeledArray2D.createQuadratic(abIndex, abIndex, "i").also { it["a", "b"] = "v" }
    val other = LabeledArray2D.createQuadratic(abIndex, abcIndex, "i").also { it["a", "b"] = "v" }
    assertTrue(array != other)
  }

  @Test
  fun `different-content arrays do not compare equal`() {
    val array = LabeledArray2D.createQuadratic(abIndex, abIndex, "i").also { it["a", "b"] = "v" }
    val other =
        LabeledArray2D.createQuadratic(abcIndex, abIndex, "i").also { it["a", "b"] = "not-v" }
    assertTrue(array != other)
  }

  @Test
  fun `equal arrays have same hashCode`() {
    val array = LabeledArray2D.createQuadratic(abIndex, abcIndex, "i").also { it["a", "b"] = "v" }
    val other = LabeledArray2D.createQuadratic(abIndex, abcIndex, "i").also { it["a", "b"] = "v" }
    assertTrue(array.hashCode() == other.hashCode())
  }
}
