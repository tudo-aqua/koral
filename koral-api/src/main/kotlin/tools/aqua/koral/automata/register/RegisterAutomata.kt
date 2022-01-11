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

package tools.aqua.koral.automata.register

import net.automatalib.automata.fsa.DFA
import net.automatalib.automata.fsa.NFA
import net.automatalib.ts.acceptors.DeterministicAcceptorTS
import net.automatalib.words.Alphabet
import tools.aqua.koral.automata.acceptors.FiniteLocationAcceptor
import tools.aqua.koral.automata.extended.Configuration
import tools.aqua.koral.automata.extended.DeterministicSuffixOutputExtendedAutomaton
import tools.aqua.koral.automata.extended.ExtendedLocationTransition
import tools.aqua.koral.automata.extended.UniversalDeterministicExtendedAutomaton

/**
 * A data label combines a label (from a finite label alphabet) with parameter names. They also
 * serve as factories for [DataSymbol]s.
 *
 * @param Label the label alphabet.
 * @param Variable the variable name alphabet.
 * @param label the data label.
 * @param parameterNames the names of the parameters. May be empty.
 */
data class DataLabel<out Label, Variable>(val label: Label, val parameterNames: List<Variable>) {

  /** The label's arity is the number of parameters it has. */
  val arity
    get() = parameterNames.size

  /**
   * Convenience constructor using varargs.
   *
   * @param label the data label.
   * @param parameterNames the names of the parameters. May be empty.
   */
  constructor(label: Label, vararg parameterNames: Variable) : this(label, parameterNames.toList())

  /**
   * Create a data symbol with the given data values. The number of [values] must be identical to
   * the [arity]. The *n*-th value is assigned to the *n*-th variable name
   *
   * @param DataValue the type of the data values.
   * @param values the data values.
   * @return a data symbol labeling the [values].
   */
  fun <DataValue> createSymbol(vararg values: DataValue): DataSymbol<Label, Variable, DataValue> =
      createSymbol(values.toList())

  /**
   * Create a data symbol with the given data values. The number of [values] must be identical to
   * the [arity]. The *n*-th value is assigned to the *n*-th variable name
   *
   * @param DataValue the type of the data values.
   * @param values the data values.
   * @return a data symbol labeling the [values].
   */
  fun <DataValue> createSymbol(values: List<DataValue>): DataSymbol<Label, Variable, DataValue> {
    check(values.size == arity) {
      "Incorrect number of parameters: ${values.size} given, but $arity required"
    }
    return DataSymbol(this, values)
  }
}

/**
 * A data symbol combines a [DataLabel] with a matching number of concrete data values from a data
 * domain. They are constructed using [DataLabel.createSymbol].
 * @param Label the label alphabet.
 * @param Variable the variable name alphabet.
 * @param DataSymbol the data domain.
 * @param label the data label.
 * @param data the data values.
 */
data class DataSymbol<out Label, Variable, out DataSymbol>
internal constructor(val label: DataLabel<Label, Variable>, val data: List<DataSymbol>) {
  /**
   * Transform this symbol into a valuation, i.e., a mapping from variable name to value.
   * @return the valuation.
   */
  fun toValuation(): Map<Variable, DataSymbol> = (label.parameterNames zip data).toMap()
}

interface RAGuard<Variable, DataValue> {
  fun isSatisfiedBy(valuation: Map<Variable, DataValue>): Boolean
}

interface RAAssignment<Variable, DataValue> {
  fun computeFrom(valuation: Map<Variable, DataValue>): Map<Variable, DataValue>
}

/**
 * A register automaton transition combines a target location with
 * - a label that must be shared by an accepted symbol,
 * - a guard that must be satisfied by the correct register valuation and the input data, and
 * - an assignment that describes updates to the register valuation.
 *
 * @param Location the automaton's location type.
 * @param Variable the variable name type, used for naming registers and parameters.
 * @param Label the label alphabet.
 * @param DataValue the data domain.
 * @param Guard the type of guard. Must implement a function from valuations to satisfaction.
 * @param Assignment the type of assignment. Must implement a function from valuations to register
 * valuations.
 *
 * @param destination the location the transition leads to. This is static information.
 * @param dataLabel the data label that is matched by the transition.
 * @param guard the guard function. This must accept valuations over the automaton's registers and
 * [dataLabel]'s parameters.
 * @param assignment the guard function. This must accept valuations over the automaton's registers
 * and [dataLabel]'s parameters and return register valuations.
 */
