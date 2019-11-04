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
package com.linkedin.android.testbutler;

import android.accessibilityservice.AccessibilityService;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityManager;

public class ButlerAccessibilityService extends AccessibilityService {

    static final String SERVICE_NAME = ButlerAccessibilityService.class.getSimpleName();

    // From com.android.server.accessibility.AccessibilityManagerService#COMPONENT_NAME_SEPARATOR
    static final String COMPONENT_NAME_SEPARATOR = ":";

    static final long CREATE_DESTROY_TIMEOUT = 30000;

    private static final Object sInstanceCreateLock = new Object();
    private static final Object sInstanceDestroyLock = new Object();

    private static ButlerAccessibilityService sInstance;

    private AccessibilityManager accessibilityManager;

    @Override
    public void onCreate() {
        super.onCreate();
        accessibilityManager = (AccessibilityManager) this.getSystemService(ACCESSIBILITY_SERVICE);
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        // stub
    }

    @Override
    public void onInterrupt() {
        // stub
    }

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        sInstance = this;
        if (accessibilityManager.isEnabled()) {
            synchronized (sInstanceCreateLock) {
                sInstanceCreateLock.notifyAll();
            }
        } else {
            accessibilityManager.addAccessibilityStateChangeListener(new AccessibilityManager.AccessibilityStateChangeListener() {
                @Override
                public void onAccessibilityStateChanged(boolean enabled) {
                    if (enabled) {
                        synchronized (sInstanceCreateLock) {
                            sInstanceCreateLock.notifyAll();
                        }
                    }
                }
            });
        }

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        sInstance = null;
        if (!accessibilityManager.isEnabled()) {
            synchronized (sInstanceDestroyLock) {
                sInstanceDestroyLock.notifyAll();
            }
        } else {
            accessibilityManager.addAccessibilityStateChangeListener(new AccessibilityManager.AccessibilityStateChangeListener() {
                @Override
                public void onAccessibilityStateChanged(boolean enabled) {
                    if (!enabled) {
                        synchronized (sInstanceDestroyLock) {
                            sInstanceDestroyLock.notifyAll();
                        }
                    }
                }
            });
        }
    }

    static boolean waitForLaunch() {
        if (sInstance == null) {
            try {
                synchronized (sInstanceCreateLock) {
                    sInstanceCreateLock.wait(CREATE_DESTROY_TIMEOUT);
                }
            } catch (InterruptedException ignored) { }
        }
        return sInstance != null;
    }

    static boolean waitForDestroy() {
        if (sInstance != null) {
            try {
                synchronized (sInstanceDestroyLock) {
                    sInstanceDestroyLock.wait(CREATE_DESTROY_TIMEOUT);
                }
            } catch (InterruptedException ignored) { }
        }

        return sInstance == null;
    }
}
