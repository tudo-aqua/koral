/*
 * Copyright 2021-2022 The Koral Authors
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

package tools.aqua.koral.automata.succinct

import tools.aqua.koral.automata.register.RAAssignment
import tools.aqua.koral.automata.register.RAGuard
import tools.aqua.koral.automata.register.RATransition
import tools.aqua.koral.automata.register.RegisterAutomaton

/**
 * A succinct guard clause is a conjunction of clauses, i.e., comparisons over variables.
 *
 * @param Variable the variable alphabet.
 * @param clauses the set of clauses.
 */
data class SuccinctGuard<Variable, DataValue>(
    val clauses: Collection<SuccinctClause<Variable, DataValue>>
) : RAGuard<Variable, DataValue> {

  /** The variables used in the guard. */
  val variables: Set<Variable> = clauses.flatMapTo(mutableSetOf()) { it.variables }

  /** Convenience constructor taking [clauses] as varargs. */
  constructor(vararg clauses: SuccinctClause<Variable, DataValue>) : this(clauses.toList())

  /** Evaluate the guard given a valuation. */
  override fun isSatisfiedBy(valuation: Map<Variable, DataValue>): Boolean =
      clauses.all { it.isSatisfiedBy(valuation) }
}

/**
 * A comparison compares two variables, requiring them to be equal (`a == b`) or distinct (`a <>
 * b`).
 *
 * @param Variable the variable alphabet.
 * @param left the left-hand-side variable.
 * @param operator the operator, either equality or distinctness.
 * @param right the right-hand-side variable.
 */
data class SuccinctClause<Variable, DataValue>(
    val left: Variable,
    val operator: SuccinctOperator,
    val right: Variable
) : RAGuard<Variable, DataValue> {

  /** The variables used in both expressions. */
  val variables: Collection<Variable> = setOf(left, right)

  /** Negate this clause (i.e., negate the operator). */
  fun negate(): SuccinctClause<Variable, DataValue> = copy(operator = operator.negate())

  override fun isSatisfiedBy(valuation: Map<Variable, DataValue>): Boolean =
      operator.isSatisfiedBy(valuation.getValue(left), valuation.getValue(right))
}

/** The operators acceptable for comparisons. */
enum class SuccinctOperator {
  EQUALS,
  NOT_EQUALS,
  ;

  /** Negate this operator. */
  fun negate(): SuccinctOperator =
      when (this) {
        EQUALS -> NOT_EQUALS
        NOT_EQUALS -> EQUALS
      }

  /** Execute this operator on data values. */
  fun <DataValue> isSatisfiedBy(left: DataValue, right: DataValue) =
      when (this) {
        EQUALS -> left == right
        NOT_EQUALS -> left != right
      }
}

/**
 * A succinct assignment is a mapping from target registers to source variables. The assignment is
 * computed in parallel, i.e., `b := c; a := b` will not assign `c`'s value to `a`.
 *
 * @param Variable the variable alphabet.
 * @param mapping the target-to-source mapping.
 */
data class SuccinctAssignment<Variable, DataValue>(val mapping: Map<Variable, Variable>) :
    RAAssignment<Variable, DataValue> {

  /** Get the set of source variables used by the assignment. */
  val sourceVariables: Collection<Variable>
    get() = mapping.values

  /** Get the set of registers written to by the assignment. */
  val targetRegisters: Collection<Variable>
    get() = mapping.keys

  /** Perform the assignment operation using a given valuation. */
  override fun computeFrom(valuation: Map<Variable, DataValue>): Map<Variable, DataValue> =
      mapping.mapValues { (_, source) -> valuation.getValue(source) }
}

typealias SuccinctTransition<Location, Variable, Label, DataValue> =
    RATransition<
        Location,
        Variable,
        Label,
        DataValue,
        SuccinctGuard<Variable, DataValue>,
        SuccinctAssignment<Variable, DataValue>>

/** A succinct register automaton using [SuccinctGuard]s and [SuccinctAssignment]s. */
interface SuccinctRegisterAutomaton<Location, Variable, Label, DataValue> :
    RegisterAutomaton<
        Location,
        Variable,
        Label,
        DataValue,
        SuccinctGuard<Variable, DataValue>,
        SuccinctAssignment<Variable, DataValue>> {

  override val registers: Collection<Variable>
    get() = buildSet {
      initialStates.forEach { this += it.valuation.keys }
      locations.forEach { location ->
        getLocationTransitions(location).forEach { this += it.assignment.targetRegisters }
      }
    }
}
