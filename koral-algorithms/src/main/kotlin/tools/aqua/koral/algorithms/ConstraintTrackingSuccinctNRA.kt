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

import com.google.common.collect.Multimap
import com.google.common.collect.MultimapBuilder
import net.automatalib.words.Alphabet
import tools.aqua.koral.automata.extended.Configuration
import tools.aqua.koral.automata.register.DataLabel
import tools.aqua.koral.automata.register.RAAssignment
import tools.aqua.koral.automata.register.RAGuard
import tools.aqua.koral.automata.register.RATransition
import tools.aqua.koral.automata.register.RegisterAutomaton
import tools.aqua.koral.automata.succinct.SuccinctAssignment
import tools.aqua.koral.automata.succinct.SuccinctClause
import tools.aqua.koral.automata.succinct.SuccinctGuard
import tools.aqua.koral.automata.succinct.SuccinctOperator
import tools.aqua.koral.automata.succinct.SuccinctOperator.EQUALS
import tools.aqua.koral.automata.succinct.SuccinctOperator.NOT_EQUALS
import tools.aqua.koral.automata.succinct.SuccinctRegisterAutomaton
import tools.aqua.koral.automata.succinct.SuccinctTransition
import tools.aqua.koral.impl.QuickSuccinctNRA
import tools.aqua.koral.impl.RALocation
import tools.aqua.koral.util.AbstractConstraint
import tools.aqua.koral.util.AssignmentConstraint
import tools.aqua.koral.util.GuardConstraint
import tools.aqua.koral.util.InfeasibleConstraintException
import tools.aqua.koral.util.LocationConstraint
import tools.aqua.koral.util.MutableCanonicalSet
import tools.aqua.koral.util.ProjectionConstraint
import tools.aqua.koral.util.Relation
import tools.aqua.koral.util.Relation.DISTINCT
import tools.aqua.koral.util.Relation.EQUAL
import tools.aqua.koral.util.Tag.DESTINATION_REGISTER
import tools.aqua.koral.util.Tag.PARAMETER
import tools.aqua.koral.util.Tag.SOURCE_REGISTER
import tools.aqua.koral.util.addIntern
import tools.aqua.koral.util.closeTransitive
import tools.aqua.koral.util.combinations
import tools.aqua.koral.util.index
import tools.aqua.koral.util.plusAssign
import tools.aqua.koral.util.putAndCheck
import tools.aqua.koral.util.removeFirst

data class ConstraintLocation<Location, Variable>(
    val constraint: LocationConstraint<Variable>,
    val originalLocation: Location
)

data class ConstraintGuard<Variable, DataValue>(
    val constraint: GuardConstraint<Variable>,
    val originalGuard: SuccinctGuard<Variable, DataValue>
) : RAGuard<Variable, DataValue> {
  override fun isSatisfiedBy(valuation: Map<Variable, DataValue>): Boolean =
      runCatching { constraint.copyOf() += valuation }.isSuccess
}

data class ConstraintAssignment<Variable, DataValue>(
    val constraint: AssignmentConstraint<Variable>,
    val originalAssignment: SuccinctAssignment<Variable, DataValue>
) : RAAssignment<Variable, DataValue> {
  override fun computeFrom(valuation: Map<Variable, DataValue>): Map<Variable, DataValue> {
    return buildMap {
      constraint.rows.forEach { target ->
        val from = constraint.columns.first { source -> constraint[source, target] == EQUAL }
        this[target.variable] = valuation.getValue(from.variable)
      }
    }
  }
}

typealias ConstraintTransition<Location, Variable, Label, DataValue> =
    RATransition<
        ConstraintLocation<Location, Variable>,
        Variable,
        Label,
        DataValue,
        ConstraintGuard<Variable, DataValue>,
        ConstraintAssignment<Variable, DataValue>>

