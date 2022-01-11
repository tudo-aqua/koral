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

package tools.aqua.koral.util

import tools.aqua.koral.util.Tag.*

internal val abIndex = listOf("a", "b").index()
internal val abcIndex = listOf("a", "b", "c").index()
internal val xyIndex = listOf("x", "y").index()
internal val xyzIndex = listOf("x", "y", "z").index()

internal val xSource = SOURCE_REGISTER("x")
internal val ySource = SOURCE_REGISTER("y")
internal val zSource = SOURCE_REGISTER("z")
internal val aParameter = PARAMETER("a")
internal val bParameter = PARAMETER("b")
internal val xDestination = DESTINATION_REGISTER("x")
internal val yDestination = DESTINATION_REGISTER("y")

internal val xySourceSet = setOf(xSource, ySource)
internal val abParameterSet = setOf(aParameter, bParameter)
internal val xyDestinationSet = setOf(xDestination, yDestination)
