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

import net.automatalib.automata.concepts.StateIDs
import net.automatalib.automata.simple.SimpleAutomaton
import net.automatalib.automata.simple.SimpleDeterministicAutomaton
import net.automatalib.ts.simple.SimpleDTS
import net.automatalib.ts.simple.SimpleTS
import tools.aqua.koral.helpers.SimpleLocationIDs

/**
 * An extended automaton's configuration is a tuple of a [location] (from a finite set) and a
 * [valuation] (from a possibly infinite domain). An automaton with memory would use locations as
 * the automaton's states and valuations as the memory configuration.
 *
 * @param Location the location type.
 * @param Valuation the valuation type.
 *
 * @param location the location, from the corresponding automaton's finite set of locations.
 * @param valuation the valuation, e.g., the memory configuration.
 */
data class Configuration<out Location, out Valuation>(
    val location: Location,
    val valuation: Valuation
)

/**
 * The base type for extended automata. The underlying transition system's states are
 * [Configuration]s, with locations being defined in [locations].
 *
 * The corresponding concept for automata with finite states is [SimpleAutomaton].
 *
 * @param Location the location type.
 * @param Valuation the valuation type.
 * @param Input the automaton's input type.
 */
interface SimpleExtendedAutomaton<Location, Valuation, Input> :
    Iterable<Location>, SimpleTS<Configuration<Location, Valuation>, Input> {

  /** The automaton's location set. */
  val locations: Collection<Location>

  /** The number of locations in the automaton. */
  val size: Int
    get() = locations.size

  /** Returns an iterator over the automaton's locations. */
  override fun iterator(): Iterator<Location> = locations.iterator()

  /** Get a state enumerator over the locations. */
  fun locationIDs(): StateIDs<Location> = SimpleLocationIDs(this)
}

/**
 * The base type for *deterministic* extended automata. In a deterministic EA, for each input and
 * state, there exists at most one successor.
 *
 * Note that "no successor" is encoded as a `null` value, so the [Location] type should not be
 * nullable.
 *
 * The corresponding concept for automata with finite states is [SimpleDeterministicAutomaton].
 *
 * @param Location the location type.
 * @param Valuation the valuation type.
 * @param Input the automaton's input type.
 */
interface SimpleDeterministicExtendedAutomaton<Location, Valuation, Input> :
    SimpleDTS<Configuration<Location, Valuation>, Input>,
    SimpleExtendedAutomaton<Location, Valuation, Input>
