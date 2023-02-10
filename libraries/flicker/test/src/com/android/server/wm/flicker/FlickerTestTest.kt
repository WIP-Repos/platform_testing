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

package com.android.server.wm.flicker

import android.annotation.SuppressLint
import com.android.server.wm.InitRule
import com.android.server.wm.flicker.datastore.CachedResultReader
import com.android.server.wm.flicker.datastore.DataStore
import com.android.server.wm.flicker.io.ResultReader
import com.android.server.wm.flicker.io.TraceType
import com.google.common.truth.Truth
import java.io.File
import org.junit.Before
import org.junit.ClassRule
import org.junit.Test

/** Tests for [FlickerTest] */
@SuppressLint("VisibleForTests")
class FlickerTestTest {
    private var executionCount = 0

    @Before
    fun setup() {
        executionCount = 0
        outputFileName(RunStatus.RUN_EXECUTED).deleteIfExists()
        DataStore.clear()
    }

    @Test
    fun failsWithoutScenario() {
        val actual = FlickerTest()
        val failure =
            assertThrows<IllegalArgumentException> { actual.assertLayers { executionCount++ } }
        assertExceptionMessage(failure, "Scenario shouldn't be empty")
        Truth.assertWithMessage("Executed").that(executionCount).isEqualTo(0)
    }

    @Test
    fun executesLayers() {
        val predicate: (FlickerTest) -> Unit = { it.assertLayers { executionCount++ } }
        doWriteTraceExecuteAssertionAndVerify(
            TraceType.SF,
            predicate,
            TestTraces.LayerTrace.FILE,
            expectedExecutionCount = 2
        )
    }

    @Test
    fun executesLayerStart() {
        val predicate: (FlickerTest) -> Unit = { it.assertLayersStart { executionCount++ } }
        doWriteTraceExecuteAssertionAndVerify(
            TraceType.SF,
            predicate,
            TestTraces.LayerTrace.FILE,
            expectedExecutionCount = 2
        )
    }

    @Test
    fun executesLayerEnd() {
        val predicate: (FlickerTest) -> Unit = { it.assertLayersEnd { executionCount++ } }
        doWriteTraceExecuteAssertionAndVerify(
            TraceType.SF,
            predicate,
            TestTraces.LayerTrace.FILE,
            expectedExecutionCount = 2
        )
    }

    @Test
    fun doesNotExecuteLayersWithoutTrace() {
        val predicate: (FlickerTest) -> Unit = { it.assertLayers { executionCount++ } }
        doExecuteAssertionWithoutTraceAndVerifyNotExecuted(TraceType.SF, predicate)
    }

    @Test
    fun doesNotExecuteLayersStartWithoutTrace() {
        val predicate: (FlickerTest) -> Unit = { it.assertLayersStart { executionCount++ } }
        doExecuteAssertionWithoutTraceAndVerifyNotExecuted(TraceType.SF, predicate)
    }

    @Test
    fun doesNotExecuteLayersEndWithoutTrace() {
        val predicate: (FlickerTest) -> Unit = { it.assertLayersEnd { executionCount++ } }
        doExecuteAssertionWithoutTraceAndVerifyNotExecuted(TraceType.SF, predicate)
    }

    @Test
    fun doesNotExecuteLayerTagWithoutTag() {
        val predicate: (FlickerTest) -> Unit = { it.assertLayersTag("tag") { executionCount++ } }
        doExecuteAssertionWithoutTraceAndVerifyNotExecuted(TraceType.SF, predicate)
    }

    @Test
    fun executesWm() {
        val predicate: (FlickerTest) -> Unit = { it.assertWm { executionCount++ } }
        doWriteTraceExecuteAssertionAndVerify(
            TraceType.WM,
            predicate,
            TestTraces.WMTrace.FILE,
            expectedExecutionCount = 2
        )
    }

    @Test
    fun executesWmStart() {
        val predicate: (FlickerTest) -> Unit = { it.assertWmStart { executionCount++ } }
        doWriteTraceExecuteAssertionAndVerify(
            TraceType.WM,
            predicate,
            TestTraces.WMTrace.FILE,
            expectedExecutionCount = 2
        )
    }

