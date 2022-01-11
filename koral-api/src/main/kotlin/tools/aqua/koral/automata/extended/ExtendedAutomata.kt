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

import net.automatalib.automata.Automaton
import net.automatalib.automata.DeterministicAutomaton
import net.automatalib.ts.DeterministicTransitionSystem
import net.automatalib.ts.TransitionSystem
import tools.aqua.koral.automata.extended.*

fun interface ExtendedLocationTransition<in Valuation, in Input, out Transition> {
  fun getTransitionOrNull(valuation: Valuation, input: Input): Transition?
}

/**
 * The base type for extended automata with a visible transition structure on the locations. These
 * location transitions are defined per location and encode the additional constraints and valuation
 * changes for an EA's transitions. Semantically, they either map to a transition in the underlying
 * TS or to null if the transition is unavailable, e.g., due to a transition guard.
 *
 * The corresponding concept for automata with finite states is [Automaton].
 *
 * @param Location the location type.
 * @param Valuation the valuation type.
 * @param Input the automaton's input type.
 * @param LocationTransition the location-level transition type. These must implement functions from
 * a valuation-input-tuple to either the resulting transition object or `null` (to reject).
 * @param Transition the transition system-level transition type.
 */
interface ExtendedAutomaton<
    Location,
    Valuation,
    Input,
    LocationTransition : ExtendedLocationTransition<Valuation, Input, Transition>,
    Transition> :
    SimpleExtendedAutomaton<Location, Valuation, Input>,
    TransitionSystem<Configuration<Location, Valuation>, Input, Transition> {

  override fun getTransitions(
      state: Configuration<Location, Valuation>,
      input: Input
  ): Collection<Transition> =
      getLocationTransitions(state.location).mapNotNull {
        it.getTransitionOrNull(state.valuation, input)
      }

  /** Get the set of location transitions originating in [location]. */
  fun getLocationTransitions(location: Location): Collection<LocationTransition>
}

/**
 * The base type for *deterministic* EAs with a visible transition structure on the locations. Here,
 * at most one location transition per input and configuration must return a non-`null` value.
 *
 * The determinism must be enforced by an implementing class, since a semantic understanding of the
 * location transitions is required.
 *
 * The corresponding concept for automata with finite states is [DeterministicAutomaton].
 *
 * @param Location the location type.
 * @param Valuation the valuation type.
 * @param Input the automaton's input type.
 * @param LocationTransition the location-level transition type.
 * @param Transition the transition system-level transition type.
 */
interface DeterministicExtendedAutomaton<
    Location,
    Valuation,
    Input,
    LocationTransition : ExtendedLocationTransition<Valuation, Input, Transition>,
    Transition> :
    DeterministicTransitionSystem<Configuration<Location, Valuation>, Input, Transition>,
    ExtendedAutomaton<Location, Valuation, Input, LocationTransition, Transition>,
    SimpleDeterministicExtendedAutomaton<Location, Valuation, Input> {

  override fun getTransition(state: Configuration<Location, Valuation>, input: Input): Transition? =
      getLocationTransitions(state.location).firstNotNullOfOrNull {
        it.getTransitionOrNull(state.valuation, input)
      }

  override fun getTransitions(
      state: Configuration<Location, Valuation>,
      input: Input
  ): MutableCollection<Transition> =
      super<DeterministicTransitionSystem>.getTransitions(state, input)
}
