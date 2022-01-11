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

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import tools.aqua.koral.automata.register.RATransition
import tools.aqua.koral.automata.succinct.SuccinctAssignment
import tools.aqua.koral.automata.succinct.SuccinctClause
import tools.aqua.koral.automata.succinct.SuccinctGuard
import tools.aqua.koral.automata.succinct.SuccinctOperator.EQUALS
import tools.aqua.koral.automata.succinct.SuccinctOperator.NOT_EQUALS
import tools.aqua.koral.impl.QuickSuccinctNRA
import tools.aqua.koral.util.Relation.DISTINCT
import tools.aqua.koral.util.Relation.EQUAL
import tools.aqua.koral.util.Relation.UNKNOWN
import tools.aqua.koral.util.Tag.DESTINATION_REGISTER
import tools.aqua.koral.util.Tag.PARAMETER
import tools.aqua.koral.util.Tag.SOURCE_REGISTER

internal class ConstraintTrackingSuccinctNRATest {

  @Test
  fun `initial location translation works`() {
    val ra = QuickSuccinctNRA<String, String, String>()
    val (q0, _) = ra.addInitialState(mapOf("x0" to "a"))

    val ctra = ConstraintTrackingSuccinctNRA(ra)

    assertEquals(1, ctra.locations.size)
    assertEquals(q0, ctra.locations.single().originalLocation)

    assertEquals(1, ctra.initialStates.size)
    assertEquals(q0, ctra.initialStates.single().location.originalLocation)
  }

  @Test
  fun `accepting location translation works`() {
    val ra = QuickSuccinctNRA<String, String, String>()
    val (q0, _) = ra.addInitialState(mapOf("x0" to "a"))
    val q1 = ra.addLocation(true)

    val ctra = ConstraintTrackingSuccinctNRA(ra)
    val cq0 = ctra.locations.single { it.originalLocation == q0 }
    val cq1 = ctra.locations.single { it.originalLocation == q1 }

    assertFalse(ctra.isAcceptingLocation(cq0))
    assertTrue(ctra.isAcceptingLocation(cq1))
  }

  @Test
  fun `regular transition translation works`() {
    val ra = QuickSuccinctNRA<String, String, String>()
    val (q0, _) = ra.addInitialState(mapOf("x0" to "a"))
    val q1 = ra.addLocation()

    val t0 =
        RATransition(
            q1, l0, SuccinctGuard<String, String>(), SuccinctAssignment(mapOf("x0" to "x0")))
    ra.addLocationTransition(q0, t0)

    val ctra = ConstraintTrackingSuccinctNRA(ra)

    val cq0 = ctra.locations.single { it.originalLocation == q0 }
    val cq1 = ctra.locations.single { it.originalLocation == q1 }

    val transitions = ctra.getLocationTransitions(cq0)
    assertEquals(1, transitions.size)
    assertTrue(ctra.getLocationTransitions(cq1).isEmpty())

    val ct0 = transitions.single()
    assertEquals(q1, ct0.destination.originalLocation)
    assertEquals(l0, ct0.dataLabel)
  }

  @Test
  fun `loop transition translation works`() {
    val ra = QuickSuccinctNRA<String, String, String>()
    val (q0, _) = ra.addInitialState(mapOf("x0" to "a"))

    val t0 =
        RATransition(
            q0, l0, SuccinctGuard<String, String>(), SuccinctAssignment(mapOf("x0" to "x0")))
    ra.addLocationTransition(q0, t0)

    val ctra = ConstraintTrackingSuccinctNRA(ra)

    val cq0 = ctra.locations.single { it.originalLocation == q0 }

    val transitions = ctra.getLocationTransitions(cq0)
    assertEquals(1, transitions.size)

    val ct0 = transitions.single()
    assertEquals(q0, ct0.destination.originalLocation)
    assertEquals(l0, ct0.dataLabel)
  }

