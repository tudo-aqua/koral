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

internal class MutableCanonicalSetTest {

  @Test
  fun `size is correct`() {
    val set = MutableCanonicalSet<Int>().also { it += 1 }
    assertEquals(1, set.size)
  }

  @Test
  fun `size ignores duplicates`() {
    val set =
        MutableCanonicalSet<Int>().also {
          it += 1
          it += 1
        }
    assertEquals(1, set.size)
  }

  @Test
  fun `re-interning yields original object`() {
    val canonical = listOf(1)
    val set = MutableCanonicalSet<List<Int>>().also { it.intern(canonical) }
    assertSame(canonical, set.intern(listOf(1)))
  }

  @Test
  fun `interning adds to set`() {
    val set = MutableCanonicalSet<Int>().also { it.intern(1) }
    assertEquals(setOf(1), set)
  }

  @Test
  fun `adding interns`() {
    val canonical = listOf(1)
    val set = MutableCanonicalSet<List<Int>>().also { it.add(canonical) }
    assertSame(canonical, set.intern(listOf(1)))
  }

  @Test
  fun `adding works`() {
    val set = MutableCanonicalSet<Int>().also { it.add(1) }
    assertEquals(setOf(1), set)
  }

  @Test
  fun `clearing works`() {
    val set =
        MutableCanonicalSet<Int>().also {
          it += 1
          it.clear()
        }
    assertEquals(emptySet<Int>(), set)
  }

  @Test
  fun `contains works`() {
    val set = MutableCanonicalSet<Int>().also { it += 1 }
    assertTrue(set.contains(1))
    assertFalse(set.contains(2))
  }

  @Test
  fun `contains all works`() {
    val set =
        MutableCanonicalSet<Int>().also {
          it += 1
          it += 2
        }
    assertTrue(set.containsAll(listOf(1)))
    assertTrue(set.containsAll(listOf(1, 2)))
    assertFalse(set.containsAll(listOf(1, 2, 3)))
  }

  @Test
  fun `iterator works`() {
    val iterator =
        MutableCanonicalSet<Int>()
            .also {
              it += 1
              it += 2
            }
            .iterator()
    val first = iterator.next()
    val second = iterator.next()
    assertEquals(setOf(1, 2), setOf(first, second))
    assertFalse(iterator.hasNext())
  }

  @Test
  fun `remove works`() {
    val set =
        MutableCanonicalSet<Int>().also {
          it += 1
          it += 2
          it.remove(1)
        }
    assertEquals(setOf(2), set)
  }

  @Test
  fun `remove ignores missing`() {
    val set =
        MutableCanonicalSet<Int>().also {
          it += 1
          it += 2
        }
    assertDoesNotThrow { set.remove(3) }
    assertEquals(setOf(1, 2), set)
  }

  @Test
  fun `retainAll works`() {
    val set =
        MutableCanonicalSet<Int>().also {
          it += 1
          it += 2
          it += 3
          it.retainAll(listOf(1, 3))
        }
    assertEquals(setOf(1, 3), set)
  }

  @Test
  fun `retainAll ignores missing`() {
    val set =
        MutableCanonicalSet<Int>().also {
          it += 1
          it += 2
          it.retainAll(listOf(1, 3))
        }
    assertEquals(setOf(1), set)
  }
}
