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

import org.gradle.api.JavaVersion.VERSION_1_8

plugins { `kotlin-dsl` }

repositories {
  gradlePluginPortal()
  mavenCentral()
}

dependencies {
  implementation(libs.bundles.gradle.kotlin.full)

  implementation(libs.gradle.bmVersions)
  implementation(libs.gradle.dependencyManagement)
  implementation(libs.gradle.detekt)
  implementation(libs.gradle.gitVersioning)
  implementation(libs.gradle.jaxb)
  implementation(libs.gradle.kover)
  implementation(libs.gradle.kotlin.serialization)
  implementation(libs.gradle.nexusPublish)
  implementation(libs.gradle.spotless)
  implementation(libs.gradle.taskTree)

  // black magic from https://github.com/gradle/gradle/issues/15383
  implementation(files(libs.javaClass.superclass.protectionDomain.codeSource.location))
}

java {
  sourceCompatibility = VERSION_1_8
  targetCompatibility = VERSION_1_8
}
