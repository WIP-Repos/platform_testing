/*
 * Copyright (C) 2021 The Android Open Source Project
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

package com.android.server.wm.flicker.dsl

import android.app.Instrumentation
import android.support.test.launcherhelper.ILauncherStrategy
import android.support.test.launcherhelper.LauncherStrategyFactory
import androidx.test.uiautomator.UiDevice
import com.android.server.wm.flicker.Flicker
import com.android.server.wm.flicker.FlickerDslMarker
import com.android.server.wm.flicker.TransitionRunner
import com.android.server.wm.flicker.getDefaultFlickerOutputDir
import com.android.server.wm.flicker.monitor.EventLogMonitor
import com.android.server.wm.flicker.monitor.ITransitionMonitor
import com.android.server.wm.flicker.monitor.LayersTraceMonitor
import com.android.server.wm.flicker.monitor.NoTraceMonitor
import com.android.server.wm.flicker.monitor.ScreenRecorder
import com.android.server.wm.flicker.monitor.TransactionsTraceMonitor
import com.android.server.wm.flicker.monitor.TransitionsTraceMonitor
import com.android.server.wm.flicker.monitor.WindowManagerTraceMonitor
import com.android.server.wm.traces.common.layers.BaseLayerTraceEntry
import com.android.server.wm.traces.common.layers.LayersTrace
import com.android.server.wm.traces.common.windowmanager.WindowManagerState
import com.android.server.wm.traces.common.windowmanager.WindowManagerTrace
import com.android.server.wm.traces.parser.windowmanager.WindowManagerStateHelper
import java.io.File
import java.nio.file.Path

/**
 * Build Flicker tests using Flicker DSL
 */
