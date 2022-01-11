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

import net.automatalib.automata.ShrinkableAutomaton
import net.automatalib.automata.ShrinkableDeterministic

/**
 * The base type for shrinkable universal extended automata. Shrinkablility implies that the
 * automaton can be modified programmatically, including additions and removals.
 *
 * The corresponding concept for automata with finite states is [ShrinkableAutomaton].
 *
 * @param Location the location type.
 * @param Valuation the valuation type.
 * @param Input the automaton's input type.
 * @param LocationTransition the location-level transition type.
 * @param Transition the transition system-level transition type.
 * @param LocationProperty the type of annotations on locations.
 */
interface ShrinkableExtendedAutomaton<
    Location,
    Valuation,
    Input,
    LocationTransition : ExtendedLocationTransition<Valuation, Input, Transition>,
    Transition,
    LocationProperty> :
    MutableExtendedAutomaton<
        Location, Valuation, Input, LocationTransition, Transition, LocationProperty> {

  /**
   * Remove a location from the automaton. This also removes all in- and outgoing location
   * transitions.
   * @param location the location to remove.
   */
  fun removeLocation(location: Location) = replaceLocation(location, null)

  /**
   * Replace a location in the automaton with a new one.
   *
   * This removes the location, but substitutes a replacement in all in- and outgoing transitions.
   * I.ee, if a transition previously targeted the location, it will target the replacement
   * afterwards. If `null` is passed as a replacement, this method operates identically to
   * [removeLocation].
   * @param location the location to replace.
   * @param replacement the replacement location or `null` to remove.
   */
  fun replaceLocation(location: Location, replacement: Location?)
}

/**
 * The base type for *deterministic* shrinkable universal extended automata.
 *
 * The corresponding concept for automata with finite states is [ShrinkableDeterministic].
 *
 * @param Location the location type.
 * @param Valuation the valuation type.
 * @param Input the automaton's input type.
 * @param LocationTransition the location-level transition type.
 * @param Transition the transition system-level transition type.
 * @param LocationProperty the type of annotations on locations.
 */
interface ShrinkableDeterministicExtendedAutomaton<
    Location,
    Valuation,
    Input,
    LocationTransition : ExtendedLocationTransition<Valuation, Input, Transition>,
    Transition,
    LocationProperty> :
    ShrinkableExtendedAutomaton<
        Location, Valuation, Input, LocationTransition, Transition, LocationProperty>,
    MutableDeterministicExtendedAutomaton<
        Location, Valuation, Input, LocationTransition, Transition, LocationProperty>
