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
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

internal class IterablesTest {

  @Test
  fun `index gives correct result`() {
    val list = listOf("a", "b", "c", "d", "e")
    val reference = mapOf("a" to 0, "b" to 1, "c" to 2, "d" to 3, "e" to 4)
    assertEquals(reference, list.index())
  }

  @Test
  fun `index with offset gives correct result`() {
    val list = listOf("a", "b", "c", "d", "e")
    val reference = mapOf("a" to -2, "b" to -1, "c" to 0, "d" to 1, "e" to 2)
    assertEquals(reference, list.index(-2))
  }

  @Test
  fun `empty index is correct`() {
    assertEquals(emptyMap<String, Int>(), emptyList<String>().index())
  }

  @Test
  fun `empty index with offset is correct`() {
    assertEquals(emptyMap<String, Int>(), emptyList<String>().index(100))
  }

  @ParameterizedTest
  @ValueSource(ints = [0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10])
  fun `subListFrom mirrors subList`(index: Int) {
    val list = (0 until 10).toList()
    assertEquals(list.subList(index, list.size), list.subListFrom(index))
  }

  @ParameterizedTest
  @ValueSource(ints = [0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10])
  fun `subListTo mirrors subList`(index: Int) {
    val list = (0 until 10).toList()
    assertEquals(list.subList(0, index), list.subListTo(index))
  }

  @Test
  fun `combinations gives correct result`() {
    val list = listOf(1, 2, 3, 4, 5)
    val reference =
        listOf(1 to 2, 1 to 3, 1 to 4, 1 to 5, 2 to 3, 2 to 4, 2 to 5, 3 to 4, 3 to 5, 4 to 5)
    assertEquals(reference, list.combinations())
  }

  @Test
  fun `empty list combinations are correct`() {
    assertEquals(emptyList<Pair<String, String>>(), emptyList<String>().combinations())
  }

  @Test
  fun `list product is correct`() {
    assertEquals(
        listOf(1 to "a", 1 to "b", 1 to "c", 2 to "a", 2 to "b", 2 to "c"),
        listOf(1, 2) * listOf("a", "b", "c"))
  }

  @Test
  fun `list times empty is correct`() {
    assertEquals(emptyList<Pair<Int, String>>(), listOf(1, 2) * emptyList<String>())
  }

  @Test
  fun `empty times list is correct`() {
    assertEquals(emptyList<Pair<Int, String>>(), emptyList<Int>() * listOf("a", "b", "c"))
  }

  @Test
  fun `set product is correct`() {
    assertEquals(
        setOf(1 to "a", 1 to "b", 1 to "c", 2 to "a", 2 to "b", 2 to "c"),
        setOf(1, 2) * setOf("a", "b", "c"))
  }

  @Test
  fun `set times empty is correct`() {
    assertEquals(emptySet<Pair<Int, String>>(), setOf(1, 2) * emptySet<String>())
  }

  @Test
  fun `empty times set is correct`() {
    assertEquals(emptySet<Pair<Int, String>>(), emptySet<Int>() * setOf("a", "b", "c"))
  }
}
