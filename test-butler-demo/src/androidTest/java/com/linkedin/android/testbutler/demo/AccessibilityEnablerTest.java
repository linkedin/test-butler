/**
 * Copyright (C) 2019 LinkedIn Corp.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.linkedin.android.testbutler.demo;

import android.accessibilityservice.AccessibilityServiceInfo;
import android.content.Context;
import android.view.accessibility.AccessibilityManager;

import androidx.test.core.app.ApplicationProvider;

import com.linkedin.android.testbutler.TestButler;

import org.junit.Before;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class AccessibilityEnablerTest {

    private AccessibilityManager accessibilityManager;

    @Before
    public void setup() {
        Context context = ApplicationProvider.getApplicationContext();
        this.accessibilityManager =
                (AccessibilityManager) context.getSystemService(Context.ACCESSIBILITY_SERVICE);
    }

    @Test
    public void startAndStopAccessibility() {
        TestButler.setAccessibilityServiceState(true);
        assertTrue(isButlerAccessibilityServiceEnabled());

        TestButler.setAccessibilityServiceState(false);
        assertFalse(isButlerAccessibilityServiceEnabled());
    }

    private boolean isButlerAccessibilityServiceEnabled() {
        List<AccessibilityServiceInfo> serviceInfoList = accessibilityManager
                .getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_SPOKEN);
        for (AccessibilityServiceInfo info : serviceInfoList) {
            if (info.getId().endsWith("ButlerAccessibilityService")) {
                return true;
            }
        }
        return false;
    }
}