private data class GraphStructure<Location, Variable, Label, DataValue>(
    private val outgoingTransitions:
        Multimap<
            ConstraintLocation<Location, Variable>,
            ConstraintTransition<Location, Variable, Label, DataValue>> =
        linkedHashSetMultimap(),
    private val loopTransitions:
        Multimap<
            ConstraintLocation<Location, Variable>,
            ConstraintTransition<Location, Variable, Label, DataValue>> =
        linkedHashSetMultimap(),
    private val incomingTransitionsWithSource:
        Multimap<
            ConstraintLocation<Location, Variable>,
            SourceWithTransition<Location, Variable, Label, DataValue>> =
        linkedHashSetMultimap(),
) {
  fun getAll(): Set<SourceWithTransition<Location, Variable, Label, DataValue>> =
      incomingTransitionsWithSource.values().toSet() +
          loopTransitions.values().mapTo(mutableSetOf()) {
            SourceWithTransition(it.destination, it)
          }

  fun getIncomingWithSource(
      source: ConstraintLocation<Location, Variable>
  ): Set<SourceWithTransition<Location, Variable, Label, DataValue>> =
      incomingTransitionsWithSource[source].toSet()

  fun getLoops(
      source: ConstraintLocation<Location, Variable>
  ): Set<ConstraintTransition<Location, Variable, Label, DataValue>> =
      loopTransitions[source].toSet()

  fun getOutgoing(
      source: ConstraintLocation<Location, Variable>
  ): Set<ConstraintTransition<Location, Variable, Label, DataValue>> =
      outgoingTransitions[source].toSet()

  fun put(
      source: ConstraintLocation<Location, Variable>,
      transition: ConstraintTransition<Location, Variable, Label, DataValue>
  ): Boolean {
    return if (source == transition.destination) {
      loopTransitions.put(source, transition)
    } else {
      outgoingTransitions.put(source, transition)
      incomingTransitionsWithSource.put(
          transition.destination, SourceWithTransition(source, transition))
    }
  }

  fun remove(
      source: ConstraintLocation<Location, Variable>,
      transition: ConstraintTransition<Location, Variable, Label, DataValue>
  ): Boolean {
    return if (source == transition.destination) {
      loopTransitions.remove(source, transition)
    } else {
      outgoingTransitions.remove(source, transition)
      incomingTransitionsWithSource.remove(
          transition.destination, SourceWithTransition(source, transition))
    }
  }

  fun remove(
      source: ConstraintLocation<Location, Variable>
  ): Set<SourceWithTransition<Location, Variable, Label, DataValue>> = buildSet {
    outgoingTransitions.removeAll(source).forEach { transition ->
      val withSource = SourceWithTransition(source, transition)
      incomingTransitionsWithSource[transition.destination].remove(withSource)
      this += withSource
    }
    loopTransitions.removeAll(source).forEach { this += SourceWithTransition(it.destination, it) }
    incomingTransitionsWithSource.removeAll(source).forEach { withSource ->
      val (predecessor, transition) = withSource
      outgoingTransitions[predecessor].remove(transition)
      this += withSource
    }
  }
}

private data class SourceWithTransition<Location, Variable, Label, DataValue>(
    val source: ConstraintLocation<Location, Variable>,
    val transition: ConstraintTransition<Location, Variable, Label, DataValue>
)

private fun <K, V> linkedHashSetMultimap() =
    MultimapBuilder.linkedHashKeys().linkedHashSetValues().build<K, V>()