data class RATransition<
    Location,
    Variable,
    Label,
    DataValue,
    Guard : RAGuard<Variable, DataValue>,
    Assignment : RAAssignment<Variable, DataValue>>(
    val destination: Location,
    val dataLabel: DataLabel<Label, Variable>,
    val guard: Guard,
    val assignment: Assignment
) :
    ExtendedLocationTransition<
        Map<Variable, DataValue>,
        DataSymbol<Label, Variable, DataValue>,
        Configuration<Location, Map<Variable, DataValue>>> {

  /**
   * Compute the transition or reject it. The semantics are:
   * - If [dataLabel] is distinct from [input]'s label, return `null` (i.e., reject).
   * - If [guard] does not accept the register valuation [valuation] combined with [input]'s
   * parameter valuation, return `null` (i.e., reject).
   * - Else, apply [assignment] to the combined valuations and return the combination of
   * [destination] and the result.
   *
   * @param valuation the source register valuation.
   * @param input the input symbol.
   * @return `null` or the next configuration.
   */
  override fun getTransitionOrNull(
      valuation: Map<Variable, DataValue>,
      input: DataSymbol<Label, Variable, DataValue>
  ): Configuration<Location, Map<Variable, DataValue>>? {
    if (input.label != dataLabel) return null
    val sourceValuation = input.toValuation() + valuation
    if (guard.isSatisfiedBy(sourceValuation).not()) return null
    return Configuration(destination, assignment.computeFrom(sourceValuation))
  }
}

/**
 * The base type for register automata. Register automata possess a finite set of registers that can
 * either be empty or a value from a data domain. Acceptance is classic non-deterministic
 * acceptance: if one path (i.e., one location in a state set) accepts, the automaton does.
 *
 * The corresponding concept for finite-state automata is [NFA].
 *
 * @param Location the location type.
 * @param Variable the variable name alphabet.
 * @param Label the label alphabet.
 * @param DataValue the data domain.
 * @param Guard the concrete guard type.
 * @param Assignment the concrete assignment type.
 */
interface RegisterAutomaton<
    Location,
    Variable,
    Label,
    DataValue,
    Guard : RAGuard<Variable, DataValue>,
    Assignment : RAAssignment<Variable, DataValue>> :
    FiniteLocationAcceptor<
        Location,
        Map<Variable, DataValue>,
        DataSymbol<Label, Variable, DataValue>,
        RATransition<Location, Variable, Label, DataValue, Guard, Assignment>> {

  /** The data labels used by the automaton. May overapproximate the actual labels. */
  val labels: Alphabet<DataLabel<Label, Variable>>

  /** The automaton's register set. */
  val registers: Collection<Variable>

  override fun isAcceptingLocations(locations: Collection<Location>): Boolean =
      locations.any(::isAcceptingLocation)

  /** Get a [location]'s RA transitions restricted to a specific transition [label]. */
  fun getLocationTransitions(
      location: Location,
      label: DataLabel<Label, Variable>
  ): Collection<RATransition<Location, Variable, Label, DataValue, Guard, Assignment>> =
      getLocationTransitions(location).filter { it.dataLabel == label }
}

/**
 * The base type for `deterministic* register automata. Acceptance checks for multiple states and
 * yields an error if encounteted.
 *
 * The corresponding concept for finite-state automata is [DFA].
 *
 * @param Location the location type.
 * @param Variable the variable name alphabet.
 * @param Label the label alphabet.
 * @param DataValue the data domain.
 * @param Guard the concrete guard type.
 * @param Assignment the concrete assignment type
 */
interface DeterministicRegisterAutomaton<
    Location,
    Variable,
    Label,
    DataValue,
    Guard : RAGuard<Variable, DataValue>,
    Assignment : RAAssignment<Variable, DataValue>> :
    DeterministicAcceptorTS<
        Configuration<Location, Map<Variable, DataValue>>, DataSymbol<Label, Variable, DataValue>>,
    DeterministicSuffixOutputExtendedAutomaton<
        Location,
        Map<Variable, DataValue>,
        DataSymbol<Label, Variable, DataValue>,
        RATransition<Location, Variable, Label, DataValue, Guard, Assignment>,
        Configuration<Location, Map<Variable, DataValue>>,
        Boolean>,
    RegisterAutomaton<Location, Variable, Label, DataValue, Guard, Assignment>,
    UniversalDeterministicExtendedAutomaton<
        Location,
        Map<Variable, DataValue>,
        DataSymbol<Label, Variable, DataValue>,
        RATransition<Location, Variable, Label, DataValue, Guard, Assignment>,
        Configuration<Location, Map<Variable, DataValue>>,
        Boolean> {
  override fun isAccepting(
      states: Collection<Configuration<Location, Map<Variable, DataValue>>>
  ): Boolean = super<DeterministicAcceptorTS>.isAccepting(states)

  override fun computeOutput(input: Iterable<DataSymbol<Label, Variable, DataValue>>): Boolean =
      super<DeterministicAcceptorTS>.computeOutput(input)

  override fun computeStateOutput(
      state: Configuration<Location, Map<Variable, DataValue>>,
      input: Iterable<DataSymbol<Label, Variable, DataValue>>
  ): Boolean = getSuccessor(state, input)?.let { isAccepting(it) } ?: false

  override fun computeSuffixOutput(
      prefix: Iterable<DataSymbol<Label, Variable, DataValue>>,
      suffix: Iterable<DataSymbol<Label, Variable, DataValue>>
  ): Boolean = super<DeterministicSuffixOutputExtendedAutomaton>.computeSuffixOutput(prefix, suffix)
}
