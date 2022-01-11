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

import tools.aqua.koral.automata.extended.*

/**
 * The base type for shrinkable register automata. Shrinkablility implies that the automaton can be
 * modified programmatically, including additions and removals.
 *
 * @param Location the location type.
 * @param Variable the variable name alphabet.
 * @param Label the label alphabet.
 * @param DataValue the data domain.
 * @param Guard the concrete guard type.
 * @param Assignment the concrete assignment type.
 */
interface ShrinkableRegisterAutomaton<
    Location,
    Variable,
    Label,
    DataValue,
    Guard : RAGuard<Variable, DataValue>,
    Assignment : RAAssignment<Variable, DataValue>> :
    ShrinkableExtendedAutomaton<
        Location,
        Map<Variable, DataValue>,
        DataSymbol<Label, Variable, DataValue>,
        RATransition<Location, Variable, Label, DataValue, Guard, Assignment>,
        Configuration<Location, Map<Variable, DataValue>>,
        Boolean>,
    MutableRegisterAutomaton<Location, Variable, Label, DataValue, Guard, Assignment>

/**
 * The base type for shrinkable *deterministic* register automata.
 *
 * @param Location the location type.
 * @param Variable the variable name alphabet.
 * @param Label the label alphabet.
 * @param DataValue the data domain.
 * @param Guard the concrete guard type.
 * @param Assignment the concrete assignment type.
 */
interface ShrinkableDeterministicRegisterAutomaton<
    Location,
    Variable,
    Label,
    DataValue,
    Guard : RAGuard<Variable, DataValue>,
    Assignment : RAAssignment<Variable, DataValue>> :
    MutableDeterministicRegisterAutomaton<Location, Variable, Label, DataValue, Guard, Assignment>,
    ShrinkableDeterministicExtendedAutomaton<
        Location,
        Map<Variable, DataValue>,
        DataSymbol<Label, Variable, DataValue>,
        RATransition<Location, Variable, Label, DataValue, Guard, Assignment>,
        Configuration<Location, Map<Variable, DataValue>>,
        Boolean>,
    ShrinkableRegisterAutomaton<Location, Variable, Label, DataValue, Guard, Assignment>
