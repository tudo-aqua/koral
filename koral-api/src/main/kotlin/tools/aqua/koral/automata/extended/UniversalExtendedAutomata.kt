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

import net.automatalib.automata.UniversalAutomaton
import net.automatalib.automata.UniversalDeterministicAutomaton
import net.automatalib.ts.UniversalDTS
import net.automatalib.ts.UniversalTransitionSystem

/**
 * The base type for extended automata with additional properties bound to the transition structure.
 * These might, e.g., include a state's acceptance. Note that due to them infinite size of the
 * transition system, no transition properties are supported. Instead, they can be encoded as
 * [LocationTransition]s.
 *
 * The corresponding concept for automata with finite states is [UniversalAutomaton]. See its
 * documentation for details on properties.
 *
 * @param Location the location type.
 * @param Valuation the valuation type.
 * @param Input the automaton's input type.
 * @param LocationTransition the location-level transition type.
 * @param Transition the transition system-level transition type.
 * @param LocationProperty the type of annotations on locations.
 */
interface UniversalExtendedAutomaton<
    Location,
    Valuation,
    Input,
    LocationTransition : ExtendedLocationTransition<Valuation, Input, Transition>,
    Transition,
    LocationProperty> :
    ExtendedAutomaton<Location, Valuation, Input, LocationTransition, Transition>,
    UniversalTransitionSystem<
        Configuration<Location, Valuation>, Input, Transition, LocationProperty, Void> {

  /** Read a given [location]'s property. */
  fun getLocationProperty(location: Location): LocationProperty

  override fun getStateProperty(state: Configuration<Location, Valuation>): LocationProperty =
      getLocationProperty(state.location)
}

/**
 * The base type for *deterministic* extended automata with additional properties bound to the
 * transition structure.
 *
 * The corresponding concept for automata with finite states is [UniversalDeterministicAutomaton].
 *
 * @param Location the location type.
 * @param Valuation the valuation type.
 * @param Input the automaton's input type.
 * @param LocationTransition the location-level transition type.
 * @param Transition the transition system-level transition type.
 * @param LocationProperty the type of annotations on locations.
 */
interface UniversalDeterministicExtendedAutomaton<
    Location,
    Valuation,
    Input,
    LocationTransition : ExtendedLocationTransition<Valuation, Input, Transition>,
    Transition,
    LocationProperty> :
    DeterministicExtendedAutomaton<Location, Valuation, Input, LocationTransition, Transition>,
    UniversalDTS<Configuration<Location, Valuation>, Input, Transition, LocationProperty, Void>,
    UniversalExtendedAutomaton<
        Location, Valuation, Input, LocationTransition, Transition, LocationProperty>
