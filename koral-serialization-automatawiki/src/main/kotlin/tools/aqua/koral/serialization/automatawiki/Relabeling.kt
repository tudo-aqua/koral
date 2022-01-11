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

internal fun AutomataWikiRegisterAutomaton.relabel(transition: AWTransition): AWTransition {
  val label = transition.symbol
  val realParameters =
      xml.alphabet.inputsAndOutputs.first { it.name == label }.params.map { it.name }
  val naming = (transition.params zip realParameters).toMap().withDefault { it }
  return AWTransition().also {
    it.from = transition.from
    it.to = transition.to
    it.symbol = transition.symbol
    it.params = realParameters
    it.guard = transition.guard.relabel(naming)
    it.assignments = transition.assignments.relabel(naming)
  }
}

private fun Guard.relabel(naming: Map<String, String>): Guard =
    when (this) {
      is True -> this
      is Equals -> Equals(left.relabel(naming), right.relabel(naming))
      is NotEquals -> NotEquals(left.relabel(naming), right.relabel(naming))
      is Greater -> Greater(left.relabel(naming), right.relabel(naming))
      is GreaterEquals -> GreaterEquals(left.relabel(naming), right.relabel(naming))
      is Less -> Less(left.relabel(naming), right.relabel(naming))
      is LessEquals -> LessEquals(left.relabel(naming), right.relabel(naming))
      is And -> And(terms.map { it.relabel(naming) })
      is Or -> Or(terms.map { it.relabel(naming) })
    }

private fun Expression.relabel(naming: Map<String, String>): Expression =
    if (this is Variable) Variable(naming.getValue(name)) else this

private fun List<AWAssignment>.relabel(naming: Map<String, String>): List<AWAssignment> = map {
  it.relabel(naming)
}

private fun AWAssignment.relabel(naming: Map<String, String>): AWAssignment =
    AWAssignment().also {
      it.from = naming.getValue(from)
      it.to = naming.getValue(to)
    }
