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

interface Array2D<T> : Collection2D<Int, Int, T> {
  override fun copyOf(): Array2D<T>
}

class TriangularArray2D<T>
internal constructor(
    private val nColumnsAndRows: Int,
    private val diagonalValue: T,
    private val matrix: Array<T>
) : Array2D<T> {

  companion object {
    internal inline fun <reified T> create(
        size: Int,
        diagonalValue: T,
        initialValue: T
    ): TriangularArray2D<T> {
      return TriangularArray2D(size, diagonalValue, Array(matrixSize(size)) { initialValue })
    }

    private fun matrixSize(edgeSize: Int): Int = (edgeSize * edgeSize - edgeSize) / 2
    private fun matrixIndex(column: Int, row: Int): Int =
        if (row < column) matrixIndex(row, column)
        else row * (row - 1) / 2 + column // gauss sum to (row - 1)

    private operator fun <T> Array<T>.get(column: Int, row: Int): T = this[matrixIndex(row, column)]
    private operator fun <T> Array<T>.set(column: Int, row: Int, value: T) {
      this[matrixIndex(row, column)] = value
    }
  }

  private val columnRowRange = 0 until nColumnsAndRows
  override val columns: List<Int> by lazy { columnRowRange.toList() }
  override val rows: List<Int> by lazy { columns }

  override fun get(column: Int, row: Int): T {
    assert(column in columnRowRange)
    assert(row in columnRowRange)

    return if (column == row) diagonalValue else matrix[column, row]
  }

  override fun set(column: Int, row: Int, value: T) {
    assert(column in columnRowRange)
    assert(row in columnRowRange)
    require((column == row) implies (value == diagonalValue)) // may be checked at runtime

    matrix[column, row] = value
  }

  override fun copyOf(): TriangularArray2D<T> =
      TriangularArray2D(nColumnsAndRows, diagonalValue, matrix.copyOf())

  override fun equals(other: Any?): Boolean =
      when {
        this === other -> true
        other !is TriangularArray2D<*> -> false
        else ->
            nColumnsAndRows == other.nColumnsAndRows &&
                diagonalValue == other.diagonalValue &&
                matrix.contentEquals(other.matrix)
      }

  override fun hashCode(): Int =
      ((nColumnsAndRows * 31) + diagonalValue.hashCode()) * 31 + matrix.contentHashCode()
}

class QuadraticArray2D<T>
internal constructor(
    private val nColumns: Int,
    private val nRows: Int,
    private val matrix: Array<T>
) : Array2D<T> {

  companion object {
    internal inline fun <reified T> create(
        columns: Int,
        rows: Int,
        initialValue: T
    ): QuadraticArray2D<T> {
      return QuadraticArray2D(columns, rows, Array(matrixSize(columns, rows)) { initialValue })
    }

    private fun matrixSize(columns: Int, rows: Int): Int = rows * columns
  }

  private val columnRange = 0 until nColumns
  private val rowRange = 0 until nRows
  override val columns: List<Int> by lazy { columnRange.toList() }
  override val rows: List<Int> by lazy { rowRange.toList() }

  private fun matrixIndex(column: Int, row: Int): Int = row * nColumns + column

  private operator fun <T> Array<T>.get(column: Int, row: Int): T = this[matrixIndex(column, row)]
  private operator fun <T> Array<T>.set(column: Int, row: Int, value: T) {
    this[matrixIndex(column, row)] = value
  }

  override fun get(column: Int, row: Int): T {
    require(column in columnRange)
    require(row in rowRange)

    return matrix[column, row]
  }

  override fun set(column: Int, row: Int, value: T) {
    require(column in columnRange)
    require(row in rowRange)

    matrix[column, row] = value
  }

  override fun copyOf(): QuadraticArray2D<T> = QuadraticArray2D(nColumns, nRows, matrix.copyOf())

  override fun equals(other: Any?): Boolean =
      when {
        this === other -> true
        other !is QuadraticArray2D<*> -> false
        else -> columns == other.columns && rows == other.rows && matrix.contentEquals(other.matrix)
      }

  override fun hashCode(): Int = ((nRows * 31) + nColumns) * 31 + matrix.contentHashCode()
}
