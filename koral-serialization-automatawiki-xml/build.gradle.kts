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

plugins {
  id("tools.aqua.koral.jaxb-conventions")
  id("tools.aqua.koral.library-conventions")
}

mavenMetadata {
  name.set("Koral RA AutomataWiki Serialization (XML Conversion)")
  description.set(
      "Serialization and Deserialization for RAs in the AutomataWiki XML format (XML Conversion via JAXB).")
}

dependencies { api(project(":koral-serialization-automatawiki-util")) }

jaxb {
  schemaGen {
    register("automataWiki") {
      inputDir = projectDir.resolve("src/main/java")
      include("tools/aqua/koral/serialization/automatawiki/**/*.java")

      val schemaName = "ra.xsd"
      namespaceconfigs = mapOf("" to schemaName)

      // add to JAR
      sourceSets.main { resources.srcDir(outputDir) }

      // make schemagen write to correct directory
      outputDir = outputDir.resolve("tools/aqua/koral/serialization/automatawiki")

      // add as standalone publication
      val artifact =
          artifacts.add("default", outputDir.resolve(schemaName)) { classifier = "ra-schema" }

      publishing {
        publications { publications.named<MavenPublication>("maven") { artifact(artifact) } }
      }
    }
  }
}

tasks.processResources { dependsOn(tasks.withType(JavaToSchemaTask::class)) }
