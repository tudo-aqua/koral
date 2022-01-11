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

package tools.aqua.koral.util

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

internal class SaturatedArithmeticTest {
  @Test
  fun `saturated addition of small numbers is correct`() {
    assertEquals(100, 25 satPlus 75)
  }

  @Test
  fun `saturated addition to MAX_VALUE is correct`() {
    assertEquals(Int.MAX_VALUE, (Int.MAX_VALUE - 1) satPlus 1)
  }

  @Test
  fun `saturated addition above MAX_VALUE is correct`() {
    assertEquals(Int.MAX_VALUE, Int.MAX_VALUE satPlus 1)
  }

  @Test
  fun `saturated addition of small negative numbers is correct`() {
    assertEquals(-100, -25 satPlus -75)
  }

  @Test
  fun `saturated addition to MIN_VALUE is correct`() {
    assertEquals(Int.MIN_VALUE, (Int.MIN_VALUE + 1) satPlus -1)
  }

  @Test
  fun `saturated addition below MIN_VALUE is correct`() {
    assertEquals(Int.MIN_VALUE, Int.MIN_VALUE satPlus -1)
  }

  @Test
  fun `saturated subtraction of small numbers is correct`() {
    assertEquals(100, 175 satMinus 75)
  }

  @Test
  fun `saturated subtraction to MAX_VALUE is correct`() {
    assertEquals(Int.MAX_VALUE, (Int.MAX_VALUE - 1) satMinus -1)
  }

  @Test
  fun `saturated subtraction above MAX_VALUE throws correct exception`() {
    assertEquals(Int.MAX_VALUE, Int.MAX_VALUE satMinus -1)
  }

  @Test
  fun `saturated subtraction of small negative numbers is correct`() {
    assertEquals(100, 25 satPlus 75)
  }

  @Test
  fun `saturated subtraction to MIN_VALUE is correct`() {
    assertEquals(Int.MIN_VALUE, (Int.MIN_VALUE + 1) satMinus 1)
  }

  @Test
  fun `saturated subtraction below MIN_VALUE is correct`() {
    assertEquals(Int.MIN_VALUE, Int.MIN_VALUE satMinus 1)
  }
}
