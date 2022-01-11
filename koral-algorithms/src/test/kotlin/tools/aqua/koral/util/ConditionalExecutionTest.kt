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

package tools.aqua.koral.util

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

internal class ConditionalExecutionTest {

  private data class Holder(var condition: Boolean)

  @ParameterizedTest
  @ValueSource(booleans = [true, false])
  fun `conditional execution executes correctly`(shouldRun: Boolean) {
    val holder = Holder(!shouldRun)
    var ran = false
    computeGuarded(holder::condition) { ran = true }
    assertEquals(shouldRun, ran)
  }

  @ParameterizedTest
  @ValueSource(booleans = [true, false])
  fun `conditional execution sets guard correctly`(shouldRun: Boolean) {
    val holder = Holder(!shouldRun)
    computeGuarded(holder::condition) {}
    assertTrue(holder.condition)
  }
}
