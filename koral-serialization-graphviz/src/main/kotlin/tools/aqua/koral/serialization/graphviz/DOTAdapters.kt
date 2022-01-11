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

package tools.aqua.koral.serialization.graphviz

import com.github.h0tk3y.betterParse.grammar.parseToEnd
import java.io.InputStream
import java.io.OutputStream
import java.util.function.BiFunction
import java.util.function.Function as MonoFunction
import kotlinx.html.stream.createHTML
import kotlinx.html.table
import kotlinx.html.td
import kotlinx.html.tr
import net.automatalib.serialization.ModelDeserializer
import net.automatalib.serialization.ModelSerializer
import org.jgrapht.graph.DirectedPseudograph
import org.jgrapht.nio.Attribute
import org.jgrapht.nio.AttributeType.BOOLEAN
import org.jgrapht.nio.AttributeType.HTML
import org.jgrapht.nio.AttributeType.STRING
import org.jgrapht.nio.DefaultAttribute
import org.jgrapht.nio.dot.DOTExporter
import org.jgrapht.nio.dot.DOTImporter
import tools.aqua.koral.automata.extended.Configuration
import tools.aqua.koral.automata.register.DataLabel
import tools.aqua.koral.automata.register.RATransition
import tools.aqua.koral.automata.succinct.SuccinctAssignment
import tools.aqua.koral.automata.succinct.SuccinctClause
import tools.aqua.koral.automata.succinct.SuccinctGuard
import tools.aqua.koral.automata.succinct.SuccinctOperator.EQUALS
import tools.aqua.koral.automata.succinct.SuccinctOperator.NOT_EQUALS
import tools.aqua.koral.automata.succinct.SuccinctRegisterAutomaton
import tools.aqua.koral.impl.QuickSuccinctNRA
import tools.aqua.koral.impl.RALocation

private const val ACCEPTING = "accepting"
private const val ASSIGNMENT = "assignment"
private const val DATA_LABEL = "datalabel"
private const val GUARD = "guard"
private const val INITIAL = "initial"

private const val EDGE_LABEL = "label"

private data class DOTLocation(
    val name: String,
    val initialValuations: Set<Map<String, Int>>,
    val isAccepting: Boolean
)

private object DOTLocationDeserializer : BiFunction<String, Map<String, Attribute>, DOTLocation> {
  override fun apply(name: String, attributes: Map<String, Attribute>): DOTLocation {
    val initial =
        DOTInitializationParser.parseToEnd(attributes[INITIAL]?.value ?: "").assignments.mapTo(
                mutableSetOf()) {
          it.assignments.associate { (target, source) -> target.name to source.value }
        }

    val accepting = attributes[ACCEPTING]?.value.toBoolean()

    val unquotedName = name.removeSurrounding("\"").replace("\\\"", "\"")

    return DOTLocation(unquotedName, initial, accepting)
  }
}

private object DOTLocationSerializer : MonoFunction<DOTLocation, Map<String, Attribute>> {
  override fun apply(location: DOTLocation): Map<String, Attribute> = buildMap {
    if (location.isAccepting) {
      this[ACCEPTING] = DefaultAttribute(true.toString(), BOOLEAN)
    }

    if (location.initialValuations.isNotEmpty()) {
      this[INITIAL] =
          DefaultAttribute(
              location.initialValuations.joinToString(",", "{", "}") {
                it.entries.joinToString("; ") { (target, source) -> "$target := $source" }
              },
              STRING)
    }
  }
}

private data class DOTTransition(
    val label: DataLabel<String, String>,
    val guard: SuccinctGuard<String, Int>,
    val assignment: SuccinctAssignment<String, Int>
) {
  //  jgraph expects edge objects with difference source and target to be nonequal
  override fun equals(other: Any?): Boolean = this === other
  override fun hashCode(): Int = System.identityHashCode(this)
}

private object DOTTransitionDeserializer : MonoFunction<Map<String, Attribute>, DOTTransition> {
  override fun apply(attributes: Map<String, Attribute>): DOTTransition {
    val label = DOTLabelParser.parseToEnd(attributes.getValue(DATA_LABEL).value)

    val guard =
        SuccinctGuard<String, Int>(
            DOTGuardParser.parseToEnd(attributes[GUARD]?.value ?: "").comparisons.map {
              when (it) {
                is Equals -> SuccinctClause(it.left.name, EQUALS, it.right.name)
                is NotEquals -> SuccinctClause(it.left.name, NOT_EQUALS, it.right.name)
              }
            })

    val assignment =
        SuccinctAssignment<String, Int>(
            DOTAssignmentParser.parseToEnd(attributes[ASSIGNMENT]?.value ?: "")
                .assignments
                .associate { (target, source) -> target.name to source.name })

    return DOTTransition(label, guard, assignment)
  }
}

