/*
 * Copyright 2021-2022 The Koral Authors
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

import tools.aqua.automataWiki

plugins {
  id("tools.aqua.koral.kotlin-conventions")
  id("tools.aqua.koral.library-conventions")
}

mavenMetadata {
  name.set("Koral RA Graphviz Serialization")
  description.set("Serialization and Deserialization for RAs in the Graphviz (DOT) format.")
}

dependencies {
  api(project(":koral-api"))
  api(project(":koral-impl"))
  api(libs.betterParse)
  api(libs.automatalib.serialization.core)

  implementation(project(":koral-util"))
  implementation(libs.automatalib.core)

  implementation(libs.jgrapht.core)
  implementation(libs.jgrapht.io)
  implementation(libs.kotlinx.html)
}

repositories { automataWiki() }
