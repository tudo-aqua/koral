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

import tools.aqua.automataWiki

plugins {
  id("tools.aqua.koral.kotlin-conventions")
  id("tools.aqua.koral.executable-conventions")
  kotlin("plugin.serialization")
}

mavenMetadata {
  name.set("Koral Experiment Launcher")
  description.set("CLI entry point for experiments.")
}

repositories { automataWiki() }

val ra = libs.automatawiki.ra.get()

dependencies {
  implementation(project(":koral-algorithms"))
  implementation(project(":koral-serialization-automatawiki"))
  implementation(project(":koral-serialization-graphviz"))
  implementation(libs.clikt)
  implementation(libs.kotlinx.serialization.json)
  implementation(ra.module.group, ra.module.name, ra.versionConstraint.requiredVersion, ext = "zip")
}

application {
  mainClass.set("tools.aqua.koral.cli.ExperimentCLI")
  applicationName = "koral-experiment"
  applicationDefaultJvmArgs = listOf("-Xmx32G")
}
