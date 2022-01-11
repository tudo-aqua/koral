/*
 * Copyright 2020-2022 The Koral Authors
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

import kotlin.math.min
import kotlin.random.Random
import org.apache.commons.math3.distribution.IntegerDistribution
import org.apache.commons.math3.distribution.RealDistribution
import tools.aqua.koral.automata.register.DataLabel
import tools.aqua.koral.automata.register.RATransition
import tools.aqua.koral.automata.succinct.SuccinctAssignment
import tools.aqua.koral.automata.succinct.SuccinctClause
import tools.aqua.koral.automata.succinct.SuccinctGuard
import tools.aqua.koral.automata.succinct.SuccinctOperator.EQUALS
import tools.aqua.koral.impl.QuickSuccinctNRA
import tools.aqua.koral.impl.RALocation
import tools.aqua.koral.util.combinations

/**
 * Generator for a random sequence of register automata.
 *
 * Note that all distributions are reseeded using the provided random number generator. For parallel
 * determinism, the distributions must not be shared between generators. The generation algorithm
 * works as follows:
 * - determine the label alphabet
 * - determine the number of registers and states
 * - remove the initial state from the next steps
 * - create a spanning tree of transitions to make each state reachable from the second generated
 * state
 * - create random additional transitions between the states
 * - for each transition:
 * - select random clusters of registers and parameters that are tested for equality
 * - create inquality tests between some clusters
 * - create assignments from random registers and parameters to registers
 * - add a single transition from the reserved initial to the second state that overwrites all
 * registers
 *
 * @param DataValue the data type the generated automata use.
 * @param random the random generator to use.
 * @param maximumArity the distribution of the maximum arity of the input labels. This is sampled
 * once per generated RA.
 * @param labelsPerArity the distribution of the number of labels with the same arity. This is
 * sampled once per arity "group", i.e., for 0, 1, …, X([maximumArity]).
 * @param registers the distribution of the number of registers in the RA. This is sampled once per
 * RA.
 * @param locations the distribution of the number of states in the RA. This is sampled once per RA.
 * @param acceptingProportion the distribution of the number of accepting states vs rejecting
 * states. This is sampled once per RA. The number of accepting states is `|Q| *
 * [acceptingProportion]`, rounded to the nearest integer.
 * @param additionalTransitions the distribution of the number of additional transitions (i.e.,
 * those added to the spanning tree). This is sampled once per RA.
 * @param equalityClusters the distribution of the number of equality clusters in a guard, i.e., the
 * groups of registers and variables between which equality constraints are generated. This is
 * sampled one per transition. If the generation runs out of values, the cluster generation is
 * aborted early.
 * @param equalityClusterSize the distribution of the size of equality clusters, i.e., the number of
 * registers and variables in it. This is sampled once per equality cluster. If the generation runs
 * out of values, the cluster generation is aborted early.
 * @param inequalities the distribution of the number of inequalities between clusters in a guard.
 * For each inequality, inequality constraints between all members of two clusters are generated.
 * This is sampled one per transition. If the generation runs out of clusters, the guard generation
 * is aborted early.
 * @param assignmentClusters the distribution of the number of assignment clusters, i.e., the groups
 * of registers and variables that are written to a register. All registers not written to by a
 * cluster keep their value. This is sampled one per transition.
 * @param assignmentClusterSize the distribution of the size of assignment clusters, i.e., the
 * number of registers and variables written by it. This is sampled once per equality cluster. If
 * the generation runs out of values, the assignment generation is aborted early.
 */