@FlickerDslMarker
class FlickerBuilder private constructor(
    internal val instrumentation: Instrumentation,
    private val launcherStrategy: ILauncherStrategy,
    private val outputDir: Path,
    private val wmHelper: WindowManagerStateHelper,
    private var testName: String,
    private var iterations: Int,
    private val setupCommands: TestCommandsBuilder,
    private val teardownCommands: TestCommandsBuilder,
    private val transitionCommands: MutableList<Flicker.() -> Any>,
    val device: UiDevice,
    private val traceMonitors: MutableList<ITransitionMonitor>,
    private var faasEnabled: Boolean = false
) {
    private var usingExistingTraces = false

    /**
     * Default flicker builder constructor
     */
    @JvmOverloads
    constructor(
        /**
         * Instrumentation to run the tests
         */
        instrumentation: Instrumentation,
        /**
         * Strategy used to interact with the launcher
         */
        launcherStrategy: ILauncherStrategy = LauncherStrategyFactory
            .getInstance(instrumentation).launcherStrategy,
        /**
         * Output directory for the test results
         */
        outputDir: Path = getDefaultFlickerOutputDir(),
        /**
         * Helper object for WM Synchronization
         */
        wmHelper: WindowManagerStateHelper = WindowManagerStateHelper(instrumentation),
        traceMonitors: MutableList<ITransitionMonitor> = mutableListOf<ITransitionMonitor>()
            .also {
                it.add(WindowManagerTraceMonitor(outputDir))
                it.add(LayersTraceMonitor(outputDir))
                it.add(TransitionsTraceMonitor(outputDir))
                it.add(TransactionsTraceMonitor(outputDir))
                it.add(ScreenRecorder(instrumentation.targetContext, outputDir))
                it.add(EventLogMonitor())
            }
    ) : this(
        instrumentation,
        launcherStrategy,
        outputDir,
        wmHelper,
        testName = "",
        iterations = 1,
        setupCommands = TestCommandsBuilder(),
        teardownCommands = TestCommandsBuilder(),
        transitionCommands = mutableListOf(),
        device = UiDevice.getInstance(instrumentation),
        traceMonitors = traceMonitors
    )

    /**
     * Copy constructor
     */
    constructor(otherBuilder: FlickerBuilder) : this(
        otherBuilder.instrumentation,
        otherBuilder.launcherStrategy,
        otherBuilder.outputDir.toAbsolutePath(),
        otherBuilder.wmHelper,
        otherBuilder.testName,
        otherBuilder.iterations,
        TestCommandsBuilder(otherBuilder.setupCommands),
        TestCommandsBuilder(otherBuilder.teardownCommands),
        otherBuilder.transitionCommands.toMutableList(),
        UiDevice.getInstance(otherBuilder.instrumentation),
        otherBuilder.traceMonitors.toMutableList(),
        faasEnabled = otherBuilder.faasEnabled
    )

    /**
     * Test name used to store the test results
     *
     * If reused throughout the test, only the last value is stored
     */
    fun withTestName(testName: () -> String): FlickerBuilder = apply {
        val name = testName()
        require(!name.contains(" ")) {
            "The test tag can not contain spaces since it is a part of the file name"
        }
        this.testName = name
    }

    /**
     * Disable [WindowManagerTraceMonitor].
     */
    fun withoutWindowManagerTracing(): FlickerBuilder = apply {
        withWindowManagerTracing { null }
    }

    /**
     * Configure a [WindowManagerTraceMonitor] to obtain [WindowManagerTrace]
     *
     * By default the tracing is always active. To disable tracing return null
     *
     * If this tracing is disabled, the assertions for [WindowManagerTrace] and
     * [WindowManagerState] will not be executed
     */
    fun withWindowManagerTracing(
        traceMonitor: (Path) -> WindowManagerTraceMonitor?
    ): FlickerBuilder = apply {
        traceMonitors.removeIf { it is WindowManagerTraceMonitor }
        addMonitor(traceMonitor(outputDir))
    }

    /**
     * Disable [LayersTraceMonitor].
     */
    fun withoutLayerTracing(): FlickerBuilder = apply {
        withLayerTracing { null }
    }

    /**
     * Configure a [LayersTraceMonitor] to obtain [LayersTrace].
     *
     * By default the tracing is always active. To disable tracing return null
     *
     * If this tracing is disabled, the assertions for [LayersTrace] and [BaseLayerTraceEntry]
     * will not be executed
     */
    fun withLayerTracing(
        traceMonitor: (Path) -> LayersTraceMonitor?
    ): FlickerBuilder = apply {
        traceMonitors.removeIf { it is LayersTraceMonitor }
        addMonitor(traceMonitor(outputDir))
    }

    /**
     * Disable [TransitionsTraceMonitor].
     */
    fun withoutTransitionTracing(): FlickerBuilder = apply {
        withTransitionTracing { null }
    }

    /**
     * Configure a [TransitionsTraceMonitor] to obtain [TransitionsTrace].
     *
     * By default shell transition tracing is disabled.
     */
    fun withTransitionTracing(
        traceMonitor: (Path) -> TransitionsTraceMonitor?
    ): FlickerBuilder = apply {
        traceMonitors.removeIf { it is TransitionsTraceMonitor }
        addMonitor(traceMonitor(outputDir))
    }

    /**
     * Disable [TransactionsTraceMonitor].
     */
    fun withoutTransactionsTracing(): FlickerBuilder = apply {
        withTransactionsTracing { null }
    }

    /**
     * Configure a [TransactionsTraceMonitor] to obtain [TransactionsTrace].
     *
     * By default shell transition tracing is disabled.
     */
    fun withTransactionsTracing(
        traceMonitor: (Path) -> TransactionsTraceMonitor?
    ): FlickerBuilder = apply {
        traceMonitors.removeIf { it is TransactionsTraceMonitor }
        addMonitor(traceMonitor(outputDir))
    }

    /**
     * Configure a [ScreenRecorder].
     *
     * By default the tracing is always active. To disable tracing return null
     */
    fun withScreenRecorder(
        screenRecorder: (Path) -> ScreenRecorder?
    ): FlickerBuilder = apply {
        traceMonitors.removeIf { it is ScreenRecorder }
        addMonitor(screenRecorder(outputDir))
    }

    /**
     * Defines how many times the test run should be repeated
     */
    fun repeat(predicate: () -> Int): FlickerBuilder = apply {
        val repeat = predicate()

        require(!usingExistingTraces || repeat == 1) {
            "Repetitions are not supported with usingExistingTraces"
        }

        require(repeat >= 1) { "Number of repetitions should be greater or equal to 1" }
        iterations = repeat
    }

    fun withFlickerAsAService(predicate: () -> Boolean): FlickerBuilder = apply {
        faasEnabled = predicate()
    }

    /**
     * Defines the test ([TestCommandsBuilder.testCommands]) and run ([TestCommandsBuilder.runCommands])
     * commands executed before the [transitions] to test
     */
    fun setup(commands: TestCommandsBuilder.() -> Unit): FlickerBuilder = apply {
        setupCommands.apply { commands() }
    }

    /**
     * Defines the test ([TestCommandsBuilder.testCommands]) and run ([TestCommandsBuilder.runCommands])
     * commands executed after the [transitions] to test
     */
    fun teardown(commands: TestCommandsBuilder.() -> Unit): FlickerBuilder = apply {
        teardownCommands.apply { commands() }
    }

    /**
     * Defines the commands that trigger the behavior to test
     */
    fun transitions(command: Flicker.() -> Unit): FlickerBuilder = apply {
        require(!usingExistingTraces) {
            "Can't update transition after calling usingExistingTraces"
        }
        transitionCommands.add(command)
    }

    data class TraceFiles(
        val wmTrace: File,
        val layersTrace: File,
        val transactions: File,
        val transitions: File
    )

    /**
     * Use pre-executed results instead of running transitions to get the traces
     */
    fun usingExistingTraces(_traceFiles: () -> TraceFiles): FlickerBuilder = apply {
        val traceFiles = _traceFiles()
        // Remove all trace monitor and use only monitor that read from existing trace file
        this.traceMonitors.clear()
        addMonitor(NoTraceMonitor { it.setWmTrace(traceFiles.wmTrace) })
        addMonitor(NoTraceMonitor { it.setLayersTrace(traceFiles.layersTrace) })
        addMonitor(NoTraceMonitor { it.setTransactionsTrace(traceFiles.transactions) })
        addMonitor(NoTraceMonitor { it.setTransitionsTrace(traceFiles.transitions) })

        // Remove all transitions execution
        this.transitionCommands.clear()

        // Set to one iteration since we are only providing one iteration
        // We don't support more than 1 iteration for this. We will be deprecating iterations so
        // we don't plan to support it in the future either.
        this.iterations = 1

        this.usingExistingTraces = true
    }

    /**
     * Creates a new Flicker runner based on the current builder configuration
     */
    @JvmOverloads
    fun build(runner: TransitionRunner = TransitionRunner()): Flicker {
        require(testName.isNotEmpty()) {
            "Test name must be provided by calling .withTestName {} on builder"
        }

        return Flicker(
            instrumentation,
            device,
            launcherStrategy,
            outputDir,
            testName,
            iterations,
            traceMonitors,
            setupCommands.buildTestCommands(),
            setupCommands.buildRunCommands(),
            teardownCommands.buildTestCommands(),
            teardownCommands.buildRunCommands(),
            transitionCommands,
            runner,
            wmHelper,
            faasEnabled = faasEnabled
        )
    }

    /**
     * Returns a copy of the current builder with the changes of [block] applied
     */
    fun copy(block: FlickerBuilder.() -> Unit) = FlickerBuilder(this).apply(block)

    private fun addMonitor(newMonitor: ITransitionMonitor?) {
        require(!usingExistingTraces) { "Can't add monitors after calling usingExistingTraces" }

        if (newMonitor != null) {
            traceMonitors.add(newMonitor)
        }
    }
}
