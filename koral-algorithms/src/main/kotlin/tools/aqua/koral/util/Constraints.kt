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

import tools.aqua.koral.util.Relation.EQUAL
import tools.aqua.koral.util.Relation.UNKNOWN
import tools.aqua.koral.util.Tag.*

class InfeasibleConstraintException : RuntimeException()

/** The relations that can be stored in the matrix. */
enum class Relation {
  /** The indexed values are equal. */
  EQUAL,
  /** The indexed values are distinct. */
  DISTINCT,
  /** The relation between the indexed values is unknown / dont-care. */
  UNKNOWN,
  ;

  override fun toString(): String =
      when (this) {
        EQUAL -> "=="
        DISTINCT -> "<>"
        UNKNOWN -> "??"
      }
}

/** Disambiguates register and parameter names. */
enum class Tag {
  /** A source register for a computation. */
  SOURCE_REGISTER,
  /** An input symbol's parameter. */
  PARAMETER,
  /** A destination register for a computation. */
  DESTINATION_REGISTER,
  ;

  /** Tag a variable name. */
  operator fun <T> invoke(tagged: T): Tagged<T> = Tagged(this, tagged)
}

/**
 * A [Tag]ged variable name.
 * @param Variable the variable type.
 * @param tag the tag.
 * @param variable the variable name.
 */
data class Tagged<Variable>(val tag: Tag, val variable: Variable) {
  override fun toString(): String =
      when (tag) {
        SOURCE_REGISTER -> "s_$variable"
        PARAMETER -> "p_$variable"
        DESTINATION_REGISTER -> "d_$variable"
      }
}

abstract class AbstractConstraint<Variable> :
    Collection2D<Tagged<Variable>, Tagged<Variable>, Relation> {

  protected companion object {
    fun <E> createTriangularRelationStore(index: Map<E, Int>): LabeledArray2D<E, E, Relation> =
        LabeledArray2D.createTriangular(index, EQUAL, UNKNOWN)

    fun <C, R> createQuadraticRelationStore(
        columnIndex: Map<C, Int>,
        rowIndex: Map<R, Int>
    ): LabeledArray2D<C, R, Relation> =
        LabeledArray2D.createQuadratic(columnIndex, rowIndex, UNKNOWN)
  }

  protected abstract val lookup: Map<Pair<Tag, Tag>, Collection2D<Variable, Variable, Relation>>

  protected abstract val reflexiveLookup:
      Map<Pair<Tag, Tag>, Collection2D<Variable, Variable, Relation>>

  final override fun get(column: Tagged<Variable>, row: Tagged<Variable>): Relation =
      reflexiveLookup.getValue(column.tag to row.tag)[column.variable, row.variable]

  final override fun set(column: Tagged<Variable>, row: Tagged<Variable>, value: Relation) {
    reflexiveLookup.getValue(column.tag to row.tag)[column.variable, row.variable] = value
  }

  final override fun put(
      column: Tagged<Variable>,
      row: Tagged<Variable>,
      value: Relation
  ): Relation =
      reflexiveLookup.getValue(column.tag to row.tag).put(column.variable, row.variable, value)

  final override fun equals(other: Any?): Boolean =
      when {
        this === other -> true
        other !is AbstractConstraint<*> -> false
        else -> javaClass == other.javaClass && lookup == other.lookup
      }

  final override fun hashCode(): Int = lookup.hashCode()

  final override fun toString(): String =
      indices.filter { (left, right) -> this[left, right] != UNKNOWN }.joinToString(
              " ∧ ", "(", ")") { (left, right) -> "$left ${this[left, right]} $right" }
}

fun <C, R, Collection : Collection2D<C, R, Relation>> Collection.putAndCheck(
    column: C,
    row: R,
    value: Relation
): Relation =
    try {
      this.put(column, row, value).also {
        if (it != UNKNOWN && it != value) {
          throw InfeasibleConstraintException()
        }
      }
    } catch (_: IllegalArgumentException) {
      throw InfeasibleConstraintException() // attempted to overwrite fixed diagonal
    }

@Suppress("UNCHECKED_CAST") // safe due to column + row comparison
val <C, R, Constraint : Collection2D<C, R, Relation>> Constraint.indices: List<Pair<C, R>>
  get() =
      if (columns == rows) columns.toList().combinations() as List<Pair<C, R>> else columns * rows