class RAGenerator<DataValue>(
    private val random: Random,
    private val maximumArity: IntegerDistribution,
    private val labelsPerArity: IntegerDistribution,
    private val registers: IntegerDistribution,
    private val locations: IntegerDistribution,
    private val acceptingProportion: RealDistribution,
    private val additionalTransitions: IntegerDistribution,
    private val equalityClusters: IntegerDistribution,
    private val equalityClusterSize: IntegerDistribution,
    private val inequalities: IntegerDistribution,
    private val assignmentClusters: IntegerDistribution,
    private val assignmentClusterSize: IntegerDistribution,
) : Iterator<QuickSuccinctNRA<String, String, DataValue>> {

  init {
    acceptingProportion.reseedRandomGenerator(random.nextLong())
    listOf(
        maximumArity,
        labelsPerArity,
        registers,
        locations,
        additionalTransitions,
        equalityClusters,
        equalityClusterSize,
        inequalities,
        assignmentClusters,
        assignmentClusterSize)
        .forEach { it.reseedRandomGenerator(random.nextLong()) }
  }

  override fun hasNext(): Boolean = true

  /**
   * Generate another automaton using the initialized parameter.
   *
   * @return a random RA.
   */
  override fun next(): QuickSuccinctNRA<String, String, DataValue> =
      QuickSuccinctNRA<String, String, DataValue>().apply {
        val registers = generateRegisters()
        val alphabet = generateAlphabet()
        val initializationLabel = generateInitializationLabel(registers.size)

        generateLocations()
        generateInitialTransition(initializationLabel, registers)
        generateMSTTransitions(alphabet, registers)
        generateAdditionalTransitions(alphabet, registers)
      }

  /**
   * Generate a random number of register names.
   *
   * @return the register set.
   */
  private fun generateRegisters(): List<String> = List(registers.sample()) { "x$it" }

  /**
   * Generate a random input alphabet matching the given [dataType].
   *
   * @param dataType the data type of the inputs.
   * @return the input alphabet.
   */
  private fun generateAlphabet(): Set<DataLabel<String, String>> = buildSet {
    repeat(maximumArity.sample()) { arity ->
      val parameters = List(arity) { "p_$it" }
      repeat(labelsPerArity.sample()) { instance ->
        this += DataLabel("l_${arity}_${instance}", parameters)
      }
    }
  }

  /**
   * Generate an additional input alphabet for the special register initialization transition.
   *
   * @return the input symbol.
   */
  private fun generateInitializationLabel(arity: Int) = DataLabel("init", List(arity) { "p_$it" })

  /**
   * Generate a random state set and add it to the automaton.
   *
   * @receiver the automaton under construction.
   */
  private fun QuickSuccinctNRA<String, String, DataValue>.generateLocations() {
    val localAcceptingProportion = acceptingProportion.sample()
    addInitialState(emptyMap())
    repeat(this@RAGenerator.locations.sample()) {
      addLocation(random.nextDouble() < localAcceptingProportion)
    }
  }

  /**
   * Generate the spanning-tree-based initial transition structure.
   *
   * @receiver the automaton under construction.
   */
  private fun QuickSuccinctNRA<String, String, DataValue>.generateInitialTransition(
      initializer: DataLabel<String, String>,
      registers: List<String>
  ) {
    val initial = initialStates.single().location
    addLocationTransition(
        initial,
        RATransition(
            locations.first { it != initial },
            initializer,
            SuccinctGuard(),
            SuccinctAssignment((registers zip initializer.parameterNames).toMap())))
  }

  /**
   * Generate the spanning-tree-based initial transition structure.
   *
   * @receiver the automaton under construction.
   */
  private fun QuickSuccinctNRA<String, String, DataValue>.generateMSTTransitions(
      alphabet: Collection<DataLabel<String, String>>,
      registers: List<String>
  ) {
    val initial = initialStates.single().location
    val firstNonInitial = getLocationTransitions(initial).single().destination
    val otherNonInitial =
        locations.filter { it != initial && it != firstNonInitial }.shuffled(random)

    val connected = mutableListOf(firstNonInitial)

    otherNonInitial.forEach { destination ->
      val source = connected.random(random)
      connected += destination
      generateTransition(source, destination, alphabet, registers)
    }
  }

  /**
   * Generate the random transition structure.
   *
   * @param states the state objects.
   * @return the tuples that can be refined into transition in the next step.
   */
  private fun QuickSuccinctNRA<String, String, DataValue>.generateAdditionalTransitions(
      alphabet: Collection<DataLabel<String, String>>,
      registers: List<String>
  ) {
    repeat(additionalTransitions.sample()) {
      generateTransition(locations.random(random), locations.random(random), alphabet, registers)
    }
  }

  /**
   * Generate transitions based on the transition structure and add them to the automaton.
   *
   * @param automaton the automaton under construction.
   * @param alphabet the input alphabet.
   * @param registers the automaton's register set.
   * @param transitionStructure the transition structure to refine.
   */
  private fun QuickSuccinctNRA<String, String, DataValue>.generateTransition(
      source: RALocation,
      destination: RALocation,
      alphabet: Collection<DataLabel<String, String>>,
      registers: List<String>
  ) {
    val label = alphabet.random(random)
    addLocationTransition(
        source,
        RATransition(
            destination,
            label,
            generateGuard(label.parameterNames, registers),
            generateAssignment(label.parameterNames, registers)))
  }

  /**
   * Generate a random guard.
   *
   * @param input the alphabet symbol for the current transition.
   * @param registers the automaton's register set.
   * @return the generated guard.
   */
  private fun generateGuard(
      parameters: Collection<String>,
      registers: Collection<String>
  ): SuccinctGuard<String, DataValue> {
    val variables = (registers + parameters).shuffled(random).toMutableList()

    val equalityClusters = buildList {
      repeat(equalityClusters.sample()) {
        if (variables.size > 1) {
          val size = equalityClusterSize.sample().coerceIn(1, variables.size)
          val members = variables.take(size).toSet()
          add(members)
          variables.removeAll(members)
        }
      }
    }

    val equalityClauses =
        equalityClusters.flatMap {
          it.zipWithNext().map { (left, right) ->
            SuccinctClause<String, DataValue>(left, EQUALS, right)
          }
        }

    val nInequalities =
        min(inequalities.sample(), equalityClusters.size * (equalityClusters.size - 1) / 2)
    val inequalities = equalityClusters.combinations().shuffled(random).take(nInequalities)
    val inequalityClauses =
        inequalities.map { (leftCluster, rightCluster) ->
          SuccinctClause<String, DataValue>(leftCluster.first(), EQUALS, rightCluster.first())
        }

    return SuccinctGuard(equalityClauses + inequalityClauses)
  }

  /**
   * Generate a random assignment.
   *
   * @param input the alphabet symbol for the current transition.
   * @param registers the automaton's register set.
   * @return the generated assignment.
   */
  private fun generateAssignment(
      parameters: Collection<String>,
      registers: Collection<String>
  ): SuccinctAssignment<String, DataValue> {

    val sources = (registers + parameters)
    val targets = registers.shuffled(random).toMutableList()

    val nAssignmentClusters = min(assignmentClusters.sample(), sources.size)
    val assignment =
        registers.associateByTo(mutableMapOf()) { it } +
            targets
                .take(nAssignmentClusters)
                .flatMap { target ->
                  val clusterSize = min(assignmentClusterSize.sample(), targets.size)

                  sources.shuffled(random).take(clusterSize).map { source -> target to source }
                }
                .toMap()

    return SuccinctAssignment(assignment)
  }
}
