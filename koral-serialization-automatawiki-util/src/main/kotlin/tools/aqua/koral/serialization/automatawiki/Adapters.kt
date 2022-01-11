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
import jakarta.xml.bind.annotation.adapters.XmlAdapter

/** Adapter for string lists of form `a,b,c`. */
class CSVAdapter : XmlAdapter<String?, List<String?>?>() {
  override fun marshal(v: List<String?>?): String? = v?.joinToString(",")

  override fun unmarshal(v: String?): List<String?>? = v?.split(",")
}

/** Adapter for [Guard]s' string representation. */
class GuardAdapter : XmlAdapter<String?, Guard?>() {
  override fun marshal(v: Guard?): String? {
    return v?.toString()
  }

  override fun unmarshal(v: String?): Guard? {
    return if (v == null) null else GuardParser.parseToEnd(v)
  }
}
