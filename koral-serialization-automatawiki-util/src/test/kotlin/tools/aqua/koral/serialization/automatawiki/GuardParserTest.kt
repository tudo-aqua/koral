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

import com.github.h0tk3y.betterParse.grammar.parseToEnd
import kotlin.random.Random
import org.apache.commons.text.CharacterPredicates.ASCII_ALPHA_NUMERALS
import org.apache.commons.text.CharacterPredicates.ASCII_LETTERS
import org.apache.commons.text.RandomStringGenerator
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

internal class GuardParserTest {

  companion object {

    private val random = Random(1000)

    private val chars =
        RandomStringGenerator.Builder()
            .filteredBy(ASCII_LETTERS)
            .usingRandom(random::nextInt)
            .build()
    private val charsAndNumbers =
        RandomStringGenerator.Builder()
            .filteredBy(ASCII_ALPHA_NUMERALS)
            .usingRandom(random::nextInt)
            .build()

    data class WithString<out T>(val thing: T, val toParse: String)

    private val variables =
        (1..10).map {
          (chars.generate(1) + charsAndNumbers.generate(random.nextInt(10))).let {
            WithString(Variable(it), it)
          }
        }

    private val literals =
        (1..10).map { random.nextInt().let { WithString(Literal(it), it.toString()) } }

    private val expressions: List<WithString<Expression>> = variables + literals

    private val trueGuard = WithString(True, "")

    private val binaryGuards =
        expressions.flatMap { (left, leftRef) ->
          expressions.flatMap { (right, rightRef) ->
            listOf(
                WithString(Equals(left, right), "$leftRef == $rightRef"),
                WithString(NotEquals(left, right), "$leftRef != $rightRef"),
                WithString(Greater(left, right), "$leftRef > $rightRef"),
                WithString(GreaterEquals(left, right), "$leftRef >= $rightRef"),
                WithString(Less(left, right), "$leftRef < $rightRef"),
                WithString(LessEquals(left, right), "$leftRef <= $rightRef"))
          }
        }

    private val chainGaurds =
        (2..10).flatMap { size ->
          val elements = binaryGuards.shuffled(Random(size)).take(size)
          listOf(
              WithString(
                  And(elements.map { it.thing }), elements.joinToString(" && ") { it.toParse }),
              WithString(
                  Or(elements.map { it.thing }), elements.joinToString(" || ") { it.toParse }))
        }

    private val overbracedGuards =
        (1..10).flatMap {
          (binaryGuards + chainGaurds).map { (guard, string) ->
            WithString(guard, "(".repeat(it) + string + ")".repeat(it))
          }
        }

    private val complexGuard =
        binaryGuards.shuffled(Random(1000)).let { guards ->
          WithString(
              And(
                  Or(And(guards[0].thing, guards[1].thing), And(guards[2].thing, guards[3].thing)),
                  Or(And(guards[4].thing, guards[5].thing), And(guards[6].thing, guards[7].thing))),
              "(((${guards[0].toParse}) && (${guards[1].toParse})) || " +
                  "((${guards[2].toParse}) && (${guards[3].toParse}))" +
                  ") && (" +
                  "((${guards[4].toParse}) && (${guards[5].toParse})) || " +
                  "((${guards[6].toParse}) && (${guards[7].toParse})))")
        }

    @JvmStatic
    fun guards() = binaryGuards + chainGaurds + overbracedGuards + complexGuard + trueGuard
  }

  @ParameterizedTest
  @MethodSource("guards")
  fun `parse matches reference`(case: WithString<Guard>) {
    assertEquals(case.thing, GuardParser.parseToEnd(case.toParse))
  }

  @ParameterizedTest
  @MethodSource("guards")
  fun `round-trip parse is equal`(case: WithString<Guard>) {
    assertEquals(case.thing, GuardParser.parseToEnd(case.thing.toString()))
  }
}