operator fun <C, R, Collection : Collection2D<C, R, Relation>> Collection.plusAssign(
    other: Collection
) {
  assert(columns == other.columns)
  assert(rows == other.rows)

  indices.forEach { (column, row) ->
    val value = other[column, row]
    if (value != UNKNOWN) putAndCheck(column, row, other[column, row])
  }
}

fun <E, Collection : Collection2D<E, E, Relation>> Collection.closeTransitive() {
  assert(columns == rows)

  columns.forEach { intermediate ->
    indices.forEach { (left, right) ->
      val leftToIntermediate = this[left, intermediate]
      val rightToIntermediate = this[right, intermediate]
      if (leftToIntermediate == EQUAL && rightToIntermediate != UNKNOWN) {
        this.putAndCheck(left, right, rightToIntermediate)
      } else if (leftToIntermediate != UNKNOWN && rightToIntermediate == EQUAL) {
        this.putAndCheck(left, right, leftToIntermediate)
      }
    }
  }
}

typealias Index<T> = Map<T, Int>

class ProjectionConstraint<Variable>
internal constructor(
    internal val registers: Index<Variable>,
    internal val parameters: Index<Variable>,
    internal val sourceToSource: LabeledArray2D<Variable, Variable, Relation>,
    internal val sourceToParameter: LabeledArray2D<Variable, Variable, Relation>,
    internal val sourceToDestination: LabeledArray2D<Variable, Variable, Relation>,
    internal val parameterToParameter: LabeledArray2D<Variable, Variable, Relation>,
    internal val parameterToDestination: LabeledArray2D<Variable, Variable, Relation>,
    internal val destinationToDestination: LabeledArray2D<Variable, Variable, Relation>,
) : AbstractConstraint<Variable>() {

  companion object {
    fun <Variable> assemble(
        source: LocationConstraint<Variable>,
        guard: GuardConstraint<Variable>,
        assignment: AssignmentConstraint<Variable>,
        destination: LocationConstraint<Variable>
    ): ProjectionConstraint<Variable> {
      assert(
          source.registers == guard.registers &&
              guard.registers == assignment.registers &&
              assignment.registers == destination.registers)
      assert(guard.parameters == assignment.parameters)

      return ProjectionConstraint(
              source.registers,
              guard.parameters,
              sourceToSource = source.sourceToSource.copyOf(),
              sourceToParameter = guard.sourceToParameter.copyOf(),
              sourceToDestination = assignment.sourceToDestination.copyOf(),
              parameterToParameter = guard.parameterToParameter.copyOf(),
              parameterToDestination = assignment.parameterToDestination.copyOf(),
              destinationToDestination = destination.sourceToSource.copyOf())
          .also { it.sourceToSource += guard.sourceToSource }
    }
  }

  override val lookup =
      mapOf(
          (SOURCE_REGISTER to SOURCE_REGISTER) to sourceToSource,
          (SOURCE_REGISTER to PARAMETER) to sourceToParameter,
          (SOURCE_REGISTER to DESTINATION_REGISTER) to sourceToDestination,
          (PARAMETER to PARAMETER) to parameterToParameter,
          (PARAMETER to DESTINATION_REGISTER) to parameterToDestination,
          (DESTINATION_REGISTER to DESTINATION_REGISTER) to destinationToDestination)

  override val reflexiveLookup =
      lookup +
          mapOf(
              (PARAMETER to SOURCE_REGISTER) to sourceToParameter.flip(),
              (DESTINATION_REGISTER to PARAMETER) to parameterToDestination.flip(),
              (DESTINATION_REGISTER to SOURCE_REGISTER) to sourceToDestination.flip())

  override val columns: Collection<Tagged<Variable>> by lazy {
    buildSet {
      registers.keys.forEach {
        this += SOURCE_REGISTER(it)
        this += DESTINATION_REGISTER(it)
      }
      parameters.keys.forEach { this += PARAMETER(it) }
    }
  }
  override val rows by lazy { columns }

  override fun copyOf(): ProjectionConstraint<Variable> =
      ProjectionConstraint(
          registers,
          parameters,
          sourceToSource.copyOf(),
          sourceToParameter.copyOf(),
          sourceToDestination.copyOf(),
          parameterToParameter.copyOf(),
          parameterToDestination.copyOf(),
          destinationToDestination.copyOf())

  fun toSourceConstraint(): LocationConstraint<Variable> =
      LocationConstraint(registers, sourceToSource)
  fun toGuardConstraint(): GuardConstraint<Variable> =
      GuardConstraint(
          registers, parameters, sourceToSource, sourceToParameter, parameterToParameter)
  fun toAssignmentConstraint(): AssignmentConstraint<Variable> =
      AssignmentConstraint(registers, parameters, sourceToDestination, parameterToDestination)
  fun toDestinationConstraint(): LocationConstraint<Variable> =
      LocationConstraint(registers, destinationToDestination)
}

