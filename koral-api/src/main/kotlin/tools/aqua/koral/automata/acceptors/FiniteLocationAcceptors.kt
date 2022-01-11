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

package tools.aqua.koral.automata.acceptors

import net.automatalib.automata.concepts.SuffixOutput
import net.automatalib.automata.fsa.*
import net.automatalib.commons.util.WrapperUtil
import net.automatalib.ts.acceptors.AcceptorTS
import tools.aqua.koral.automata.extended.*

/**
 * Finite location acceptors accepts words in a language over an infinite domain. Transitions lead
 * from one configuration to the next. As a result, the transition type is the configuration type. A
 * word is accepted if, staring from initial states, a corresponding sequence of transitions to an
 * accepting state can be made. Acceptance is encoded as the location property and defaults to
 * `false`.
 *
 * The corresponding concept for automata with finite states is [FiniteStateAcceptor].
 *
 * @param Location the location type.
 * @param Valuation the valuation type.
 * @param Input the automaton's input type.
 * @param LocationTransition the location-level transition type.
 */
interface FiniteLocationAcceptor<
    Location,
    Valuation,
    Input,
    LocationTransition : ExtendedLocationTransition<
        Valuation, Input, Configuration<Location, Valuation>>> :
    AcceptorTS<Configuration<Location, Valuation>, Input>,
    UniversalExtendedAutomaton<
        Location,
        Valuation,
        Input,
        LocationTransition,
        Configuration<Location, Valuation>,
        Boolean>,
    SuffixOutput<Input, Boolean> {

  override fun computeSuffixOutput(prefix: Iterable<Input>, suffix: Iterable<Input>): Boolean =
      computeOutput(prefix + suffix)

  /** Checks if the given [location] is accepting. */
  fun isAcceptingLocation(location: Location): Boolean
  /**
   * Checks if the given set of [locations] is accepting. The semantics of an accepting set (any,
   * all) are defined by the specific subclass.
   */
  fun isAcceptingLocations(locations: Collection<Location>): Boolean

  override fun isAccepting(state: Configuration<Location, Valuation>): Boolean =
      isAcceptingLocation(state.location)

  override fun isAccepting(states: Collection<Configuration<Location, Valuation>>): Boolean =
      isAcceptingLocations(states.map(Configuration<Location, Valuation>::location))

  override fun getLocationProperty(location: Location): Boolean = isAcceptingLocation(location)

  override fun getStateProperty(state: Configuration<Location, Valuation>): Boolean =
      super<AcceptorTS>.getStateProperty(state)
}

/**
 * Mutable finite location acceptors can modify the set of initial states and a location's
 * acceptance.
 *
 * The corresponding concept for automata with finite states is [MutableFSA].
 *
 * @param Location the location type.
 * @param Valuation the valuation type.
 * @param Input the automaton's input type.
 * @param LocationTransition the location-level transition type.
 */
interface MutableFiniteLocationAcceptor<
    Location,
    Valuation,
    Input,
    LocationTransition : ExtendedLocationTransition<
        Valuation, Input, Configuration<Location, Valuation>>> :
    FiniteLocationAcceptor<Location, Valuation, Input, LocationTransition>,
    MutableExtendedAutomaton<
        Location,
        Valuation,
        Input,
        LocationTransition,
        Configuration<Location, Valuation>,
        Boolean> {

  /**
   * Invert the given automaton's acceptance. For a deterministic automaton, the resulting automaton
   * usually recognizes the complement language.
   */
  fun flipAcceptance() = forEach { location ->
    setAccepting(location, !isAcceptingLocation(location))
  }

  /** Set [location]'s acceptance to [accepting]. */
  fun setAccepting(location: Location, accepting: Boolean)

  /**
   * Add a new location.
   * @param accepting `true` iff the location shall be accepting.
   * @return the new location object.
   */
  fun addLocation(accepting: Boolean): Location

  override fun addLocation(): Location = addLocation(false)

  override fun addLocation(property: Boolean?): Location =
      addLocation(WrapperUtil.booleanValue(property))

  /**
   * Add an initial state with a given acceptance. This method exists to offer a null-safe
   * interface.
   * @param accepting `true` iff the state should be accepting.
   * @param valuation the valuation to assign to the initial state.
   * @return a configuration combining the new location and the given [valuation].
   */
  fun addInitialState(
      accepting: Boolean,
      valuation: Valuation
  ): Configuration<Location, Valuation> = super.addInitialState(accepting, valuation)

  override fun addInitialState(valuation: Valuation): Configuration<Location, Valuation> =
      addInitialState(false, valuation)

  override fun setLocationProperty(location: Location, property: Boolean?) {
    setAccepting(location, WrapperUtil.booleanValue(property))
  }
}
