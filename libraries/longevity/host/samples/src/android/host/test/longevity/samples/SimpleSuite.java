/*
 * Copyright (C) 2017 The Android Open Source Project
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

package android.host.test.longevity.samples;

import android.host.test.longevity.LongevitySuite;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.junit.runners.Suite.SuiteClasses;

@RunWith(LongevitySuite.class)
@SuiteClasses({
    SimpleSuite.PassingTest.class,
    SimpleSuite.FailingTest.class
})
/**
 * Sample host-side test cases.
 */
public class SimpleSuite {
    // no local test cases.

    @RunWith(JUnit4.class)
    public static class PassingTest {
        @Test
        public void testAssertEquals() {
            Assert.assertEquals(1, 1);
        }
    }

    @RunWith(JUnit4.class)
    public static class FailingTest {
        @Test
        public void testAssertEquals() {
            Assert.assertEquals(1, 2);
        }
    }
}