class LocationConstraint<Variable>
internal constructor(
    internal val registers: Index<Variable>,
    internal val sourceToSource: LabeledArray2D<Variable, Variable, Relation>,
) : AbstractConstraint<Variable>() {

  constructor(
      registers: Index<Variable>,
  ) : this(
      registers,
      sourceToSource = createTriangularRelationStore(registers),
  )

  override val lookup =
      mapOf(
          (SOURCE_REGISTER to SOURCE_REGISTER) to sourceToSource,
      )

  override val reflexiveLookup = lookup

  override val columns: Collection<Tagged<Variable>> by lazy {
    registers.keys.map { SOURCE_REGISTER(it) }
  }
  override val rows by lazy { columns }

  override fun copyOf(): LocationConstraint<Variable> =
      LocationConstraint(
          registers,
          sourceToSource.copyOf(),
      )
}

class GuardConstraint<Variable>
internal constructor(
    internal val registers: Index<Variable>,
    internal val parameters: Index<Variable>,
    internal val sourceToSource: LabeledArray2D<Variable, Variable, Relation>,
    internal val sourceToParameter: LabeledArray2D<Variable, Variable, Relation>,
    internal val parameterToParameter: LabeledArray2D<Variable, Variable, Relation>,
) : AbstractConstraint<Variable>() {

  internal constructor(
      registers: Index<Variable>,
      parameters: Index<Variable>,
  ) : this(
      registers,
      parameters,
      sourceToSource = createTriangularRelationStore(registers),
      sourceToParameter = createQuadraticRelationStore(registers, parameters),
      parameterToParameter = createTriangularRelationStore(parameters),
  )

  override val lookup =
      mapOf(
          (SOURCE_REGISTER to SOURCE_REGISTER) to sourceToSource,
          (SOURCE_REGISTER to PARAMETER) to sourceToParameter,
          (PARAMETER to PARAMETER) to parameterToParameter)

  override val reflexiveLookup =
      lookup + mapOf((PARAMETER to SOURCE_REGISTER) to sourceToParameter.flip())

  override val columns: Collection<Tagged<Variable>> by lazy {
    buildSet {
      registers.keys.forEach { this += SOURCE_REGISTER(it) }
      parameters.keys.forEach { this += PARAMETER(it) }
    }
  }
  override val rows by lazy { columns }

  override fun copyOf(): GuardConstraint<Variable> =
      GuardConstraint(
          registers,
          parameters,
          sourceToSource.copyOf(),
          sourceToParameter.copyOf(),
          parameterToParameter.copyOf())
}

class AssignmentConstraint<Variable>
internal constructor(
    internal val registers: Index<Variable>,
    internal val parameters: Index<Variable>,
    internal val sourceToDestination: LabeledArray2D<Variable, Variable, Relation>,
    internal val parameterToDestination: LabeledArray2D<Variable, Variable, Relation>,
) : AbstractConstraint<Variable>() {

  constructor(
      registers: Index<Variable>,
      parameters: Index<Variable>,
  ) : this(
      registers,
      parameters,
      sourceToDestination = createQuadraticRelationStore(registers, registers),
      parameterToDestination = createQuadraticRelationStore(parameters, registers),
  )

  override val lookup =
      mapOf(
          (SOURCE_REGISTER to DESTINATION_REGISTER) to sourceToDestination,
          (PARAMETER to DESTINATION_REGISTER) to parameterToDestination,
      )

  override val reflexiveLookup =
      lookup +
          mapOf(
              (DESTINATION_REGISTER to PARAMETER) to parameterToDestination.flip(),
              (DESTINATION_REGISTER to SOURCE_REGISTER) to sourceToDestination.flip())

  override val columns: Collection<Tagged<Variable>> by lazy {
    buildSet {
      registers.keys.forEach { this += SOURCE_REGISTER(it) }
      parameters.keys.forEach { this += PARAMETER(it) }
    }
  }
  override val rows by lazy { registers.keys.map { DESTINATION_REGISTER(it) } }

  override fun copyOf(): AssignmentConstraint<Variable> =
      AssignmentConstraint(
          registers, parameters, sourceToDestination.copyOf(), parameterToDestination.copyOf())
}
