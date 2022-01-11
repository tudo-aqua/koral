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

import com.google.common.collect.Interner

/**
 * A [MutableSet] with canonical value retrieval. This adds a [get] (and [getValue]) method similar
 * to [Map]s that retrieves a canonical value that is [equal](Object.equals) to the parameter if any
 * is already stored in this object. Since this realizes the [Interner] pattern, this class also
 * implements the [Interner] interface.
 *
 * Note that this also permits the removal of objects from the set. If this happens, the canonical
 * value is completely removed.
 *
 * @param E the elements in this set.
 */
class MutableCanonicalSet<E> : AbstractMutableSet<E>(), Interner<E> {
  /** Value storage. */
  private val map = HashMap<E, E>()
  override val size: Int
    get() = map.size

  override fun intern(element: E): E = map.computeIfAbsent(element) { element }

  override fun add(element: E): Boolean = map.put(element, element) == null

  override fun clear() {
    map.clear()
  }

  override fun contains(element: E): Boolean = element in map

  override fun containsAll(elements: Collection<E>): Boolean = map.keys.containsAll(elements)

  /**
   * Get the canonical value for this object.
   * @param element the object to look up.
   * @return if an object `o` s.t. `o == `[element] is present in this set, `o`, else, `null`
   */
  operator fun get(element: E): E? = map[element]

  /**
   * Get the canonical value for this object and throw an exception if it is missing.
   * @param element the object to look up.
   * @return an object `o` s.t. `o == `[element] is present in this set.
   * @throws NoSuchElementException if no equal object is present in this set.
   */
  fun getValue(element: E): E = map.getValue(element)

  override fun iterator(): MutableIterator<E> = map.keys.iterator()

  override fun remove(element: E): Boolean = map.remove(element) != null

  override fun retainAll(elements: Collection<E>): Boolean = map.keys.retainAll(elements.toSet())
}

/**
 * [intern] a given element and report if it was already present in this interner. This basically
 * combined the semantics of [MutableCollection.add] and [Interner.intern].
 * @receiver the interner that the element is interned with.
 * @param element the element to intern.
 * @return a tuple `(b, e)`, with `b == true` iff the [element] was *not* present already (i.e.,
 * [element] is the new canonical element in the interner) and `e` being the canonical element
 * returned by [intern].
 */
fun <E> Interner<E>.addIntern(element: E): Pair<Boolean, E> {
  val interned = intern(element)
  return (interned === element) to interned
}