    @Test
    fun executesWmEnd() {
        val predicate: (FlickerTest) -> Unit = { it.assertWmEnd { executionCount++ } }
        doWriteTraceExecuteAssertionAndVerify(
            TraceType.WM,
            predicate,
            TestTraces.WMTrace.FILE,
            expectedExecutionCount = 2
        )
    }

    @Test
    fun doesNotExecuteWmWithoutTrace() {
        val predicate: (FlickerTest) -> Unit = { it.assertWm { executionCount++ } }
        doExecuteAssertionWithoutTraceAndVerifyNotExecuted(TraceType.WM, predicate)
    }

    @Test
    fun doesNotExecuteWmStartWithoutTrace() {
        val predicate: (FlickerTest) -> Unit = { it.assertWmStart { executionCount++ } }
        doExecuteAssertionWithoutTraceAndVerifyNotExecuted(TraceType.WM, predicate)
    }

    @Test
    fun doesNotExecuteWmEndWithoutTrace() {
        val predicate: (FlickerTest) -> Unit = { it.assertWmEnd { executionCount++ } }
        doExecuteAssertionWithoutTraceAndVerifyNotExecuted(TraceType.WM, predicate)
    }

    @Test
    fun doesNotExecuteWmTagWithoutTag() {
        val predicate: (FlickerTest) -> Unit = { it.assertWmTag("tag") { executionCount++ } }
        doWriteTraceExecuteAssertionAndVerify(
            TraceType.WM,
            predicate,
            TestTraces.WMTrace.FILE,
            expectedExecutionCount = 0
        )
    }

    @Test
    fun executesEventLog() {
        val predicate: (FlickerTest) -> Unit = { it.assertEventLog { executionCount++ } }
        doWriteTraceExecuteAssertionAndVerify(
            TraceType.EVENT_LOG,
            predicate,
            TestTraces.EventLog.FILE,
            expectedExecutionCount = 2
        )
    }

    @Test
    fun doesNotExecuteEventLogWithoutEventLog() {
        val predicate: (FlickerTest) -> Unit = { it.assertEventLog { executionCount++ } }
        newTestCachedResultWriter().write()
        val flickerWrapper = FlickerTest()
        flickerWrapper.initialize(TEST_SCENARIO.testClass)
        // Each assertion is executed independently and not cached, only Flicker as a Service
        // assertions are cached
        predicate.invoke(flickerWrapper)
        predicate.invoke(flickerWrapper)

        Truth.assertWithMessage("Executed").that(executionCount).isEqualTo(0)
    }

    private fun doExecuteAssertionWithoutTraceAndVerifyNotExecuted(
        traceType: TraceType,
        predicate: (FlickerTest) -> Unit
    ) =
        doWriteTraceExecuteAssertionAndVerify(
            traceType,
            predicate,
            file = null,
            expectedExecutionCount = 0
        )

    private fun doWriteTraceExecuteAssertionAndVerify(
        traceType: TraceType,
        predicate: (FlickerTest) -> Unit,
        file: File?,
        expectedExecutionCount: Int
    ) {
        val writer = newTestCachedResultWriter()
        if (file != null) {
            writer.addTraceResult(traceType, file)
        }
        writer.write()
        val flickerWrapper =
            FlickerTest(
                resultReaderProvider = {
                    CachedResultReader(
                        it,
                        DEFAULT_TRACE_CONFIG,
                        reader = ResultReader(DataStore.getResult(it), DEFAULT_TRACE_CONFIG)
                    )
                }
            )
        flickerWrapper.initialize(TEST_SCENARIO.testClass)
        // Each assertion is executed independently and not cached, only Flicker as a Service
        // assertions are cached
        predicate.invoke(flickerWrapper)
        predicate.invoke(flickerWrapper)

        Truth.assertWithMessage("Executed").that(executionCount).isEqualTo(expectedExecutionCount)
    }

    companion object {
        @ClassRule @JvmField val initRule = InitRule()
    }
}
