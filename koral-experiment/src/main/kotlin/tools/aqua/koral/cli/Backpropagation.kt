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

package tools.aqua.koral.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.requireObject
import com.github.ajalt.clikt.parameters.arguments.ArgumentTransformContext
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.transformAll
import com.github.ajalt.clikt.parameters.options.check
import com.github.ajalt.clikt.parameters.options.convert
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.types.double
import com.github.ajalt.clikt.parameters.types.file
import com.github.ajalt.clikt.parameters.types.int
import com.github.ajalt.clikt.parameters.types.long
import com.google.common.base.Stopwatch
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.writeText
import kotlin.math.roundToInt
import kotlin.math.sqrt
import kotlin.random.Random
import kotlin.time.Duration
import kotlin.time.toKotlinDuration
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.apache.commons.math3.distribution.ConstantRealDistribution
import org.apache.commons.math3.distribution.PoissonDistribution
import tools.aqua.koral.algorithms.ConstraintTrackingSuccinctNRA
import tools.aqua.koral.algorithms.RAGenerator
import tools.aqua.koral.algorithms.times
import tools.aqua.koral.automata.register.RAAssignment
import tools.aqua.koral.automata.register.RAGuard
import tools.aqua.koral.automata.register.RegisterAutomaton
import tools.aqua.koral.automata.succinct.SuccinctRegisterAutomaton
import tools.aqua.koral.impl.RALocation
import tools.aqua.koral.serialization.automatawiki.AutomataWikiDeserializer
import tools.aqua.koral.serialization.automatawiki.tryToSuccinct
import tools.aqua.koral.serialization.graphviz.GraphvizSerializer
import tools.aqua.koral.util.ConstantIntegerDistribution
import tools.aqua.koral.util.DurationAsLongSerializer

class Backpropagate : CliktCommand(help = "Run backpropagation transformation") {
  private val outputDirectory by
      option("-o", "--output", help = "Output directory for result file (default: .)")
          .file(canBeFile = false)
          .convert { it.toPath() }
          .default(Path.of(""))

  data class Options(val outputDirectory: Path)
  override fun run() {
    currentContext.findOrSetObject { Options(outputDirectory) }
    outputDirectory.createDirectories()
  }
}

data class Automaton(
    val name: String,
    val ra: SuccinctRegisterAutomaton<RALocation, String, String, Int>
)

fun ArgumentTransformContext.loadFromAutomatonWiki(args: List<String>): Automaton {
  val (benchmark, model) =
      when (args.size) {
        1 -> args.single() to "model"
        2 -> args[0] to args[1]
        else -> fail("Only one or two arguments supported")
      }
  val name = "$benchmark/$model"
  val path = "/Register/principle/Benchmark$name.register.xml"
  val url = javaClass.getResource(path) ?: fail("Automaton $name not found")
  val ra =
      AutomataWikiDeserializer.readModel(url).tryToSuccinct()
          ?: fail("Automaton $name is not succinct")
  return Automaton(name, ra)
}

class AutomataWiki : CliktCommand(help = "Use automaton from AutomataWiki") {
  private val automaton by
      argument(
              help =
                  "The automaton group and instance, e.g., Queue fifo_4. If only the group is given, the instance defaults to model")
          .transformAll(nvalues = -1, transform = ArgumentTransformContext::loadFromAutomatonWiki)

  private val config by requireObject<Backpropagate.Options>()

  override fun run() {
    runBackpropagationAndWriteResult(
        config.outputDirectory, automaton.name.replace("/", "-"), automaton.ra)
  }
}

class AutomataWikiProduct : CliktCommand(help = "Use product of two automata from AutomataWiki") {
  private val leftAutomaton by
      argument(help = "The automaton group and instance, e.g., Queue fifo_4.")
          .transformAll(nvalues = 2, transform = ArgumentTransformContext::loadFromAutomatonWiki)
  private val rightAutomaton by
      argument(help = "The automaton group and instance, e.g., Queue fifo_4.")
          .transformAll(nvalues = 2, transform = ArgumentTransformContext::loadFromAutomatonWiki)

  private val config by requireObject<Backpropagate.Options>()

  override fun run() {
    runBackpropagationAndWriteResult(
        config.outputDirectory,
        "${leftAutomaton.name.replace("/", "-")}-*-${rightAutomaton.name.replace("/", "-")}",
        leftAutomaton.ra * rightAutomaton.ra)
  }
}

