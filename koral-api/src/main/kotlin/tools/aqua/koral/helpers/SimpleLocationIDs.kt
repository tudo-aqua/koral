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

package tools.aqua.koral.helpers

import net.automatalib.automata.concepts.StateIDs
import net.automatalib.automata.helpers.SimpleStateIDs
import tools.aqua.koral.automata.extended.SimpleExtendedAutomaton

/**
 * Location ID tracking mechanism for an extended automaton, similar to [SimpleStateIDs].
 * @param Location the location type.
 * @param automaton the automaton to cooperate with.
 */
class SimpleLocationIDs<Location>(automaton: SimpleExtendedAutomaton<Location, *, *>) :
    StateIDs<Location> {

  /** The internal ordered location list. */
  private val locations: List<Location> = automaton.locations.toList()

  /** The backwards location-to-index mapping. */
  private val locationIDs =
      locations.withIndex().associate { (index, location) -> location to index }

  override fun getStateId(location: Location): Int =
      locationIDs[location] ?: throw IllegalArgumentException("missing location $location")

  override fun getState(id: Int): Location? = locations[id]
}