private object DOTTransitionSerializer : MonoFunction<DOTTransition, Map<String, Attribute>> {
  override fun apply(transition: DOTTransition): Map<String, Attribute> = buildMap {
    val label =
        "${transition.label.label}(${transition.label.parameterNames.joinToString ( ", " )})"
    this[DATA_LABEL] = DefaultAttribute(label, STRING)

    val guard =
        transition.guard.clauses.joinToString(" && ", "(", ")") { (left, operator, right) ->
          val symbol =
              when (operator) {
                EQUALS -> "=="
                NOT_EQUALS -> "<>"
              }
          "$left $symbol $right"
        }
    this[GUARD] = DefaultAttribute(guard, STRING)

    val assignment =
        transition.assignment.mapping.entries.joinToString("; ") { (target, source) ->
          "$target $:= $source"
        }
    this[ASSIGNMENT] = DefaultAttribute(assignment, STRING)

    val humanReadable =
        createHTML().table {
          attributes["border"] = "0"
          attributes["cellborder"] = "1"
          attributes["cellspacing"] = "0"
          tr {
            td { +label }
            td { +guard }
          }
          tr {
            td {
              colSpan = "2"
              +assignment
            }
          }
        }
    this[EDGE_LABEL] = DefaultAttribute(humanReadable, HTML)
  }
}

private object GraphvizDeserializer :
    ModelDeserializer<SuccinctRegisterAutomaton<RALocation, String, String, Int>> {

  private val importer =
      DOTImporter<DOTLocation, DOTTransition>().apply {
        vertexWithAttributesFactory = DOTLocationDeserializer
        edgeWithAttributesFactory = DOTTransitionDeserializer
      }

  override fun readModel(
      inputStream: InputStream
  ): SuccinctRegisterAutomaton<RALocation, String, String, Int> {
    val graph = DirectedPseudograph<DOTLocation, DOTTransition>(DOTTransition::class.java)
    importer.importGraph(graph, inputStream)

    return QuickSuccinctNRA<String, String, Int>().apply {
      val locationMapping =
          graph.vertexSet().associate { dotLocation ->
            dotLocation.name to
                addLocation(dotLocation.isAccepting).also {
                  it.humanReadableName = dotLocation.name
                }
          }
      graph.vertexSet().filter { it.initialValuations.isNotEmpty() }.forEach { dotLocation ->
        dotLocation.initialValuations.forEach {
          addInitialState(Configuration(locationMapping.getValue(dotLocation.name), it))
        }
      }

      graph.edgeSet().forEach {
        addLocationTransition(
            locationMapping.getValue(graph.getEdgeSource(it).name),
            RATransition(
                locationMapping.getValue(graph.getEdgeTarget(it).name),
                it.label,
                it.guard,
                it.assignment))
      }
    }
  }
}

object GraphvizSerializer :
    ModelSerializer<SuccinctRegisterAutomaton<RALocation, String, String, Int>> {

  private val exporter =
      DOTExporter<DOTLocation, DOTTransition>().apply {
        setVertexIdProvider { "\"${it.name.replace("\"", "\\\"")}\"" }
        setVertexAttributeProvider(DOTLocationSerializer)
        setEdgeAttributeProvider(DOTTransitionSerializer)
      }

  override fun writeModel(
      outputStream: OutputStream,
      model: SuccinctRegisterAutomaton<RALocation, String, String, Int>
  ) {

    val graph = DirectedPseudograph<DOTLocation, DOTTransition>(DOTTransition::class.java)

    val locationMap =
        model.locations.associateWith { location ->
          val valuations =
              model.initialStates.filter { it.location == location }.map { it.valuation }
          DOTLocation(
                  location.toString(),
                  valuations.toSet(),
                  model.isAcceptingLocation(location),
              )
              .also { graph.addVertex(it) }
        }

    model.locations
        .flatMap { source -> model.getLocationTransitions(source).map { source to it } }
        .forEach { (source, transition) ->
          check(
              graph.addEdge(
                  locationMap.getValue(source),
                  locationMap.getValue(transition.destination),
                  DOTTransition(transition.dataLabel, transition.guard, transition.assignment)))
        }

    exporter.exportGraph(graph, outputStream)
  }
}
