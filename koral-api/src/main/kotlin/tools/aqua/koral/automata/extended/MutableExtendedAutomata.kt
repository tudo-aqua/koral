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

import net.automatalib.automata.MutableAutomaton
import net.automatalib.automata.MutableDeterministic

/**
 * The base type for mutable universal extended automata. Mutablility implies that the automaton can
 * be extended programmatically, but not necessarily reduced. This is encoded by
 * [ShrinkableExtendedAutomaton].
 *
 * The corresponding concept for automata with finite states is [MutableAutomaton].
 *
 * @param Location the location type.
 * @param Valuation the valuation type.
 * @param Input the automaton's input type.
 * @param LocationTransition the location-level transition type.
 * @param Transition the transition system-level transition type.
 * @param LocationProperty the type of annotations on locations.
 */
interface MutableExtendedAutomaton<
    Location,
    Valuation,
    Input,
    LocationTransition : ExtendedLocationTransition<Valuation, Input, Transition>,
    Transition,
    LocationProperty> :
    UniversalExtendedAutomaton<
        Location, Valuation, Input, LocationTransition, Transition, LocationProperty> {

  /** Remove all locations and transitions from the automaton. */
  fun clear()

  /**
   * Add a new location to the automaton using a default value for the property. This value defaults
   * to `null`, but may be overridden by a subclass to a more sensible value.
   * @return the new location object.
   */
  fun addLocation(): Location = addLocation(null)

  /**
   * Add a new location with a given location property.
   * @param property the property value to use.
   * @return the new location object.
   */
  fun addLocation(property: LocationProperty?): Location

  /**
   * Alter a given location's property.
   * @param location the location to alter.
   * @param property the new property value. Implementations must handle null values, e.g., by
   * substituting a default value.
   */
  fun setLocationProperty(location: Location, property: LocationProperty?)

  /**
   * Add an initial state to the automation. This creates a new location and combines it with the
   * given valuation. The location's property is set according to [addLocation].
   * @param valuation the initial valuation.
   * @return a configuration of new location and [valuation].
   */
  fun addInitialState(valuation: Valuation): Configuration<Location, Valuation> =
      addInitialState(null, valuation)

  /**
   * Add an initial state to the automation. This creates a new location with the given property and
   * combines it with the given valuation.
   * @param property the property to assign to the new location.
   * @param valuation the initial valuation.
   * @return a configuration of new location and [valuation].
   */
  fun addInitialState(
      property: LocationProperty?,
      valuation: Valuation
  ): Configuration<Location, Valuation> =
      Configuration(addLocation(property), valuation).also { addInitialState(it) }

  /**
   * Add an initial state to the automaton. This assumes a pre-existing location.
   * @param state the new initial state. Its location must result from a call to [addLocation] or
   * similar.
   */
  fun addInitialState(state: Configuration<Location, Valuation>)

  /**
   * Revove all initial states from the automaton. The corresponding locations must remain in the
   * automaton.
   */
  fun unsetInitialStates()

  /**
   * Set the location transitions for a given source location.
   * @param location the location for which the outgoing transitions are overwritten.
   * @param transitions the replacement transition set.
   */
  fun setLocationTransitions(location: Location, transitions: Collection<LocationTransition>)

  /**
   * Add a location transition to a location's set of outgoing transitions.
   * @param location the location for which the outgoing transitions are modified.
   * @param transition the new transition.
   */
  fun addLocationTransition(location: Location, transition: LocationTransition) {
    getLocationTransitions(location).toMutableSet().also {
      if (it.add(transition)) setLocationTransitions(location, it)
    }
  }

  /**
   * Add multiple location transitions to a location's set of outgoing transitions.
   * @param location the location for which the outgoing transitions are modified.
   * @param transitions the new transitions.
   */
  fun addLocationTransitions(location: Location, transitions: Collection<LocationTransition>) {
    getLocationTransitions(location).toMutableSet().also {
      if (it.addAll(transitions)) setLocationTransitions(location, it)
    }
  }

  /**
   * Remove a location transition from a location's set of outgoing transitions. If it is not in the
   * set, the operation does not modify the automaton.
   * @param location the location for which the outgoing transitions are modified.
   * @param transition the transition to remove.
   */
  fun removeLocationTransition(location: Location, transition: LocationTransition) {
    getLocationTransitions(location).toMutableSet().also {
      if (it.remove(transition)) setLocationTransitions(location, it)
    }
  }

  /**
   * Remove all outgoing transitions from a location.
   * @param location the location for which the outgoing transitions are removed.
   */
  fun removeAllLocationTransitions(location: Location) =
      setLocationTransitions(location, emptySet())
}

/**
 * The base type for *deterministic* mutable universal extended automata.
 *
 * The corresponding concept for automata with finite states is [MutableDeterministic].
 *
 * @param Location the location type.
 * @param Valuation the valuation type.
 * @param Input the automaton's input type.
 * @param LocationTransition the location-level transition type.
 * @param Transition the transition system-level transition type.
 * @param LocationProperty the type of annotations on locations.
 */
interface MutableDeterministicExtendedAutomaton<
    Location,
    Valuation,
    Input,
    LocationTransition : ExtendedLocationTransition<Valuation, Input, Transition>,
    Transition,
    LocationProperty> :
    MutableExtendedAutomaton<
        Location, Valuation, Input, LocationTransition, Transition, LocationProperty>,
    UniversalDeterministicExtendedAutomaton<
        Location, Valuation, Input, LocationTransition, Transition, LocationProperty>
