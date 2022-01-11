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

import java.net.URL
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

internal class AutomataWikiTest {

  companion object {
    @JvmStatic
    fun automataURLs(): List<URL> =
        Companion::class.java.getResourceAsStream("/automata-file-list")!!.bufferedReader().use {
            reader ->
          reader.readLines().filter(String::isNotEmpty).map { file ->
            Companion::class.java.getResource(file)!!
          }
        }
  }

  @ParameterizedTest
  @MethodSource("automataURLs")
  fun `ra is loaded`(url: URL) {
    assertDoesNotThrow { AutomataWikiDeserializer.readModel(url) }
  }

  @ParameterizedTest
  @MethodSource("automataURLs")
  fun `ra converts to succinct or null`(url: URL) {
    assertDoesNotThrow { AutomataWikiDeserializer.readModel(url).tryToSuccinct() }
  }
}
