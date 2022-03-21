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

import org.gradle.api.publish.plugins.PublishingPlugin.PUBLISH_TASK_GROUP
import tools.aqua.MavenMetadataExtension
import tools.aqua.apache2
import tools.aqua.developers
import tools.aqua.github

plugins {
  id("tools.aqua.koral.base-conventions")

  `maven-publish`
  signing
}

val mavenMetadata = extensions.create<MavenMetadataExtension>("mavenMetadata")

publishing {
  publications {
    create<MavenPublication>("maven") {
      from(components["java"])

      pom {
        name.set(mavenMetadata.name)
        description.set(mavenMetadata.description)

        github("tudo-aqua", "koral")

        licenses { apache2() }

        developers(
            "Simon Dierl" to "simon.dierl@cs.tu-dortmund.de",
        )
      }
    }
  }
}

signing {
  setRequired { gradle.taskGraph.allTasks.any { it.group == PUBLISH_TASK_GROUP } }
  useGpgCmd()
  sign(publishing.publications["maven"])
}