class RandomAutomaton : CliktCommand(help = "Use random automaton") {
  private val seed by
      option("-s", "--seed", help = "The seed to use for generation").long().required()

  private val arity by
      option("-a", "--arity", help = "The maximum arity of labels").int().required()

  private val registers by
      option("-r", "--registers", help = "The number of registers to generate").int().required()

  private val locations by
      option("-l", "--locations", help = "The number of locations to generate").int().required()

  private val density by
      option(
          "-d",
          "--transition-density",
          help =
              "The number of additional transitions as a fraction of locations, must be in [0, )")
          .double()
          .required()
          .check { it >= 0 }

  private val transitionComplexity by
      option(
          "-c",
          "--transition-complexity",
          help = "The complexity of transitions as a fraction, must be in [0, 1]")
          .double()
          .required()
          .check { it in 0.0..1.0 }

  private val config by requireObject<Backpropagate.Options>()

  override fun run() {
    val name =
        "random" +
            "-a$arity" +
            "-r$registers" +
            "-l$locations" +
            "-d$density" +
            "-c$transitionComplexity" +
            "-#$seed"
    val additionalTransitions = (locations * density).roundToInt()
    val clusterLambda = sqrt((registers + arity) * transitionComplexity)
    val generator =
        RAGenerator<Int>(
            random = Random(seed),
            maximumArity = ConstantIntegerDistribution(arity),
            labelsPerArity = ConstantIntegerDistribution(1),
            registers = ConstantIntegerDistribution(registers),
            locations = ConstantIntegerDistribution(locations),
            acceptingProportion = ConstantRealDistribution(.01),
            additionalTransitions = ConstantIntegerDistribution(additionalTransitions),
            equalityClusters = PoissonDistribution(clusterLambda),
            equalityClusterSize = PoissonDistribution(clusterLambda),
            inequalities = PoissonDistribution(clusterLambda),
            assignmentClusters = PoissonDistribution(clusterLambda),
            assignmentClusterSize = PoissonDistribution(clusterLambda),
        )
    val ra = generator.next()

    runBackpropagationAndWriteResult(config.outputDirectory, name, ra)
  }
}

fun runBackpropagationAndWriteResult(
    targetDir: Path,
    name: String,
    ra: SuccinctRegisterAutomaton<RALocation, String, String, Int>,
) {
  val (result, resultRA) = runBackpropagation(name, ra)
  GraphvizSerializer.writeModel(targetDir.resolve("$name-in.dot").toFile(), ra)
  targetDir.resolve("$name.json").writeText(Json.encodeToString(result))
  GraphvizSerializer.writeModel(targetDir.resolve("$name-out.dot").toFile(), resultRA)
}

@Serializable
data class BackpropagationResult(
    val name: String,
    val before: RAStatistics,
    @Serializable(with = DurationAsLongSerializer::class) val conversionTime: Duration,
    @Serializable(with = DurationAsLongSerializer::class) val backpropagationTime: Duration,
    val after: RAStatistics
)

@Serializable
data class RAStatistics(val registers: Int, val locations: Int, val transitions: Int) {
  companion object {
    fun <
        Location,
        Variable,
        Label,
        DataValue,
        Guard : RAGuard<Variable, DataValue>,
        Assignment : RAAssignment<Variable, DataValue>> of(
        ra: RegisterAutomaton<Location, Variable, Label, DataValue, Guard, Assignment>
    ): RAStatistics =
        RAStatistics(
            ra.registers.size,
            ra.locations.size,
            ra.locations.sumOf { ra.getLocationTransitions(it).size })
  }
}

fun runBackpropagation(
    name: String,
    ra: SuccinctRegisterAutomaton<RALocation, String, String, Int>,
): Pair<BackpropagationResult, SuccinctRegisterAutomaton<RALocation, String, String, Int>> {
  val before = RAStatistics.of(ra)

  val (cRA, conversionTime) = timed { ConstraintTrackingSuccinctNRA(ra) }
  val (_, backpropagationTime) = timed { cRA.makeHistoryIndependent() }

  val after = RAStatistics.of(cRA)

  return BackpropagationResult(name, before, conversionTime, backpropagationTime, after) to
      cRA.toSuccinctNRA()
}

inline fun <T> timed(block: () -> T): Pair<T, Duration> {
  val stopwatch = Stopwatch.createStarted()
  val result = block()
  val elapsed = stopwatch.stop().elapsed()
  return result to elapsed.toKotlinDuration()
}
