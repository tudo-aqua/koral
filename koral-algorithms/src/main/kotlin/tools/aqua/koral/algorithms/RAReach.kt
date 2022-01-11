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

package tools.aqua.koral.algorithms

import tools.aqua.koral.automata.register.RegisterAutomaton
import tools.aqua.koral.automata.register.ShrinkableRegisterAutomaton
import tools.aqua.koral.util.removeFirst

fun <Location> ShrinkableRegisterAutomaton<Location, *, *, *, *, *>.removeUnreachableLocations() {
  val reached = computeReachableLocations()

  locations.filter { it !in reached }.forEach(::removeLocation)
}

fun <Location> RegisterAutomaton<Location, *, *, *, *, *>.computeReachableLocations():
    Set<Location> = buildSet {
  val toExamine = initialStates.mapTo(mutableSetOf()) { it.location }

  println("\n === INITIAL ===")
  toExamine.forEach(::println)

  while (toExamine.isNotEmpty()) {
    val location = toExamine.removeFirst()
    this += location

    getLocationTransitions(location).map { it.destination }.filter { it !in this }.forEach {
      toExamine += it
    }
  }
}