  @Test
  fun `guard translation works`() {
    val ra = QuickSuccinctNRA<String, String, String>()
    val (q0, _) = ra.addInitialState(mapOf("x0" to "a", "x1" to "b"))

    val t0 =
        RATransition(
            q0,
            l2,
            SuccinctGuard<String, String>(
                SuccinctClause("x0", EQUALS, "p0"), SuccinctClause("x1", NOT_EQUALS, "p1")),
            SuccinctAssignment(mapOf("x0" to "x0", "x1" to "x1")))
    ra.addLocationTransition(q0, t0)

    val ctra = ConstraintTrackingSuccinctNRA(ra)
    val ct0 =
        ctra.getLocationTransitions(ctra.locations.single { it.originalLocation == q0 }).single()
    val constraint = ct0.guard.constraint

    assertEquals(setOf("x0", "x1"), constraint.registers.keys)
    assertEquals(setOf("p0", "p1"), constraint.parameters.keys)

    assertEquals(UNKNOWN, constraint[SOURCE_REGISTER("x0"), SOURCE_REGISTER("x1")])
    assertEquals(EQUAL, constraint[SOURCE_REGISTER("x0"), PARAMETER("p0")])
    assertEquals(UNKNOWN, constraint[SOURCE_REGISTER("x0"), PARAMETER("p1")])

    assertEquals(UNKNOWN, constraint[SOURCE_REGISTER("x1"), PARAMETER("p0")])
    assertEquals(DISTINCT, constraint[SOURCE_REGISTER("x1"), PARAMETER("p1")])

    assertEquals(UNKNOWN, constraint[PARAMETER("p0"), PARAMETER("p1")])
  }

  @Test
  fun `assignment translation works`() {
    val ra = QuickSuccinctNRA<String, String, String>()
    val (q0, _) = ra.addInitialState(mapOf("x0" to "a", "x1" to "b"))

    val t0 =
        RATransition(
            q0,
            l1,
            SuccinctGuard<String, String>(),
            SuccinctAssignment(mapOf("x0" to "x0", "x1" to "x0", "x2" to "p0")))
    ra.addLocationTransition(q0, t0)

    val ctra = ConstraintTrackingSuccinctNRA(ra)
    val ct0 =
        ctra.getLocationTransitions(ctra.locations.single { it.originalLocation == q0 }).single()
    val constraint = ct0.assignment.constraint

    assertEquals(setOf("x0", "x1", "x2"), constraint.registers.keys)
    assertEquals(setOf("p0"), constraint.parameters.keys)

    assertEquals(EQUAL, constraint[SOURCE_REGISTER("x0"), DESTINATION_REGISTER("x0")])
    assertEquals(EQUAL, constraint[SOURCE_REGISTER("x0"), DESTINATION_REGISTER("x1")])
    assertEquals(UNKNOWN, constraint[SOURCE_REGISTER("x0"), DESTINATION_REGISTER("x2")])

    assertEquals(UNKNOWN, constraint[SOURCE_REGISTER("x1"), DESTINATION_REGISTER("x0")])
    assertEquals(UNKNOWN, constraint[SOURCE_REGISTER("x1"), DESTINATION_REGISTER("x1")])
    assertEquals(UNKNOWN, constraint[SOURCE_REGISTER("x1"), DESTINATION_REGISTER("x2")])

    assertEquals(UNKNOWN, constraint[PARAMETER("p0"), DESTINATION_REGISTER("x0")])
    assertEquals(UNKNOWN, constraint[PARAMETER("p0"), DESTINATION_REGISTER("x1")])
    assertEquals(EQUAL, constraint[PARAMETER("p0"), DESTINATION_REGISTER("x2")])
  }

  @Test
  fun `no-op single-location backpropagation works`() {
    val ra = QuickSuccinctNRA<String, String, String>()
    ra.addInitialState(mapOf("x0" to "a"))

    val ctra = ConstraintTrackingSuccinctNRA(ra)
    ctra.makeHistoryIndependent()

    assertEquals(1, ctra.locations.size)
  }

  @Test
  fun `no-op two-location backpropagation works`() {
    val ra = QuickSuccinctNRA<String, String, String>()
    val (q0, _) = ra.addInitialState(mapOf("x0" to "a"))
    val q1 = ra.addLocation()

    val t0 =
        RATransition(
            q1, l0, SuccinctGuard<String, String>(), SuccinctAssignment(mapOf("x0" to "x0")))
    ra.addLocationTransition(q0, t0)

    val ctra = ConstraintTrackingSuccinctNRA(ra)
    ctra.makeHistoryIndependent()

    assertEquals(2, ctra.locations.size)
    assertEquals(1, ctra.locations.count { it.originalLocation == q0 })
    assertEquals(1, ctra.locations.count { it.originalLocation == q1 })

    val cq0 = ctra.locations.single { it.originalLocation == q0 }
    val cq1 = ctra.locations.single { it.originalLocation == q1 }

    val transitions = ctra.getLocationTransitions(cq0)
    assertEquals(1, transitions.size)
    assertTrue(ctra.getLocationTransitions(cq1).isEmpty())

    val ct0 = transitions.single()
    assertEquals(q1, ct0.destination.originalLocation)
    assertEquals(l0, ct0.dataLabel)
  }

