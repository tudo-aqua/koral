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

package tools.aqua.koral.automata.extended

import net.automatalib.automata.concepts.DetSuffixOutputAutomaton
import net.automatalib.automata.concepts.SuffixOutput
import net.automatalib.exception.UndefinedPropertyAccessException
import tools.aqua.koral.automata.extended.*

/**
 * The base type for *deterministic* extended automata with an output after each transition and
 * suffix output behavior, i.e., the ability to compute output given a starting state and an input.
 *
 * The corresponding concept for automata with finite states is [DetSuffixOutputAutomaton].
 *
 * @param Location the location type.
 * @param Valuation the valuation type.
 * @param Input the automaton's input type.
 * @param LocationTransition the location-level transition type.
 * @param Transition the transition system-level transition type.
 * @param Output the type of output.
 */
interface DeterministicSuffixOutputExtendedAutomaton<
    Location,
    Valuation,
    Input,
    LocationTransition : ExtendedLocationTransition<Valuation, Input, Transition>,
    Transition,
    Output> :
    DeterministicExtendedAutomaton<Location, Valuation, Input, LocationTransition, Transition>,
    SuffixOutput<Input, Output> {
  override fun computeSuffixOutput(prefix: Iterable<Input>, suffix: Iterable<Input>): Output =
      getState(prefix)?.let { computeStateOutput(it, suffix) }
          ?: throw UndefinedPropertyAccessException("The state accessed by $prefix is undefined")

  /** Compute the output resulting from an [input] in a given [state]. */
  fun computeStateOutput(state: Configuration<Location, Valuation>, input: Iterable<Input>): Output

  override fun computeOutput(input: Iterable<Input>): Output =
      initialState?.let { computeStateOutput(it, input) }
          ?: throw UndefinedPropertyAccessException("No initial state defined")
}
