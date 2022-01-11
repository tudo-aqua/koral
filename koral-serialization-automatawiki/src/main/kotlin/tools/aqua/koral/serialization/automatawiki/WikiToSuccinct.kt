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

package tools.aqua.koral.serialization.automatawiki

import tools.aqua.koral.automata.register.DataLabel
import tools.aqua.koral.automata.register.RATransition
import tools.aqua.koral.automata.succinct.SuccinctAssignment
import tools.aqua.koral.automata.succinct.SuccinctClause
import tools.aqua.koral.automata.succinct.SuccinctGuard
import tools.aqua.koral.automata.succinct.SuccinctOperator.EQUALS
import tools.aqua.koral.automata.succinct.SuccinctOperator.NOT_EQUALS
import tools.aqua.koral.automata.succinct.SuccinctRegisterAutomaton
import tools.aqua.koral.impl.QuickSuccinctNRA
import tools.aqua.koral.impl.RALocation
import tools.aqua.koral.util.times

fun AutomataWikiRegisterAutomaton.tryToSuccinct():
    SuccinctRegisterAutomaton<RALocation, String, String, Int>? {

  val automaton = QuickSuccinctNRA<String, String, Int>()

  val literalValuation =
      xml.transitions.flatMapTo(mutableSetOf()) { it.guard.gatherLiterals() }.associate {
        it.toSuccinct() to it.value
      }

  val nonInitialLocationMap =
      xml.locationObjects.filter { it.isInitial.not() }.associate {
        it.name to automaton.addLocation(it.isInitial).apply { humanReadableName = it.name }
      }

  val initialLocationMap =
      initialStates.associate { (location, valuation) ->
        location to
            automaton.addInitialState(valuation + literalValuation).location.apply {
              humanReadableName = location
            }
      }

  val locationMap = nonInitialLocationMap + initialLocationMap

  val labelMap =
      xml.alphabet.inputsAndOutputs.associate {
        it.name to DataLabel(it.name, it.params.map(AWTypedVariable::name))
      }

  xml.transitions.forEach {
    val transition = relabel(it)

    val succinctGuards = transition.guard.tryToSuccinct() ?: return null

    succinctGuards.forEach { succinctGuard ->
      val succinctTransition =
          RATransition(
              destination = locationMap.getValue(transition.to),
              dataLabel = labelMap.getValue(transition.symbol),
              guard = succinctGuard,
              assignment = transition.assignments.toSuccinct(registers))

      automaton.addLocationTransition(locationMap.getValue(transition.from), succinctTransition)
    }
  }

  return automaton
}

fun Guard.gatherLiterals(): Set<Literal> =
    when (this) {
      is True -> emptySet()
      is BinaryRelation -> setOfNotNull(left as? Literal, right as? Literal)
      is VariadicRelation -> terms.flatMapTo(mutableSetOf(), Guard::gatherLiterals)
    }

fun Guard.tryToSuccinct(): Set<SuccinctGuard<String, Int>>? {
  val clauses = collectClauses()
  if (clauses.any { conjunct -> conjunct.any { it !is Equals && it !is NotEquals } }) {
    return null
  }
  return clauses.mapTo(mutableSetOf()) { conjunct ->
    SuccinctGuard(conjunct.map { it.toSuccinct() })
  }
}

fun Guard.collectClauses(): Set<Set<BinaryRelation>> =
    when (this) {
      is Or -> terms.flatMapTo(mutableSetOf()) { it.collectClauses() }
      is And ->
          terms.map { it.collectClauses() }.reduce { left, right ->
            (left * right).mapTo(mutableSetOf()) { (leftC, rightC) -> leftC + rightC }
          }
      is True -> setOf(emptySet())
      is BinaryRelation -> setOf(setOf(this))
    }

fun BinaryRelation.toSuccinct(): SuccinctClause<String, Int> =
    when (this) {
      is Equals -> SuccinctClause(left.toSuccinct(), EQUALS, right.toSuccinct())
      is NotEquals -> SuccinctClause(left.toSuccinct(), NOT_EQUALS, right.toSuccinct())
      else -> error("unfiltered non-succinct relation")
    }

fun Expression.toSuccinct(): String =
    when (this) {
      is Variable -> name
      is Literal -> "_const_$value" // wiki automata can't use underscore in identifiers
    }

internal fun List<AWAssignment>.toSuccinct(
    registers: Collection<String>
): SuccinctAssignment<String, Int> {
  val defined = associate { it.to to it.from }
  val definedPlusIdentity =
      registers.filterNot { it in defined.keys }.associateWithTo(mutableMapOf()) { it }.apply {
        this += defined
      }
  return SuccinctAssignment(definedPlusIdentity)
}
