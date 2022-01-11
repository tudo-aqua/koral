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

package tools.aqua.koral.impl

import net.automatalib.automata.fsa.impl.FastDFA
import net.automatalib.automata.fsa.impl.FastNFA
import net.automatalib.commons.util.nid.AbstractMutableNumericID
import tools.aqua.koral.automata.register.DataSymbol
import tools.aqua.koral.automata.succinct.SuccinctAssignment
import tools.aqua.koral.automata.succinct.SuccinctGuard
import tools.aqua.koral.automata.succinct.SuccinctRegisterAutomaton
import tools.aqua.koral.base.AbstractQuickShrinkableDeterministicRegisterAutomaton
import tools.aqua.koral.base.AbstractQuickShrinkableNondeterministicRegisterAutomaton

/** Location objects for quick RAs. */
class RALocation(var humanReadableName: String? = null) : AbstractMutableNumericID() {
  override fun toString(): String = humanReadableName ?: "l_$id"
}

/**
 * Performant implementation of succinct nondeterministic RAs. These use [RALocation]s as location
 * objects.
 *
 * The corresponding class for automata with finite states is [FastNFA].
 *
 * @param Variable the variable name alphabet.
 * @param Label the label alphabet.
 * @param DataValue the data domain.
 */
open class QuickSuccinctNRA<Variable, Label, DataValue> :
    AbstractQuickShrinkableNondeterministicRegisterAutomaton<
        RALocation,
        Variable,
        Label,
        DataValue,
        SuccinctGuard<Variable, DataValue>,
        SuccinctAssignment<Variable, DataValue>>(),
    SuccinctRegisterAutomaton<RALocation, Variable, Label, DataValue> {

  override fun createLocation(): RALocation = RALocation()
}

/**
 * Performant implementation of succinct deterministic RAs. These use [RALocation]s as location
 * objects. Determinism is not enforced programmatically, as this is not trivial and multiple
 * semantics can be used.
 *
 * The corresponding class for automata with finite states is [FastDFA].
 *
 * @param Variable the variable name alphabet.
 * @param Label the label alphabet.
 * @param DataValue the data domain.
 */
open class QuickSuccinctDRA<Variable, Label, DataValue> :
    AbstractQuickShrinkableDeterministicRegisterAutomaton<
        RALocation,
        Variable,
        Label,
        DataValue,
        SuccinctGuard<Variable, DataValue>,
        SuccinctAssignment<Variable, DataValue>>(),
    SuccinctRegisterAutomaton<RALocation, Variable, Label, DataValue> {

  override fun createLocation(): RALocation = RALocation()

  override fun computeSuffixOutput(
      prefix: Iterable<DataSymbol<Label, Variable, DataValue>>,
      suffix: Iterable<DataSymbol<Label, Variable, DataValue>>
  ): Boolean =
      super<AbstractQuickShrinkableDeterministicRegisterAutomaton>.computeSuffixOutput(
          prefix, suffix)
}
