/*
 * Copyright (C) 2023 The Android Open Source Project
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
package android.tools.traces.inputmethod

import android.tools.datatypes.Rect
import android.tools.traces.wm.DisplayCutout
import android.tools.traces.wm.WindowLayoutParams

/**
 * Represents the ViewRootImplProto in IME traces
 *
 * This is a generic object that is reused by both Flicker and Winscope and cannot access internal
 * Java/Android functionality
 */
data class ViewRootImpl(
    val view: String,
    val displayId: Int,
    val appVisible: Boolean,
    val width: Int,
    val height: Int,
    val isAnimating: Boolean,
    val visibleRect: Rect,
    val isDrawing: Boolean,
    val added: Boolean,
    val winFrame: Rect,
    val pendingDisplayCutout: DisplayCutout,
    val lastWindowInsets: String,
    val softInputMode: String,
    val scrollY: Int,
    val curScrollY: Int,
    val removed: Boolean,
    val windowAttributes: WindowLayoutParams,
)
