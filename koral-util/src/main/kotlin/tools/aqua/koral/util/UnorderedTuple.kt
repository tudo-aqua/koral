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
 * A tuple of two elements that ignores ordering for purposes of equality and hash code computation.
 * For example, the unordered tuples `{0, 1}` and `{1, 0}` are equal. Formally, tuples `{a, b} ==
 * {c, d}` iff `(a == c) && (b == d) || (a == d) && (b == c)`.
 * @param Left the first element type.
 * @param Right the second element type.
 * @param first the first element.
 * @param second the second element.
 */
data class UnorderedTuple<Left, Right>(val first: Left, val second: Right) {
  override fun equals(other: Any?): Boolean =
      when {
        this === other -> true
        other !is UnorderedTuple<*, *> -> false
        else ->
            (first == other.first && second == other.second) ||
                (first == other.second && second == other.first)
      }

  override fun hashCode(): Int = (first?.hashCode() ?: 0) + (second?.hashCode() ?: 0)
}

/** Create an unordered tuple from two objects. */
infix fun <Left, Right> Left.unorderedTo(right: Right): UnorderedTuple<Left, Right> =
    UnorderedTuple(this, right)