  @Test
  fun `two-location backpropagation with split is correct`() {
    val ra = QuickSuccinctNRA<String, String, String>()
    val (q0, _) = ra.addInitialState(mapOf("x0" to "a", "x1" to "a"))
    val q1 = ra.addLocation()

    val t0 =
        RATransition(
            q1,
            l0,
            SuccinctGuard<String, String>(SuccinctClause("x0", EQUALS, "x1")),
            SuccinctAssignment(mapOf("x0" to "x0", "x1" to "x1")))
    ra.addLocationTransition(q0, t0)

    val ctra = ConstraintTrackingSuccinctNRA(ra)
    ctra.makeHistoryIndependent()

    assertEquals(3, ctra.locations.size)

    val cq0Terminal =
        ctra.locations.singleOrNull {
          it.originalLocation == q0 &&
              it.constraint[SOURCE_REGISTER("x0"), SOURCE_REGISTER("x1")] == UNKNOWN
        }
    assertNotNull(cq0Terminal)
    assertTrue(ctra.getLocationTransitions(cq0Terminal!!).isEmpty())

    val cq0ForT0 =
        ctra.locations.singleOrNull {
          it.originalLocation == q0 &&
              it.constraint[SOURCE_REGISTER("x0"), SOURCE_REGISTER("x1")] == EQUAL
        }
    assertNotNull(cq0ForT0)

    val cq1 =
        ctra.locations.singleOrNull {
          it.originalLocation == q1 &&
              it.constraint[SOURCE_REGISTER("x0"), SOURCE_REGISTER("x1")] == UNKNOWN
        }
    assertNotNull(cq1)

    val cq0ForT0Trans = ctra.getLocationTransitions(cq0ForT0!!).singleOrNull()
    assertNotNull(cq0ForT0Trans)
    assertEquals(cq1, cq0ForT0Trans!!.destination)

    assertTrue(ctra.getLocationTransitions(cq1!!).isEmpty())
  }

  @Test
  fun `three-location backpropagation generates parameter constraints`() {
    val ra = QuickSuccinctNRA<String, String, String>()
    val (q0, _) = ra.addInitialState(mapOf("x0" to "a", "x1" to "a"))
    val q1 = ra.addLocation()
    val q2 = ra.addLocation()

    val t0 =
        RATransition(
            q1,
            l2,
            SuccinctGuard<String, String>(),
            SuccinctAssignment(mapOf("x0" to "p0", "x1" to "p1")))
    ra.addLocationTransition(q0, t0)

    val t1 =
        RATransition(
            q2,
            l0,
            SuccinctGuard<String, String>(SuccinctClause("x0", EQUALS, "x1")),
            SuccinctAssignment(mapOf("x0" to "x0", "x1" to "x1")))
    ra.addLocationTransition(q1, t1)

    val ctra = ConstraintTrackingSuccinctNRA(ra)
    ctra.makeHistoryIndependent()

    assertEquals(4, ctra.locations.size)

    val cq0 =
        ctra.locations.singleOrNull {
          it.originalLocation == q0 &&
              it.constraint[SOURCE_REGISTER("x0"), SOURCE_REGISTER("x1")] == UNKNOWN
        }
    assertNotNull(cq0)

    val cq1Terminal =
        ctra.locations.singleOrNull {
          it.originalLocation == q1 &&
              it.constraint[SOURCE_REGISTER("x0"), SOURCE_REGISTER("x1")] == UNKNOWN
        }
    assertNotNull(cq1Terminal)

    val cq1ForT1 =
        ctra.locations.singleOrNull {
          it.originalLocation == q1 &&
              it.constraint[SOURCE_REGISTER("x0"), SOURCE_REGISTER("x1")] == EQUAL
        }
    assertNotNull(cq1ForT1)

    val cq2 =
        ctra.locations.singleOrNull {
          it.originalLocation == q2 &&
              it.constraint[SOURCE_REGISTER("x0"), SOURCE_REGISTER("x1")] == UNKNOWN
        }
    assertNotNull(cq1Terminal)

    val cq0TransTerminal =
        ctra.getLocationTransitions(cq0!!).singleOrNull {
          it.destination == cq1Terminal &&
              it.guard.constraint[SOURCE_REGISTER("x0"), SOURCE_REGISTER("x1")] == UNKNOWN &&
              it.guard.constraint[SOURCE_REGISTER("x0"), PARAMETER("p0")] == UNKNOWN &&
              it.guard.constraint[SOURCE_REGISTER("x0"), PARAMETER("p1")] == UNKNOWN &&
              it.guard.constraint[SOURCE_REGISTER("x1"), PARAMETER("p0")] == UNKNOWN &&
              it.guard.constraint[SOURCE_REGISTER("x1"), PARAMETER("p1")] == UNKNOWN &&
              it.guard.constraint[PARAMETER("p0"), PARAMETER("p1")] == UNKNOWN
        }
    assertNotNull(cq0TransTerminal)

    val cq0TransInter =
        ctra.getLocationTransitions(cq0).singleOrNull {
          it.destination == cq1ForT1 &&
              it.guard.constraint[SOURCE_REGISTER("x0"), SOURCE_REGISTER("x1")] == UNKNOWN &&
              it.guard.constraint[SOURCE_REGISTER("x0"), PARAMETER("p0")] == UNKNOWN &&
              it.guard.constraint[SOURCE_REGISTER("x0"), PARAMETER("p1")] == UNKNOWN &&
              it.guard.constraint[SOURCE_REGISTER("x1"), PARAMETER("p0")] == UNKNOWN &&
              it.guard.constraint[SOURCE_REGISTER("x1"), PARAMETER("p1")] == UNKNOWN &&
              it.guard.constraint[PARAMETER("p0"), PARAMETER("p1")] == EQUAL
        }
    assertNotNull(cq0TransInter)

    assertTrue(ctra.getLocationTransitions(cq1Terminal!!).isEmpty())

    val cq1ForT1Trans =
        ctra.getLocationTransitions(cq1ForT1!!).singleOrNull {
          it.destination == cq2 &&
              it.guard.constraint[SOURCE_REGISTER("x0"), SOURCE_REGISTER("x1")] == EQUAL
        }
    assertNotNull(cq1ForT1Trans)

    assertTrue(ctra.getLocationTransitions(cq2!!).isEmpty())
  }

