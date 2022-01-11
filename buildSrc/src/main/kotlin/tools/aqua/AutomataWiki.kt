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

package tools.aqua

import java.net.URI
import org.gradle.api.artifacts.dsl.RepositoryHandler
import org.gradle.api.artifacts.repositories.IvyArtifactRepository

/**
 * Add the automata wiki downloads as a faked Ivy repository. The artifact components are handled as
 * follows:
 * - the `group` *must* be `nl.ru.cs.automata`,
 * - the downloaded file is `artifact.ext`, and
 * - all other components are ignored.
 */
fun RepositoryHandler.automataWiki(): IvyArtifactRepository = ivy {
  name = "AutomataWiki"
  url = URI("https://automata.cs.ru.nl/automata_pmwiki/uploads/Main/")
  content { includeGroup("nl.ru.cs.automata") }
  patternLayout { artifact("[artifact].[ext]") }
  metadataSources { artifact() }
}
