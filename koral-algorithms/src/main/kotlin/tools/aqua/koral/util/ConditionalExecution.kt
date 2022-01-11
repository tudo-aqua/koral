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

import kotlin.reflect.KMutableProperty0

/**
 * Conditionally execute a code block if a given property (the *guard*) is `false`. After execution
 * has completed normally, set the guard to `true`. This can be used to elide an expensive
 * computation.
 *
 * @param guard a reference to the guarding property.
 * @param function the code to conditionally execute.
 */
fun computeGuarded(guard: KMutableProperty0<Boolean>, function: () -> Unit) {
  if (guard.get()) return
  function()
  guard.set(true)
}
