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

package tools.aqua.koral.base

import java.io.Serializable
import net.automatalib.SupportsGrowingAlphabet
import net.automatalib.automata.base.fast.AbstractFastMutable
import net.automatalib.automata.base.fast.AbstractFastMutableDet
import net.automatalib.automata.base.fast.AbstractFastMutableNondet
import net.automatalib.automata.concepts.StateIDs
import net.automatalib.automata.concepts.StateLocalInput
import net.automatalib.commons.util.nid.DynamicList
import net.automatalib.commons.util.nid.MutableNumericID
import net.automatalib.words.Alphabet
import net.automatalib.words.GrowingAlphabet
import net.automatalib.words.impl.GrowingMapAlphabet
import tools.aqua.koral.automata.acceptors.MutableFiniteLocationAcceptor
import tools.aqua.koral.automata.extended.Configuration
import tools.aqua.koral.automata.register.*

/**
 * Abstract superclass for quick (i.e., performant) shrinkable register automata. Data is stored in
 * maps to allow reasonably fast access and modification.
 *
 * The corresponding class for automata with finite states is [AbstractFastMutable].
 *
 * @param Location the location type.
 * @param Variable the variable name alphabet.
 * @param Label the label alphabet.
 * @param DataValue the data domain.
 * @param Guard the concrete guard type.
 * @param Assignment the concrete assignment type.
 * @param labelAlphabet the initial label alphabet, by default empty.
 */
abstract class AbstractQuickShrinkableRegisterAutomaton<
    Location : MutableNumericID,
    Variable,
    Label,
    DataValue,
    Guard : RAGuard<Variable, DataValue>,
    Assignment : RAAssignment<Variable, DataValue>>(
    private val labelAlphabet: GrowingAlphabet<DataLabel<Label, Variable>> = GrowingMapAlphabet()
) :
    MutableFiniteLocationAcceptor<
        Location,
        Map<Variable, DataValue>,
        DataSymbol<Label, Variable, DataValue>,
        RATransition<Location, Variable, Label, DataValue, Guard, Assignment>>,
    ShrinkableRegisterAutomaton<Location, Variable, Label, DataValue, Guard, Assignment>,
    Serializable,
    StateLocalInput<Location, DataLabel<Label, Variable>>,
    StateIDs<Location>,
    SupportsGrowingAlphabet<DataLabel<Label, Variable>> {

  /** Location metadata. */
  private data class LocationData<
      Location,
      Variable,
      Label,
      DataValue,
      Guard : RAGuard<Variable, DataValue>,
      Assignment : RAAssignment<Variable, DataValue>>(
      var isAccepting: Boolean,
      var transitions:
          MutableSet<RATransition<Location, Variable, Label, DataValue, Guard, Assignment>> =
          mutableSetOf()
  )

  /** Mutable holder for location metdata. */
  private val locationData =
      mutableMapOf<
          Location, LocationData<Location, Variable, Label, DataValue, Guard, Assignment>>()

  /** Mutable ordered holder for locations. */
  private val locationList = DynamicList<Location>()

  /** The growing set of labels. */
  override val labels: Alphabet<DataLabel<Label, Variable>> = labelAlphabet

  override val locations: List<Location>
    get() = locationList

  override fun addLocation(accepting: Boolean): Location =
      createLocation().also {
        locationList += it
        locationData[it] = LocationData(accepting)
      }

  /** Create a new location object. */
  protected abstract fun createLocation(): Location

  override fun isAcceptingLocation(location: Location): Boolean =
      locationData.getValue(location).isAccepting

  override fun setAccepting(location: Location, accepting: Boolean) {
    locationData.getValue(location).isAccepting = accepting
  }

  override fun getLocationTransitions(
      location: Location
  ): Collection<RATransition<Location, Variable, Label, DataValue, Guard, Assignment>> =
      locationData.getValue(location).transitions

  override fun setLocationTransitions(
      location: Location,
      transitions: Collection<RATransition<Location, Variable, Label, DataValue, Guard, Assignment>>
  ) {
    transitions.forEach { addAlphabetSymbol(it.dataLabel) }
    locationData.getValue(location).transitions = transitions.toMutableSet()
  }

  override fun replaceLocation(location: Location, replacement: Location?) {
    filterNot { location == it }.forEach { current ->
      val outgoing = getLocationTransitions(current)
      val untouched = outgoing.filterNot { location == it.destination }
      val altered =
          if (replacement != null)
              outgoing.filter { location == it.destination }.map {
                it.copy(destination = replacement)
              }
          else emptyList()
      setLocationTransitions(location, untouched + altered)
    }
    locationData.remove(location)
    locationList.remove(location)
  }

  override fun clear() {
    locationData.clear()
    locationList.clear()
  }

  override fun getStateId(location: Location): Int = location.id

  override fun getState(id: Int): Location = locationList[id]

  override fun addAlphabetSymbol(symbol: DataLabel<Label, Variable>) {
    labelAlphabet.addSymbol(symbol)
  }

  override fun getLocalInputs(location: Location): Collection<DataLabel<Label, Variable>> =
      labelAlphabet.filter { getLocationTransitions(location, it).isNotEmpty() }

  companion object {
    private const val serialVersionUID: Long = 1L
  }
}

