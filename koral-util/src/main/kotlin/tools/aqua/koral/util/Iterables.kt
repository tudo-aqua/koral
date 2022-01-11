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

/**
 * Create an index for this list, i.e., a mapping from element to the element's index in the list.
 * E.g., the list ["a", "b", "c"] would yield the index {"a": 0, "b": 1, "c": 2}. In general,
 * `l.index()[e] == i + `[offset] iff `l[i] == e`. All list elements must me unique.
 *
 * @param offset a value to add to each base index value.
 * @return the index map.
 */
fun <T> Iterable<T>.index(offset: Int = 0): Map<T, Int> =
    withIndex().associate { (index, item) -> item to index + offset }

/**
 * Get this list's [List.subList] from a given index (inclusive) to the last element.
 *
 * @param E the element type.
 * @param fromIndex the first index present in the sublist.
 * @return the sublist.
 */
fun <E> List<E>.subListFrom(fromIndex: Int): List<E> = subList(fromIndex, size)

/**
 * Get this list's [List.subList] from the first element to a given index (exclusive).
 *
 * @param E the element type.
 * @param toIndex the last index present in the sublist, plus 1.
 * @return the sublist.
 */
fun <E> List<E>.subListTo(toIndex: Int): List<E> = subList(0, toIndex)

/**
 * Get all combinations of this list's elements *without* reflexive entries.
 *
 * Given a list `[1, 2, … k]`, this returns the list `[(1, 2), (1, 3), …, (1, k), (2, 3), …, (2, k),
 * (3, 4), …, (k-1, k)]`.
 *
 * @param E the element type.
 * @return the combinations of list elements.
 */
fun <E> List<E>.combinations(): List<Pair<E, E>> =
    if (isEmpty()) emptyList()
    else {
      subListTo(size - 1).flatMapIndexed { index, left ->
        subListFrom(index + 1).map { right -> left to right }
      }
    }

operator fun <E, F> Iterable<E>.times(other: Iterable<F>): List<Pair<E, F>> =
    flatMapTo(mutableListOf()) { left -> other.mapTo(mutableListOf()) { right -> left to right } }

operator fun <E, F> Set<E>.times(other: Set<F>): Set<Pair<E, F>> =
    flatMapTo(mutableSetOf()) { left -> other.mapTo(mutableSetOf()) { right -> left to right } }

fun <T> MutableCollection<T>.removeFirst(): T = first().also { this -= it }
