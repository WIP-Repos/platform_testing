/*
 * Copyright (C) 2022 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.server.wm.flicker.service.assertors

import com.android.server.wm.flicker.assertions.Fact
import com.android.server.wm.flicker.helpers.format
import com.android.server.wm.flicker.service.ScenarioInstance
import com.android.server.wm.traces.common.layers.LayersTrace
import com.android.server.wm.traces.common.windowmanager.WindowManagerTrace

data class FaasData(
    val scenarioInstance: ScenarioInstance,
    val entireWmTrace: WindowManagerTrace,
    val entireLayersTrace: LayersTrace
) {
    fun toFacts(): Collection<Fact> {
        return mutableListOf(
                Fact("Extracted from WM trace start", entireWmTrace.first().timestamp.format()),
                Fact("Extracted from WM trace end", entireWmTrace.first().timestamp.format()),
                Fact("Extracted from SF trace start", entireLayersTrace.first().timestamp.format()),
                Fact("Extracted from SF trace end", entireLayersTrace.first().timestamp.format()),
                Fact("Scenario description", scenarioInstance.description),
                Fact("Scenario rotation", scenarioInstance.startRotation),
                Fact("Scenario start", "${scenarioInstance.startTimestamp}"),
                Fact("Scenario end", "${scenarioInstance.endTimestamp}")
            )
            .apply {
                if (scenarioInstance.associatedTransition != null) {
                    this.add(
                        Fact(
                            "Associated transition type",
                            scenarioInstance.associatedTransition.type
                        )
                    )
                    this.add(
                        Fact(
                            "Associated transition changes",
                            scenarioInstance.associatedTransition.changes.joinToString(
                                "\n  -",
                                "\n  -"
                            ) { "${it.transitMode} ${it.windowName}" }
                        )
                    )
                }
                if (scenarioInstance.associatedCuj !== null) {
                    this.add(Fact("Associated CUJ", scenarioInstance.associatedCuj))
                }
            }
    }
}
