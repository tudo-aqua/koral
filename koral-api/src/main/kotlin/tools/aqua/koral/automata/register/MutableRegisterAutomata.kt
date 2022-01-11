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

package tools.aqua.koral.automata.register

import net.automatalib.automata.fsa.*
import tools.aqua.koral.automata.extended.*

/**
 * The base type for mutable register automata. Mutablility implies that the automaton can be
 * extended programmatically, but not necessarily reduced. This is encoded by
 * [ShrinkableRegisterAutomaton].
 *
 * The corresponding concept for finite-state automata is [MutableNFA].
 *
 * @param Location the location type.
 * @param Variable the variable name alphabet.
 * @param Label the label alphabet.
 * @param DataValue the data domain.
 * @param Guard the concrete guard type.
 * @param Assignment the concrete assignment type.
 */
interface MutableRegisterAutomaton<
    Location,
    Variable,
    Label,
    DataValue,
    Guard : RAGuard<Variable, DataValue>,
    Assignment : RAAssignment<Variable, DataValue>> :
    MutableExtendedAutomaton<
        Location,
        Map<Variable, DataValue>,
        DataSymbol<Label, Variable, DataValue>,
        RATransition<Location, Variable, Label, DataValue, Guard, Assignment>,
        Configuration<Location, Map<Variable, DataValue>>,
        Boolean>,
    RegisterAutomaton<Location, Variable, Label, DataValue, Guard, Assignment>

/**
 * The base type for mutable *deterministic* register automata.
 *
 * The corresponding concept for finite-state automata is [MutableDFA].
 *
 * @param Location the location type.
 * @param Variable the variable name alphabet.
 * @param Label the label alphabet.
 * @param DataValue the data domain.
 * @param Guard the concrete guard type.
 * @param Assignment the concrete assignment type.
 */
interface MutableDeterministicRegisterAutomaton<
    Location,
    Variable,
    Label,
    DataValue,
    Guard : RAGuard<Variable, DataValue>,
    Assignment : RAAssignment<Variable, DataValue>> :
    DeterministicRegisterAutomaton<Location, Variable, Label, DataValue, Guard, Assignment>,
    MutableDeterministicExtendedAutomaton<
        Location,
        Map<Variable, DataValue>,
        DataSymbol<Label, Variable, DataValue>,
        RATransition<Location, Variable, Label, DataValue, Guard, Assignment>,
        Configuration<Location, Map<Variable, DataValue>>,
        Boolean>,
    MutableRegisterAutomaton<Location, Variable, Label, DataValue, Guard, Assignment>
