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

import com.github.h0tk3y.betterParse.combinators.*
import com.github.h0tk3y.betterParse.grammar.Grammar
import com.github.h0tk3y.betterParse.lexer.literalToken
import com.github.h0tk3y.betterParse.lexer.regexToken
import com.github.h0tk3y.betterParse.parser.Parser
import tools.aqua.koral.automata.register.DataLabel

object DOTLabelParser : Grammar<DataLabel<String, String>>() {
  @Suppress("unused") private val whitespace by regexToken("""\s+""", ignore = true)

  private val leftParanthesis by literalToken("(")
  private val rightParanthesis by literalToken(")")

  private val sequence by literalToken(",")

  private val variable by regexToken("""[a-zA-Z]\w*""") map { it.text }

  private val parameters by separatedTerms(variable, sequence, acceptZero = true)

  override val rootParser: Parser<DataLabel<String, String>> by
      variable * -leftParanthesis * parameters * -rightParanthesis map
          { (label, parameters) ->
            DataLabel(label, parameters)
          }
}

data class Variable(val name: String) {
  override fun toString(): String = name
}

data class Guard(val comparisons: List<Comparison>) {
  constructor(vararg comparisons: Comparison) : this(comparisons.toList())
  override fun toString(): String = comparisons.joinToString(" && ", "(", ")")
}

sealed interface Comparison {
  val left: Variable
  val right: Variable
}

data class Equals(override val left: Variable, override val right: Variable) : Comparison {
  override fun toString(): String = "$left == $right"
}

data class NotEquals(override val left: Variable, override val right: Variable) : Comparison {
  override fun toString(): String = "$left != $right"
}

object DOTGuardParser : Grammar<Guard>() {
  @Suppress("unused") private val whitespace by regexToken("""\s+""", ignore = true)

  private val leftParanthesis by literalToken("(")
  private val rightParanthesis by literalToken(")")

  private val equal by literalToken("==")
  private val notEqual by literalToken("<>")

  private val and by literalToken("&&")

  private val variable by regexToken("""[a-zA-Z]\w*""") map { Variable(it.text) }

  private val equalsClause by
      variable * -equal * variable map { (left, right) -> Equals(left, right) }
  private val notEqualsClause by
      variable * -notEqual * variable map { (left, right) -> NotEquals(left, right) }

  private val comparison by -leftParanthesis * (equalsClause or notEqualsClause) * -rightParanthesis

  override val rootParser: Parser<Guard> by
      separatedTerms(comparison, and, acceptZero = true) map { Guard(it) }
}

data class Assignment(val assignments: List<SingleAssignment>) {
  constructor(vararg assignments: SingleAssignment) : this(assignments.toList())
  override fun toString(): String = assignments.joinToString("; ")
}

data class SingleAssignment(val target: Variable, val source: Variable) {
  override fun toString(): String = "$target := $source"
}

object DOTAssignmentParser : Grammar<Assignment>() {
  @Suppress("unused") private val whitespace by regexToken("""\s+""", ignore = true)

  private val assign by literalToken(":=")
  private val sequence by literalToken(";")

  private val variable by regexToken("""[a-zA-Z]\w*""") map { Variable(it.text) }

  private val singleAssignment by
      variable * -assign * variable map { (target, source) -> SingleAssignment(target, source) }

  override val rootParser: Parser<Assignment> by
      separatedTerms(singleAssignment, sequence, acceptZero = true) map { Assignment(it) }
}

data class Initialization(val assignments: List<LiteralAssignment>) {
  constructor(vararg assignments: LiteralAssignment) : this(assignments.toList())
  override fun toString(): String = assignments.joinToString(", ", "{", "}")
}

data class LiteralAssignment(val assignments: List<SingleLiteralAssignment>) {
  constructor(vararg assignments: SingleLiteralAssignment) : this(assignments.toList())
  override fun toString(): String = assignments.joinToString("; ")
}

data class SingleLiteralAssignment(val target: Variable, val source: Literal) {
  override fun toString(): String = "$target := $source"
}

data class Literal(val value: Int) {
  override fun toString(): String = value.toString()
}

object DOTInitializationParser : Grammar<Initialization>() {
  @Suppress("unused") private val whitespace by regexToken("""\s+""", ignore = true)

  private val leftParanthesis by literalToken("{")
  private val rightParanthesis by literalToken("}")

  private val assign by literalToken(":=")
  private val sequence by literalToken(";")

  private val initializationSequence by literalToken(",")

  private val variable by regexToken("""[a-zA-Z]\w*""") map { Variable(it.text) }
  private val literal by regexToken("""-?\d+""").map { Literal(it.text.toInt()) }

  private val singleLiteralAssignment by
      variable * -assign * literal map
          { (target, source) ->
            SingleLiteralAssignment(target, source)
          }

  private val literalAssignment by
      separatedTerms(singleLiteralAssignment, sequence, acceptZero = true) map
          {
            LiteralAssignment(it)
          }

  override val rootParser: Parser<Initialization> by
      separatedTerms(literalAssignment, sequence, acceptZero = true) map { Initialization(it) }
}