/**
 * Abstract superclass for quick (i.e., performant) shrinkable register automata. Data is stored in
 * maps to allow reasonably fast access and modification.
 *
 * The corresponding class for automata with finite states is [AbstractFastMutableNondet].
 *
 * @param Location the location type.
 * @param Variable the variable name alphabet.
 * @param Label the label alphabet.
 * @param DataValue the data domain.
 * @param Guard the concrete guard type.
 * @param Assignment the concrete assignment type.
 * @param labelAlphabet the initial label alphabet, by default empty.
 */
abstract class AbstractQuickShrinkableNondeterministicRegisterAutomaton<
    Location : MutableNumericID,
    Variable,
    Label,
    DataValue,
    Guard : RAGuard<Variable, DataValue>,
    Assignment : RAAssignment<Variable, DataValue>>(
    labelAlphabet: GrowingAlphabet<DataLabel<Label, Variable>> = GrowingMapAlphabet()
) :
    AbstractQuickShrinkableRegisterAutomaton<
        Location, Variable, Label, DataValue, Guard, Assignment>(labelAlphabet) {
  private val initialStatesMutable =
      mutableSetOf<Configuration<Location, Map<Variable, DataValue>>>()

  override fun getInitialStates(): Set<Configuration<Location, Map<Variable, DataValue>>> =
      initialStatesMutable

  override fun clear() {
    super.clear()
    unsetInitialStates()
  }

  override fun addInitialState(state: Configuration<Location, Map<Variable, DataValue>>) {
    initialStatesMutable += state
  }

  override fun unsetInitialStates() = initialStatesMutable.clear()

  override fun replaceLocation(location: Location, replacement: Location?) {
    super.replaceLocation(location, replacement)

    val replacedInitial = initialStatesMutable.filter { it.location == location }
    if (replacement != null) {
      initialStatesMutable.addAll(replacedInitial.map { it.copy(location = replacement) })
    }
    initialStatesMutable.removeIf { it.location == location }
  }
}

/**
 * Abstract superclass for quick (i.e., performant) shrinkable register automata. Data is stored in
 * maps to allow reasonably fast access and modification.
 *
 * The corresponding class for automata with finite states is [AbstractFastMutableDet].
 *
 * @param Location the location type.
 * @param Variable the variable name alphabet.
 * @param Label the label alphabet.
 * @param DataValue the data domain.
 * @param Guard the concrete guard type.
 * @param Assignment the concrete assignment type.
 * @param labelAlphabet the initial label alphabet, by default empty.
 */
abstract class AbstractQuickShrinkableDeterministicRegisterAutomaton<
    Location : MutableNumericID,
    Variable,
    Label,
    DataValue,
    Guard : RAGuard<Variable, DataValue>,
    Assignment : RAAssignment<Variable, DataValue>>(
    labelAlphabet: GrowingAlphabet<DataLabel<Label, Variable>> = GrowingMapAlphabet()
) :
    AbstractQuickShrinkableRegisterAutomaton<
        Location, Variable, Label, DataValue, Guard, Assignment>(labelAlphabet),
    ShrinkableDeterministicRegisterAutomaton<
        Location, Variable, Label, DataValue, Guard, Assignment> {
  private var initialStateMutable: Configuration<Location, Map<Variable, DataValue>>? = null

  override fun getInitialState(): Configuration<Location, Map<Variable, DataValue>>? =
      initialStateMutable

  override fun clear() {
    super.clear()
    unsetInitialStates()
  }

  override fun addInitialState(state: Configuration<Location, Map<Variable, DataValue>>) {
    require(initialStateMutable != null && initialStateMutable != state) {
      "Cannot set state '$state' as additional initial state (current initial state: '$initialStateMutable')."
    }
    initialStateMutable = state
  }

  override fun unsetInitialStates() {
    initialStateMutable = null
  }

  override fun replaceLocation(location: Location, replacement: Location?) {
    super.replaceLocation(location, replacement)

    val currentInitial = initialStateMutable
    if (currentInitial != null && location == currentInitial.location) {
      initialStateMutable = replacement?.let { currentInitial.copy(location = it) }
    }
  }

  override fun setLocationTransitions(
      location: Location,
      transitions: Collection<RATransition<Location, Variable, Label, DataValue, Guard, Assignment>>
  ) {
    transitions.map { it.dataLabel }.forEach { addAlphabetSymbol(it) }
    super.setLocationTransitions(location, transitions)
  }
}
