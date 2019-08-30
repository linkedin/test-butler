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
package com.linkedin.android.testbutler.shell;

import android.app.UiAutomation;
import android.os.Build;
import android.os.HandlerThread;
import android.os.Looper;
import android.util.Log;
import android.view.accessibility.AccessibilityNodeInfo;

import androidx.annotation.RequiresApi;

import java.io.Closeable;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;


/**
 * A wrapper to expose hidden APIs in IUiAutomationConnection via reflection.
 *
 * This class implements Closeable so that it may be used in a try-with-resources statement.
 * This is highly recommended, as failure to call {@link #close()} will prevent new
 * UiAutomationConnections from being created on the device.
 */
class UiAutomationConnectionWrapper implements Closeable {

    private static final String TAG = UiAutomationConnectionWrapper.class.getSimpleName();

    private final Method disconnectMethod;
    private final UiAutomation uiAutomation;
    private final HandlerThread thread;

    private UiAutomationConnectionWrapper(Method disconnectMethod, UiAutomation uiAutomation, HandlerThread thread) {
        this.disconnectMethod = disconnectMethod;
        this.uiAutomation = uiAutomation;
        this.thread = thread;
    }

    /**
     * Creates and connects a new UiAutomationConnection with a dedicated HandlerThread.
     * @return A UiAutomationWrapper
     */
    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    static UiAutomationConnectionWrapper newInstance() throws Exception {
        try {
            Class<?> iUiAutomationConnection = Class.forName("android.app.IUiAutomationConnection");
            Class<?> uiAutomationConnectionClass = Class.forName("android.app.UiAutomationConnection");
            Constructor<UiAutomation> uiAutomationConstructor = UiAutomation.class.getConstructor(Looper.class, iUiAutomationConnection);
            Method connectMethod = UiAutomation.class.getMethod("connect");
            Method disconnectMethod = UiAutomation.class.getMethod("disconnect");

            HandlerThread thread = new HandlerThread("UiAutomatorHandlerThread");
            thread.start();
            try {
                Object uiAutomationConnection = uiAutomationConnectionClass.newInstance();
                UiAutomation uiAutomation = uiAutomationConstructor.newInstance(thread.getLooper(), uiAutomationConnection);
                connectMethod.invoke(uiAutomation);
                return new UiAutomationConnectionWrapper(disconnectMethod, uiAutomation, thread);
            } catch (Exception e) {
                thread.quit();
                throw e;
            }
        } catch (Exception e) {
            throw new Exception("Failed to initialize UiAutomationWrapper", e);
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    AccessibilityNodeInfo getRootInActiveWindow() {
        return uiAutomation.getRootInActiveWindow();
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    @Override
    public void close() {
        try {
            disconnectMethod.invoke(uiAutomation);
            thread.quit();
        } catch (Exception e) {
            Log.e(TAG, "Failed to disconnect UIAutomation", e);
        }
    }
}