class ConstraintTrackingSuccinctNRA<Location, Variable, Label, DataValue>(
    private val source: SuccinctRegisterAutomaton<Location, Variable, Label, DataValue>,
) :
    RegisterAutomaton<
        ConstraintLocation<Location, Variable>,
        Variable,
        Label,
        DataValue,
        ConstraintGuard<Variable, DataValue>,
        ConstraintAssignment<Variable, DataValue>> {

  override val labels: Alphabet<DataLabel<Label, Variable>> = source.labels
  override val registers: Collection<Variable> = source.registers

  private val registerIndex = registers.index()
  private val parameterIndices = labels.associate { it.label to it.parameterNames.index() }
  private val emptyLocationConstraint = LocationConstraint(registerIndex)

  private val initialConstraintsByLocation =
      linkedHashSetMultimap<
          Location, Pair<Map<Variable, DataValue>, LocationConstraint<Variable>>>()
          .also { map ->
            source.initialStates.forEach { (location, valuation) ->
              val constraint =
                  LocationConstraint(registerIndex).also {
                    it += valuation
                    it.closeTransitive()
                  }
              map.put(location, valuation to constraint)
            }
          }

  private val locationsMutable: MutableCanonicalSet<ConstraintLocation<Location, Variable>> =
      source.locations.mapTo(MutableCanonicalSet()) { location ->
        ConstraintLocation(emptyLocationConstraint, location)
      }

  private val graph = GraphStructure<Location, Variable, Label, DataValue>()

  init {
    locationsMutable.forEach { constraintSource ->
      source
          .getLocationTransitions(constraintSource.originalLocation)
          .mapNotNull {
            it.toConstraintTransition(
                emptyLocationConstraint, locationsMutable, parameterIndices, registerIndex)
          }
          .forEach { graph.put(constraintSource, it) }
    }
  }

  private val tainted = graph.getAll().toMutableSet()

  override val locations: Collection<ConstraintLocation<Location, Variable>>
    get() = locationsMutable

  override fun isAcceptingLocation(location: ConstraintLocation<Location, Variable>): Boolean =
      source.isAcceptingLocation(location.originalLocation)

  override fun getLocationTransitions(
      location: ConstraintLocation<Location, Variable>
  ): Collection<
      RATransition<
          ConstraintLocation<Location, Variable>,
          Variable,
          Label,
          DataValue,
          ConstraintGuard<Variable, DataValue>,
          ConstraintAssignment<Variable, DataValue>>> =
      graph.getLoops(location) + graph.getOutgoing(location)

  override fun getInitialStates():
      Set<Configuration<ConstraintLocation<Location, Variable>, Map<Variable, DataValue>>> =
      buildSet {
    locationsMutable.forEach { location ->
      val initialConstraints = initialConstraintsByLocation[location.originalLocation]
      initialConstraints.forEach { (valuation, constraint) ->
        try {
          location.constraint.copyOf() += constraint
          this += Configuration(location, valuation)
        } catch (_: InfeasibleConstraintException) {}
      }
    }
  }

  fun makeHistoryIndependent() {
    while (tainted.isNotEmpty()) {
      val (source, transition) = tainted.removeFirst()
      makeHoare(source, transition)
    }

    pruneDeadLocations()
  }

  private fun makeHoare(
      source: ConstraintLocation<Location, Variable>,
      transition: ConstraintTransition<Location, Variable, Label, DataValue>
  ) {
    val (destination, _, guard, assignment) = transition
    graph.remove(source, transition)

    val constraint =
        try {
          ProjectionConstraint.assemble(
                  source = source.constraint,
                  guard = guard.constraint,
                  assignment = assignment.constraint,
                  destination = destination.constraint)
              .also { it.closeTransitive() }
        } catch (_: InfeasibleConstraintException) {
          return // discard infeasible
        }

    val newSource = getOrSplit(source, constraint.toSourceConstraint())
    val newTransition = transition.copy(guard = guard.copy(constraint.toGuardConstraint()))
    graph.put(newSource, newTransition)
    if (source == newSource) return // transition is already Hoare, no need to do more

    if (source == destination) {
      val loopTransition = transition.copy(destination = newSource)
      graph.put(source, loopTransition)
      tainted += SourceWithTransition(source, loopTransition)
    }

    if (source.constraint != emptyLocationConstraint && graph.getOutgoing(source).isEmpty()) {
        // terminal location with too-strong constraint
      locationsMutable.remove(source)
      val removed = graph.remove(source)
      tainted -= removed
    }
  }

  private fun getOrSplit(
      source: ConstraintLocation<Location, Variable>,
      constraint: LocationConstraint<Variable>
  ): ConstraintLocation<Location, Variable> {
    val (isNew, split) = locationsMutable.addIntern(source.copy(constraint = constraint))
    if (isNew) {
      graph.getIncomingWithSource(source).forEach { (predecessor, incoming) ->
        val newIncoming = incoming.copy(destination = split)
        graph.put(predecessor, newIncoming)
        tainted += SourceWithTransition(predecessor, newIncoming)
      }
      graph.getLoops(source).forEach { loop -> // if we are handling a loop, it is currently removed
        val splitLoop = loop.copy(destination = split)
        graph.put(source, splitLoop)
        tainted += SourceWithTransition(source, splitLoop)
      }
    }
    return split
  }

  private fun pruneDeadLocations() {
    val reached = computeReachableLocations()

    val locations = locationsMutable.iterator()
    for (location in locations) {
      if (location in reached) continue
      locations.remove()
      val removed = graph.remove(location)
      tainted -= removed
    }
  }

  fun toSuccinctNRA(): SuccinctRegisterAutomaton<RALocation, Variable, Label, DataValue> {
    return QuickSuccinctNRA<Variable, Label, DataValue>().also { ra ->
      val locationMap = locations.associateWith { ra.addLocation(isAcceptingLocation(it)) }
      initialStates.forEach { (location, valuation) ->
        ra.addInitialState(Configuration(locationMap.getValue(location), valuation))
      }
      locations.flatMap { source -> getLocationTransitions(source).map { source to it } }.forEach {
          (source, transition) ->
        val guard =
            SuccinctGuard(
                transition.guard.constraint.entrySet().mapNotNull { (left, right, relation) ->
                  relation.toOperator()?.let { operator ->
                    SuccinctClause<Variable, DataValue>(left.variable, operator, right.variable)
                  }
                })
        ra.addLocationTransition(
            locationMap.getValue(source),
            RATransition(
                locationMap.getValue(transition.destination),
                transition.dataLabel,
                guard,
                transition.assignment.originalAssignment))
      }
    }
  }
}

