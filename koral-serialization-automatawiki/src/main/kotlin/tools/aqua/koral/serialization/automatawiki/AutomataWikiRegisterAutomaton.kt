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

package tools.aqua.koral.serialization.automatawiki

import jakarta.xml.bind.JAXBContext
import java.io.InputStream
import javax.xml.XMLConstants.W3C_XML_SCHEMA_NS_URI
import javax.xml.stream.XMLInputFactory
import javax.xml.validation.SchemaFactory
import net.automatalib.serialization.ModelDeserializer
import net.automatalib.words.Alphabet as AutomataLibAlphabet
import net.automatalib.words.impl.ListAlphabet
import tools.aqua.koral.automata.extended.Configuration
import tools.aqua.koral.automata.register.*

object AutomataWikiDeserializer : ModelDeserializer<AutomataWikiRegisterAutomaton> {
  private val schemaFactory = SchemaFactory.newInstance(W3C_XML_SCHEMA_NS_URI)
  private val xmlInputFactory = XMLInputFactory.newInstance()

  private val schemaURL =
      AutomataWikiDeserializer::class.java.let {
        it.getResource("/${it.packageName.replace(".", "/")}/ra.xsd")!!
      }

  private val jaxbRAContext = JAXBContext.newInstance(AWRegisterAutomaton::class.java)
  private val jaxbUnmarshaller =
      jaxbRAContext.createUnmarshaller().also { it.schema = schemaFactory.newSchema(schemaURL) }

  override fun readModel(inputStream: InputStream): AutomataWikiRegisterAutomaton {
    val xmlInput = xmlInputFactory.createXMLStreamReader(inputStream)

    return AutomataWikiRegisterAutomaton(
        jaxbUnmarshaller.unmarshal(xmlInput, AWRegisterAutomaton::class.java).value)
  }
}

internal val AWAlphabet.inputsAndOutputs: List<AWSymbol>
  get() = inputs + outputs

class AutomataWikiRegisterAutomaton internal constructor(internal val xml: AWRegisterAutomaton) :
    RegisterAutomaton<String, String, String, Int, AutomataWikiGuard, AutomataWikiTransition> {

  override val labels: AutomataLibAlphabet<DataLabel<String, String>>
    get() =
        ListAlphabet(
            xml.alphabet.inputsAndOutputs.map { symbol ->
              DataLabel(symbol.name, symbol.params.map { it.name })
            })

  override val locations: Collection<String>
    get() = xml.locationObjects.map { it.name }

  override val registers: Collection<String>
    get() = (xml.globals + xml.constants).map { it.name }

  override fun isAcceptingLocation(location: String): Boolean = false

  override fun getLocationTransitions(
      location: String
  ): Collection<
      RATransition<String, String, String, Int, AutomataWikiGuard, AutomataWikiTransition>> =
      xml.transitions.filter { it.from == location }.map {
        val transition = relabel(it)
        RATransition(
            destination = transition.to,
            dataLabel = DataLabel(transition.symbol, transition.params),
            guard = AutomataWikiGuard(transition.guard),
            assignment = AutomataWikiTransition(transition, registers))
      }

  override fun getInitialStates(): Set<Configuration<String, Map<String, Int>>> {
    val initialLocations = xml.locationObjects.filter { it.isInitial }.map { it.name }
    val initialValuation = (xml.globals + xml.constants).associate { it.name to it.value }
    return initialLocations.mapTo(mutableSetOf()) { location ->
      Configuration(location, initialValuation)
    }
  }
}

class AutomataWikiGuard internal constructor(private val parse: Guard) : RAGuard<String, Int> {
  override fun isSatisfiedBy(valuation: Map<String, Int>): Boolean = parse.isSatisfiedBy(valuation)
}

class AutomataWikiTransition
internal constructor(private val xml: AWTransition, private val registers: Collection<String>) :
    RAAssignment<String, Int> {
  override fun computeFrom(valuation: Map<String, Int>): Map<String, Int> {
    val defined = xml.assignments.associate { it.to to valuation.getValue(it.from) }
    return registers
        .filterNot { it in defined.keys }
        .associateWithTo(mutableMapOf()) { valuation.getValue(it) }
        .apply { this += defined }
  }
}
