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

import com.intershop.gradle.jaxb.task.JavaToSchemaTask
import com.intershop.gradle.jaxb.task.SchemaToJavaTask
import java.util.*
import java.util.Locale.ENGLISH
import java.util.Locale.ROOT
import org.gradle.accessors.dm.LibrariesForLibs

plugins {
  id("tools.aqua.koral.base-conventions")
  id("tools.aqua.koral.java-conventions")
  id("com.intershop.gradle.jaxb")
}

// black magic from https://github.com/gradle/gradle/issues/15383
val libs = the<LibrariesForLibs>()

dependencies {
  implementation(libs.jakarta.annotations)
  implementation(libs.jakarta.bind)
}

fun Task.warnForeignLocale() {
  val locale: Locale = Locale.getDefault()
  if (locale != ROOT && locale.language != ENGLISH.language) {
    logger.warn(
        "The current locale $locale is non-english. Generated files will likely be localized.")
  }
}

tasks.withType<JavaToSchemaTask> { doFirst { warnForeignLocale() } }

tasks.withType<SchemaToJavaTask> { doFirst { warnForeignLocale() } }