private fun <Location, Variable, Label, DataValue> SuccinctTransition<
    Location, Variable, Label, DataValue>.toConstraintTransition(
    emptyLocationConstraint: LocationConstraint<Variable>,
    locationsMutable: MutableCanonicalSet<ConstraintLocation<Location, Variable>>,
    parameterIndices: Map<Label, Map<Variable, Int>>,
    registerIndex: Map<Variable, Int>
): ConstraintTransition<Location, Variable, Label, DataValue>? {
  val (destination, dataLabel, guard, assignment) = this
  val constraintDestination =
      locationsMutable.getValue(ConstraintLocation(emptyLocationConstraint, destination))
  val parameterIndex = parameterIndices.getValue(dataLabel.label)

  val guardConstraint = guard.toConstraint(registerIndex, parameterIndex) ?: return null
  val assignmentConstraint = assignment.toConstraint(registerIndex, parameterIndex)

  return ConstraintTransition(
      constraintDestination,
      dataLabel,
      ConstraintGuard(guardConstraint, guard),
      ConstraintAssignment(assignmentConstraint, assignment))
}

private fun <Variable> SuccinctGuard<Variable, *>.toConstraint(
    registerIndex: Map<Variable, Int>,
    parameterIndex: Map<Variable, Int>
): GuardConstraint<Variable>? =
    try {
      GuardConstraint(registerIndex, parameterIndex).also {
        clauses.forEach { (left, operator, right) ->
          val leftTag = if (left in registerIndex) SOURCE_REGISTER(left) else PARAMETER(left)
          val rightTag = if (right in registerIndex) SOURCE_REGISTER(right) else PARAMETER(right)
          it.putAndCheck(leftTag, rightTag, operator.toRelation())
        }
        it.closeTransitive()
      }
    } catch (_: InfeasibleConstraintException) {
      null
    }

private fun <Variable> SuccinctAssignment<Variable, *>.toConstraint(
    registerIndex: Map<Variable, Int>,
    parameterIndex: Map<Variable, Int>
): AssignmentConstraint<Variable> =
    AssignmentConstraint(registerIndex, parameterIndex).also {
      mapping.entries.forEach { (target, source) ->
        assert(target in registerIndex)
        val targetTag = DESTINATION_REGISTER(target)
        val sourceTag = if (source in registerIndex) SOURCE_REGISTER(source) else PARAMETER(source)
        it.putAndCheck(targetTag, sourceTag, EQUAL)
      }
    }

private fun SuccinctOperator.toRelation(): Relation =
    when (this) {
      EQUALS -> EQUAL
      NOT_EQUALS -> DISTINCT
    }

private fun Relation.toOperator(): SuccinctOperator? =
    when (this) {
      EQUAL -> EQUALS
      DISTINCT -> EQUALS
      else -> null
    }

private operator fun <Variable> AbstractConstraint<Variable>.plusAssign(
    valuation: Map<Variable, *>
) {
  val equivalenceClasses = valuation.entries.groupBy { it.value }.values
  equivalenceClasses.forEach { equivalenceClass ->
    equivalenceClass.map { it.key }.zipWithNext().forEach { (left, right) ->
      putAndCheck(SOURCE_REGISTER(left), SOURCE_REGISTER(right), EQUAL)
    }
  }
  equivalenceClasses.map { it.first().key }.combinations().forEach { (left, right) ->
    putAndCheck(SOURCE_REGISTER(left), SOURCE_REGISTER(right), DISTINCT)
  }
}
