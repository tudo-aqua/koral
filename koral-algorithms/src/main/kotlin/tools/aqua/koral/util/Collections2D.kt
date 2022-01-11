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

interface Collection2D<C, R, T> {
  val columns: Collection<C>
  val rows: Collection<R>

  operator fun get(column: C, row: R): T
  operator fun set(column: C, row: R, value: T)

  fun put(column: C, row: R, value: T): T {
    val previous = this[column, row]
    this[column, row] = value
    return previous
  }

  fun copyOf(): Collection2D<C, R, T>

  data class Entry<C, R, T>(val column: C, val row: R, val value: T)

  fun entrySet(): Set<Entry<C, R, T>> = buildSet {
    columns.forEach { column ->
      rows.forEach { row -> this += Entry(column, row, this@Collection2D[column, row]) }
    }
  }
}

class LabeledArray2D<C, R, T>
internal constructor(
    private val columnIndex: Map<C, Int>,
    private val rowIndex: Map<R, Int>,
    private val array: Array2D<T>,
) : Collection2D<C, R, T> {

  companion object {
    internal inline fun <E, reified T> createTriangular(
        entryIndex: Map<E, Int>,
        diagonalValue: T,
        initialValue: T
    ): LabeledArray2D<E, E, T> =
        LabeledArray2D(
            entryIndex,
            entryIndex,
            TriangularArray2D.create(entryIndex.size, diagonalValue, initialValue))

    internal inline fun <R, C, reified T> createQuadratic(
        columnIndex: Map<C, Int>,
        rowIndex: Map<R, Int>,
        initialValue: T
    ): LabeledArray2D<C, R, T> =
        LabeledArray2D(
            columnIndex,
            rowIndex,
            QuadraticArray2D.create(columnIndex.size, rowIndex.size, initialValue))
  }

  override val columns: Set<C> = columnIndex.keys
  override val rows: Set<R> = rowIndex.keys

  override fun get(column: C, row: R): T =
      array[columnIndex.getValue(column), rowIndex.getValue(row)]

  override fun set(column: C, row: R, value: T) {
    array[columnIndex.getValue(column), rowIndex.getValue(row)] = value
  }

  override fun put(column: C, row: R, value: T): T =
      array.put(columnIndex.getValue(column), rowIndex.getValue(row), value)

  override fun copyOf(): LabeledArray2D<C, R, T> =
      LabeledArray2D(columnIndex, rowIndex, array.copyOf())

  override fun equals(other: Any?): Boolean =
      when {
        this === other -> true
        other !is LabeledArray2D<*, *, *> -> false
        else ->
            columnIndex == other.columnIndex && rowIndex == other.rowIndex && array == other.array
      }

  override fun hashCode(): Int =
      array.hashCode() // usually compared to copies, so elide indices in hashcode
}

class FlippedCollection2D<C, R, T>(private val inner: Collection2D<R, C, T>) :
    Collection2D<C, R, T> {

  override val columns: Collection<C> by lazy { inner.rows }
  override val rows: Collection<R> by lazy { inner.columns }

  override fun get(column: C, row: R): T = inner[row, column]

  override fun set(column: C, row: R, value: T) {
    inner[row, column] = value
  }

  override fun put(column: C, row: R, value: T): T = inner.put(row, column, value)

  override fun copyOf(): FlippedCollection2D<C, R, T> = FlippedCollection2D(inner.copyOf())

  override fun equals(other: Any?): Boolean =
      when {
        this === other -> true
        other !is FlippedCollection2D<*, *, *> -> false
        else -> inner == other.inner
      }

  override fun hashCode(): Int = 31 * inner.hashCode()
}

fun <C, R, T> Collection2D<C, R, T>.flip(): FlippedCollection2D<R, C, T> = FlippedCollection2D(this)
