/**
 * Copyright (C) 2016 LinkedIn Corp.
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

import android.app.Instrumentation;
import android.os.IBinder;
import android.util.Log;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;

/**
 * A helper class for enabling/disabling Animations before/after running Espresso tests.
 * <p>
 * Google recommends that animations are disabled when Espresso tests are being run:
 * https://code.google.com/p/android-test-kit/wiki/Espresso#Getting_Started
 */
final class AnimationDisabler {

    private static String TAG = AnimationDisabler.class.getSimpleName();

    private static final float DISABLED = 0.0f;

    private float[] originalScaleFactors;

    private Method setAnimationScalesMethod;
    private Method getAnimationScalesMethod;
    private Object windowManagerObject;

    AnimationDisabler() {
        try {
            Class<?> windowManagerStubClazz = Class.forName("android.view.IWindowManager$Stub");
            Method asInterface = windowManagerStubClazz.getDeclaredMethod("asInterface", IBinder.class);

            Class<?> serviceManagerClazz = Class.forName("android.os.ServiceManager");
            Method getService = serviceManagerClazz.getDeclaredMethod("getService", String.class);

            Class<?> windowManagerClazz = Class.forName("android.view.IWindowManager");

            // pre-cache the relevant Method objects using reflection so they're ready to use
            setAnimationScalesMethod = windowManagerClazz.getDeclaredMethod("setAnimationScales", float[].class);
            getAnimationScalesMethod = windowManagerClazz.getDeclaredMethod("getAnimationScales");

            IBinder windowManagerBinder = (IBinder) getService.invoke(null, "window");
            windowManagerObject = asInterface.invoke(null, windowManagerBinder);
        } catch (Exception e) {
            throw new RuntimeException("Failed to access animation methods", e);
        }
    }

    /**
     * Usually should be called inside {@link Instrumentation#onStart()}, before calling super.
     */
    void disableAnimations() {
        try {
            originalScaleFactors = getAnimationScaleFactors();
            setAnimationScaleFactors(DISABLED);
        } catch (Exception e) {
            Log.e(TAG, "Failed to disable animations", e);
        }
    }

    /**
     * Usually should be called inside {@link Instrumentation#onDestroy()}, before calling super.
     */
    void enableAnimations() {
        try {
            restoreAnimationScaleFactors();
        } catch (Exception e) {
            Log.e(TAG, "Failed to enable animations", e);
        }
    }

    private float[] getAnimationScaleFactors() throws InvocationTargetException, IllegalAccessException {
        return (float[]) getAnimationScalesMethod.invoke(windowManagerObject);
    }

    private void setAnimationScaleFactors(float scaleFactor) throws InvocationTargetException, IllegalAccessException {
        float[] scaleFactors = new float[originalScaleFactors.length];
        Arrays.fill(scaleFactors, scaleFactor);
        setAnimationScalesMethod.invoke(windowManagerObject, new Object[]{scaleFactors});
    }

    private void restoreAnimationScaleFactors() throws InvocationTargetException, IllegalAccessException {
        setAnimationScalesMethod.invoke(windowManagerObject, new Object[]{originalScaleFactors});
    }
}
