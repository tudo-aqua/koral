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

package tools.aqua.koral.serialization.automatawiki;

import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlElementWrapper;
import jakarta.xml.bind.annotation.XmlType;
import jakarta.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import java.util.ArrayList;
import java.util.List;

@XmlType
public class AWTransition {

  @XmlAttribute(name = "from", required = true)
  String from;

  @XmlAttribute(name = "to", required = true)
  String to;

  @XmlAttribute(name = "symbol", required = true)
  String symbol;

  @XmlAttribute(name = "params")
  @XmlJavaTypeAdapter(CSVAdapter.class)
  List<String> params = new ArrayList<>();

  @XmlElement(name = "guard")
  @XmlJavaTypeAdapter(GuardAdapter.class)
  Guard guard = True.INSTANCE;

  @XmlElementWrapper(name = "assignments")
  @XmlElement(name = "assign")
  List<AWAssignment> assignments = new ArrayList<>();
}
