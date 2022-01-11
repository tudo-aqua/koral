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

import com.github.h0tk3y.betterParse.combinators.*
import com.github.h0tk3y.betterParse.grammar.Grammar
import com.github.h0tk3y.betterParse.grammar.parser
import com.github.h0tk3y.betterParse.lexer.literalToken
import com.github.h0tk3y.betterParse.lexer.regexToken
import com.github.h0tk3y.betterParse.parser.Parser

sealed interface Expression {
  fun getValue(valuation: Map<String, Int>): Int
}

data class Variable(val name: String) : Expression {
  override fun getValue(valuation: Map<String, Int>): Int = valuation.getValue(name)
  override fun toString(): String = name
}

data class Literal(val value: Int) : Expression {
  override fun getValue(valuation: Map<String, Int>): Int = value
  override fun toString(): String = value.toString()
}

sealed interface Guard {
  fun isSatisfiedBy(valuation: Map<String, Int>): Boolean
}

object True : Guard {
  override fun isSatisfiedBy(valuation: Map<String, Int>): Boolean = true
  override fun toString(): String = ""
}

sealed interface BinaryRelation : Guard {
  val left: Expression
  val right: Expression
}

data class Equals(override val left: Expression, override val right: Expression) : BinaryRelation {
  override fun isSatisfiedBy(valuation: Map<String, Int>): Boolean =
      left.getValue(valuation) == right.getValue(valuation)
  override fun toString(): String = "$left == $right"
}

data class NotEquals(override val left: Expression, override val right: Expression) :
    BinaryRelation {
  override fun isSatisfiedBy(valuation: Map<String, Int>): Boolean =
      left.getValue(valuation) != right.getValue(valuation)
  override fun toString(): String = "$left != $right"
}

data class Greater(override val left: Expression, override val right: Expression) : BinaryRelation {
  override fun isSatisfiedBy(valuation: Map<String, Int>): Boolean =
      left.getValue(valuation) > right.getValue(valuation)
  override fun toString(): String = "$left > $right"
}

data class GreaterEquals(override val left: Expression, override val right: Expression) :
    BinaryRelation {
  override fun isSatisfiedBy(valuation: Map<String, Int>): Boolean =
      left.getValue(valuation) >= right.getValue(valuation)
  override fun toString(): String = "$left >= $right"
}

data class Less(override val left: Expression, override val right: Expression) : BinaryRelation {
  override fun isSatisfiedBy(valuation: Map<String, Int>): Boolean =
      left.getValue(valuation) < right.getValue(valuation)
  override fun toString(): String = "$left < $right"
}

data class LessEquals(override val left: Expression, override val right: Expression) :
    BinaryRelation {
  override fun isSatisfiedBy(valuation: Map<String, Int>): Boolean =
      left.getValue(valuation) <= right.getValue(valuation)
  override fun toString(): String = "$left <= $right"
}

sealed interface VariadicRelation : Guard {
  val terms: List<Guard>
}

data class And(override val terms: List<Guard>) : VariadicRelation {
  constructor(vararg terms: Guard) : this(terms.toList())
  override fun isSatisfiedBy(valuation: Map<String, Int>): Boolean =
      terms.all { it.isSatisfiedBy(valuation) }
  override fun toString(): String = terms.joinToString(" && ", "(", ")")
}

data class Or(override val terms: List<Guard>) : VariadicRelation {
  constructor(vararg terms: Guard) : this(terms.toList())
  override fun isSatisfiedBy(valuation: Map<String, Int>): Boolean =
      terms.any { it.isSatisfiedBy(valuation) }
  override fun toString(): String = terms.joinToString(" || ", "(", ")")
}

object GuardParser : Grammar<Guard>() {
  @Suppress("unused") private val whitespace by regexToken("""\s+""", ignore = true)

  private val leftParanthesis by literalToken("(")
  private val rightParanthesis by literalToken(")")

  private val variable by regexToken("""[a-zA-Z]\w*""")
  private val literal by regexToken("""-?\d+""")

  private val variableExpression by variable map { Variable(it.text) }
  private val literalExpression by literal map { Literal(it.text.toInt()) }

  private val expression: Parser<Expression> by variableExpression or literalExpression

  private val equal by literalToken("==")
  private val notEqual by literalToken("!=")
  private val greaterEqual by literalToken(">=") // must come before greater
  private val greater by literalToken(">")
  private val lessEqual by literalToken("<=") // must come before less
  private val less by literalToken("<")

  private val equalsClause by
      expression * -equal * expression map { (left, right) -> Equals(left, right) }
  private val notEqualsClause by
      expression * -notEqual * expression map { (left, right) -> NotEquals(left, right) }
  private val greaterClause by
      expression * -greater * expression map { (left, right) -> Greater(left, right) }
  private val greaterEqualsClause by
      expression * -greaterEqual * expression map { (left, right) -> GreaterEquals(left, right) }
  private val lessClause by
      expression * -less * expression map { (left, right) -> Less(left, right) }
  private val lessEqualsClause by
      expression * -lessEqual * expression map { (left, right) -> LessEquals(left, right) }

  private val bracedGuard by -leftParanthesis * parser(GuardParser::orChain) * -rightParanthesis

  private val clause: Parser<Guard> by
      equalsClause or
          notEqualsClause or
          greaterClause or
          greaterEqualsClause or
          lessClause or
          lessEqualsClause or
          bracedGuard

  private val and by literalToken("&&")
  private val or by literalToken("||")

  private val andChain by
      separatedTerms(clause, and, acceptZero = true) map
          {
            if (it.isEmpty()) True else if (it.size == 1) it.single() else And(it)
          }
  private val orChain by
      separatedTerms(andChain, or, acceptZero = true) map
          {
            if (it.isEmpty()) True else if (it.size == 1) it.single() else Or(it)
          }

  override val rootParser: Parser<Guard> = orChain
}
