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

package tools.aqua.koral.algorithms

import tools.aqua.koral.automata.extended.Configuration
import tools.aqua.koral.automata.register.RATransition
import tools.aqua.koral.automata.succinct.SuccinctAssignment
import tools.aqua.koral.automata.succinct.SuccinctClause
import tools.aqua.koral.automata.succinct.SuccinctGuard
import tools.aqua.koral.automata.succinct.SuccinctRegisterAutomaton
import tools.aqua.koral.impl.QuickSuccinctNRA
import tools.aqua.koral.util.times

operator fun <LLocation, RLocation, Label, DataValue> SuccinctRegisterAutomaton<
    LLocation, String, Label, DataValue>.times(
    other: SuccinctRegisterAutomaton<RLocation, String, Label, DataValue>
): QuickSuccinctNRA<String, Label, DataValue> =
    QuickSuccinctNRA<String, Label, DataValue>().also { product ->
      val leftRegisterRelabeling = registers.associateWith { "l_$it" }
      val rightRegisterRelabeling = registers.associateWith { "r_$it" }

      check(labels.size == other.labels.size) { "Incompatible label alphabets" }
      val leftParameterIdentity = buildMap {
        labels.forEach { leftL ->
          this[leftL] = (leftL.parameterNames zip leftL.parameterNames).toMap()
        }
      }
      val rightParameterRelabeling = buildMap {
        labels.forEach { leftL ->
          val rightL =
              other.labels.singleOrNull { rightL ->
                leftL.label == rightL.label && leftL.arity == rightL.arity
              }
                  ?: error("Incompatible label $leftL")
          this[rightL] = (rightL.parameterNames zip leftL.parameterNames).toMap()
        }
      }

      val locationMap =
          (locations * other.locations).associateWith { (left, right) ->
            product.addLocation(isAcceptingLocation(left) && other.isAcceptingLocation(right))
                .also { it.humanReadableName = "$left+$right" }
          }

      (initialStates * other.initialStates).forEach { (left, right) ->
        product.addInitialState(
            Configuration(
                locationMap.getValue(left.location to right.location),
                left.valuation.relabelKeys(leftRegisterRelabeling) +
                    right.valuation.relabelKeys(rightRegisterRelabeling)))
      }

      locationMap.entries.forEach { (leftRight, combined) ->
        val (left, right) = leftRight
        (getLocationTransitions(left) * other.getLocationTransitions(right))
            .filter { (leftT, rightT) ->
              leftT.dataLabel.label == rightT.dataLabel.label &&
                  leftT.dataLabel.arity == rightT.dataLabel.arity
            }
            .forEach { (leftT, rightT) ->
              val (leftD, leftDL, leftG, leftA) = leftT
              val (rightD, rightDL, rightG, rightA) = rightT
              val rightRelabeling =
                  rightRegisterRelabeling + rightParameterRelabeling.getValue(rightDL)
              val leftRelabeling = leftRegisterRelabeling + leftParameterIdentity.getValue(leftDL)
              product.addLocationTransition(
                  combined,
                  RATransition(
                      locationMap.getValue(leftD to rightD),
                      leftDL,
                      leftG.relabel(leftRelabeling) + rightG.relabel(rightRelabeling),
                      leftA.relabel(leftRelabeling) + rightA.relabel(rightRelabeling)))
            }
      }

      product.removeUnreachableLocations()
    }

private fun <Variable, DataValue> SuccinctAssignment<Variable, DataValue>.relabel(
    relabeling: Map<Variable, Variable>
): SuccinctAssignment<Variable, DataValue> = copy(mapping = mapping.relabel(relabeling))

private fun <T> Map<T, T>.relabel(relabeling: Map<T, T>): Map<T, T> =
    map { (key, value) -> relabeling.getValue(key) to relabeling.getValue(value) }.toMap()

private fun <K, V> Map<K, V>.relabelKeys(relabeling: Map<K, K>): Map<K, V> = mapKeys { (key, _) ->
  relabeling.getValue(key)
}

private fun <Variable, DataValue> SuccinctGuard<Variable, DataValue>.relabel(
    relabeling: Map<Variable, Variable>
): SuccinctGuard<Variable, DataValue> = copy(clauses = clauses.map { it.relabel(relabeling) })

private fun <Variable, DataValue> SuccinctClause<Variable, DataValue>.relabel(
    relabeling: Map<Variable, Variable>
): SuccinctClause<Variable, DataValue> =
    copy(left = relabeling.getValue(left), right = relabeling.getValue(right))

private operator fun <Variable, DataValue> SuccinctGuard<Variable, DataValue>.plus(
    other: SuccinctGuard<Variable, DataValue>
): SuccinctGuard<Variable, DataValue> = SuccinctGuard(clauses + other.clauses)

private operator fun <Variable, DataValue> SuccinctAssignment<Variable, DataValue>.plus(
    other: SuccinctAssignment<Variable, DataValue>
): SuccinctAssignment<Variable, DataValue> = SuccinctAssignment(mapping + other.mapping)
