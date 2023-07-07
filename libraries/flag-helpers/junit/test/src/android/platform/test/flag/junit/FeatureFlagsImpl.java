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

package android.platform.test.flag.junit;

import java.util.HashMap;

/** A Fake FeatureFlagsImpl to test the {@code MockFlagsRule}. */
public class FeatureFlagsImpl implements FeatureFlags {

    private HashMap<String, Boolean> mFlagMap = new HashMap<>();

    public FeatureFlagsImpl() {
        this.mFlagMap.put("android.platform.test.flag.junit.flagName3", null);
        this.mFlagMap.put("android.platform.test.flag.junit.flagName4", null);
    }

    /** Returns the flag value. */
    @Override
    public boolean flagName3() {
        return this.mFlagMap.get("android.platform.test.flag.junit.flagName3");
    }

    /** another flag */
    @Override
    public boolean flagName4() {
        return this.mFlagMap.get("android.platform.test.flag.junit.flagName4");
    }

    public void setFlag(String flag, boolean value) {
        this.mFlagMap.put(flag, value);
    }

    public void resetAll() {
        this.mFlagMap.clear();
    }
}