  @Test
  fun `unreachable locations are eliminated`() {
    val ra = QuickSuccinctNRA<String, String, String>()
    val (q0, _) = ra.addInitialState(mapOf("x0" to "a", "x1" to "b"))
    ra.addLocation()

    val ctra = ConstraintTrackingSuccinctNRA(ra)
    ctra.makeHistoryIndependent()

    assertEquals(1, ctra.locations.size)

    val cq0Terminal =
        ctra.locations.singleOrNull {
          it.originalLocation == q0 &&
              it.constraint[SOURCE_REGISTER("x0"), SOURCE_REGISTER("x1")] == UNKNOWN
        }
    assertNotNull(cq0Terminal)

    assertTrue(ctra.getLocationTransitions(cq0Terminal!!).isEmpty())
  }

  @Test
  fun `infeasible locations are eliminated`() {
    val ra = QuickSuccinctNRA<String, String, String>()
    val (q0, _) = ra.addInitialState(mapOf("x0" to "a", "x1" to "b"))
    val q1 = ra.addLocation()

    val t0 =
        RATransition(
            q1,
            l0,
            SuccinctGuard<String, String>(SuccinctClause("x0", EQUALS, "x1")),
            SuccinctAssignment(mapOf("x0" to "x0", "x1" to "x1")))
    ra.addLocationTransition(q0, t0)

    val ctra = ConstraintTrackingSuccinctNRA(ra)
    ctra.makeHistoryIndependent()

    assertEquals(1, ctra.locations.size)

    val cq0Terminal =
        ctra.locations.singleOrNull {
          it.originalLocation == q0 &&
              it.constraint[SOURCE_REGISTER("x0"), SOURCE_REGISTER("x1")] == UNKNOWN
        }
    assertNotNull(cq0Terminal)

    assertTrue(ctra.getLocationTransitions(cq0Terminal!!).isEmpty())
  }

  @Test
  fun `two-location backpropagation with loop is correct`() {
    val ra = QuickSuccinctNRA<String, String, String>()
    val (q0, _) = ra.addInitialState(mapOf("x0" to "a", "x1" to "b", "x2" to "c"))
    val q1 = ra.addLocation()

    val t0 =
        RATransition(
            q0,
            l0,
            SuccinctGuard<String, String>(SuccinctClause("x0", NOT_EQUALS, "x2")),
            SuccinctAssignment(mapOf("x0" to "x1", "x1" to "x2", "x2" to "x2")))
    ra.addLocationTransition(q0, t0)

    val t1 =
        RATransition(
            q1,
            l0,
            SuccinctGuard<String, String>(SuccinctClause("x0", EQUALS, "x2")),
            SuccinctAssignment(mapOf("x0" to "x0", "x1" to "x1", "x2" to "x2")))
    ra.addLocationTransition(q0, t1)

    val ctra = ConstraintTrackingSuccinctNRA(ra)
    ctra.makeHistoryIndependent()

    assertEquals(6, ctra.locations.size)

    val cq0Terminal =
        ctra.locations.singleOrNull {
          it.originalLocation == q0 &&
              it.constraint[SOURCE_REGISTER("x0"), SOURCE_REGISTER("x1")] == UNKNOWN &&
              it.constraint[SOURCE_REGISTER("x0"), SOURCE_REGISTER("x2")] == UNKNOWN &&
              it.constraint[SOURCE_REGISTER("x1"), SOURCE_REGISTER("x2")] == UNKNOWN
        }
    assertNotNull(cq0Terminal)

    val cq0ShiftToTerminal =
        ctra.locations.singleOrNull {
          it.originalLocation == q0 &&
              it.constraint[SOURCE_REGISTER("x0"), SOURCE_REGISTER("x1")] == UNKNOWN &&
              it.constraint[SOURCE_REGISTER("x0"), SOURCE_REGISTER("x2")] == DISTINCT &&
              it.constraint[SOURCE_REGISTER("x1"), SOURCE_REGISTER("x2")] == UNKNOWN
        }
    assertNotNull(cq0ShiftToTerminal)

    val cq0Initial =
        ctra.locations.singleOrNull {
          it.originalLocation == q0 &&
              it.constraint[SOURCE_REGISTER("x0"), SOURCE_REGISTER("x1")] == UNKNOWN &&
              it.constraint[SOURCE_REGISTER("x0"), SOURCE_REGISTER("x2")] == DISTINCT &&
              it.constraint[SOURCE_REGISTER("x1"), SOURCE_REGISTER("x2")] == DISTINCT
        }
    assertNotNull(cq0Initial)

    val cq0FirstShift =
        ctra.locations.singleOrNull {
          it.originalLocation == q0 &&
              it.constraint[SOURCE_REGISTER("x0"), SOURCE_REGISTER("x1")] == DISTINCT &&
              it.constraint[SOURCE_REGISTER("x0"), SOURCE_REGISTER("x2")] == DISTINCT &&
              it.constraint[SOURCE_REGISTER("x1"), SOURCE_REGISTER("x2")] == EQUAL
        }
    assertNotNull(cq0FirstShift)

    val cq0SecondShift =
        ctra.locations.singleOrNull {
          it.originalLocation == q0 &&
              it.constraint[SOURCE_REGISTER("x0"), SOURCE_REGISTER("x1")] == UNKNOWN &&
              it.constraint[SOURCE_REGISTER("x0"), SOURCE_REGISTER("x2")] == EQUAL &&
              it.constraint[SOURCE_REGISTER("x1"), SOURCE_REGISTER("x2")] == UNKNOWN
        }
    assertNotNull(cq0SecondShift)

    val cq1 =
        ctra.locations.singleOrNull {
          it.originalLocation == q1 &&
              it.constraint[SOURCE_REGISTER("x0"), SOURCE_REGISTER("x1")] == UNKNOWN &&
              it.constraint[SOURCE_REGISTER("x0"), SOURCE_REGISTER("x2")] == UNKNOWN &&
              it.constraint[SOURCE_REGISTER("x1"), SOURCE_REGISTER("x2")] == UNKNOWN
        }
    assertNotNull(cq1)

    assertEquals(
        setOf(cq0Initial, cq0ShiftToTerminal, cq0Terminal),
        ctra.initialStates.mapTo(mutableSetOf()) { it.location })

    assertTrue(ctra.getLocationTransitions(cq0Terminal!!).isEmpty())

    val cq0ShiftToTerminalTrans = ctra.getLocationTransitions(cq0ShiftToTerminal!!).singleOrNull()
    assertNotNull(cq0ShiftToTerminalTrans)
    assertEquals(cq0Terminal, cq0ShiftToTerminalTrans!!.destination)

    val cq0InitialTrans = ctra.getLocationTransitions(cq0Initial!!)
    assertEquals(
        setOf(cq0ShiftToTerminal, cq0FirstShift),
        cq0InitialTrans.mapTo(mutableSetOf()) { it.destination })

    val cq0FirstShiftTrans = ctra.getLocationTransitions(cq0FirstShift!!).singleOrNull()
    assertNotNull(cq0FirstShiftTrans)
    assertEquals(cq0SecondShift, cq0FirstShiftTrans!!.destination)

    val cq0SecondShiftTrans = ctra.getLocationTransitions(cq0SecondShift!!).singleOrNull()
    assertNotNull(cq0SecondShiftTrans)
    assertEquals(cq1, cq0SecondShiftTrans!!.destination)

    assertTrue(ctra.getLocationTransitions(cq1!!).isEmpty())
  }
}
