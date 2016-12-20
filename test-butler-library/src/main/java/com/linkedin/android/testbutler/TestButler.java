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

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.ActivityManager;
import android.app.Instrumentation;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.provider.Settings;
import android.support.annotation.IntDef;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.Surface;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Locale;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Main entry point for test code to interface with the Test Butler app running on the emulator.
 * <p>
 * Contains a number of helper methods for safely communicating with the Test Butler app via RPC.
 */
@SuppressLint("InlinedApi")
public class TestButler {

    @IntDef({
            Settings.Secure.LOCATION_MODE_OFF,
            Settings.Secure.LOCATION_MODE_BATTERY_SAVING,
            Settings.Secure.LOCATION_MODE_SENSORS_ONLY,
            Settings.Secure.LOCATION_MODE_HIGH_ACCURACY
    })
    @Retention(RetentionPolicy.SOURCE)
    public @interface LocationMode {}

    /**
     * AOSP has an IntDef for this, but it's hidden, so we create our own here
     */
    @IntDef({
            Surface.ROTATION_0,
            Surface.ROTATION_90,
            Surface.ROTATION_180,
            Surface.ROTATION_270
    })
    @Retention(RetentionPolicy.SOURCE)
    public @interface Rotation {}

    private static final String TAG = TestButler.class.getSimpleName();

    private static CountDownLatch serviceStarted = new CountDownLatch(1);

    private static final ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            butlerApi = ButlerApi.Stub.asInterface(service);
            serviceStarted.countDown();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            butlerApi = null;
        }
    };

    private static ButlerApi butlerApi;

    /**
     * Start the remote ButlerService to prepare for running tests
     * <p>
     * This method should be called from a subclass of {@link Instrumentation#onStart()}, BEFORE
     * calling super.onStart(). Often this will be a subclass of AndroidJUnitRunner.
     * <p>
     * This will handle disabling animations on the device, as well as disabling system popups when
     * apps crash or ANR on the emulator. NOTE: Calling this method will cause the system method
     * {@link ActivityManager#isUserAMonkey()} to start returning true. If your code uses this method,
     * you will need to create a helper method for detecting when your code is running under instrumentation
     * testing and not under monkey testing.
     *
     * @param context the "target context"; i.e. Context of the app under test (not the test apk context!)
     */
    public static void setup(@NonNull Context context) {
        Intent intent = new Intent();
        intent.setComponent(new ComponentName("com.linkedin.android.testbutler",
                "com.linkedin.android.testbutler.ButlerService"));

        context.startService(intent);
        context.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
        try {
            if (!serviceStarted.await(5, TimeUnit.SECONDS)) {
                Log.e(TAG, "Failed to start TestButler; Did you remember to install it before running your tests?\n" +
                        "Running tests without ButlerService, failures or unexpected behavior may occur!!!");
            }
        } catch (InterruptedException e) {
            throw new IllegalStateException("Interrupted while trying to start ButlerService", e);
        }
    }

    /**
     * Stop the remote ButlerService to indicate the test run has completed.
     * <p>
     * This method should be called from a subclass of {@link Instrumentation#finish(int, Bundle)}, BEFORE
     * calling super.finish(). Often this will be a subclass of AndroidJUnitRunner.
     * <p>
     * This will handle re-enabling animations on the device, as well as allow system popups to be shown when
     * apps crash or ANR on the emulator.
     *
     * @param context the "target context"; i.e. Context of the app under test (not the test apk context!)
     */
    public static void teardown(@NonNull Context context) {
        Intent intent = new Intent();
        intent.setComponent(new ComponentName("com.linkedin.android.testbutler",
                "com.linkedin.android.testbutler.ButlerService"));

        context.unbindService(serviceConnection);
        context.stopService(intent);
        serviceStarted = new CountDownLatch(1);
    }

    /**
     * Validate that all animations have been disabled during the {@link #setup(Context)} call.
     * <p>
     * This method will fail with an {@link junit.framework.AssertionFailedError} if animations were not disabled
     * by the {@link #setup(Context)} method. This is useful if you want to immediately halt test execution if
     * animations could not be disabled by Test Butler, rather than failing gracefully and continuing
     * to try and run your tests.
     *
     * @param context the "target context"; i.e. Context of the app under test (not the test apk context!)
     */
    public static void verifyAnimationsDisabled(@NonNull Context context) {
        AnimationAssertions.verifyAnimationsDisabled(context);
    }

    /**
     * Enable/disable the Wifi connection on the emulator
     *
     * @param enabled true if wifi should be enabled, false otherwise
     */
    public static void setWifiState(boolean enabled) {
        verifyApiReady();
        try {
            if (!butlerApi.setWifiState(enabled)) {
                throw new IllegalStateException("Failed to set wifi state!");
            }
        } catch (RemoteException e) {
            throw new IllegalStateException("Failed to communicate with ButlerService", e);
        }
    }

    /**
     * Enable/disable the GSM connection on the emulator
     *
     * @param enabled true if GSM should be enabled, false otherwise
     */
    @TargetApi(Build.VERSION_CODES.KITKAT)
    public static void setGsmState(boolean enabled) {
        verifyApiReady();
        try {
            if (!butlerApi.setGsmState(enabled)) {
                throw new IllegalStateException("Failed to set gsm state!");
            }
        } catch (RemoteException e) {
            throw new IllegalStateException("Failed to communicate with ButlerService", e);
        }
    }

    /**
     * Change the location services mode on the emulator
     *
     * @param locationMode one of the {@link LocationMode} IntDef values
     */
    public static void setLocationMode(@LocationMode int locationMode) {
        verifyApiReady();
        try {
            if (!butlerApi.setLocationMode(locationMode)) {
                throw new IllegalStateException("Failed to set location mode!");
            }
        } catch (RemoteException e) {
            throw new IllegalStateException("Failed to communicate with ButlerService", e);
        }
    }

    /**
     * Set the locale for the application under test. Requires API 17+.
     * <p>
     * NOTE: this does not change the device locale! Changing the device locale on the emulator
     * has been slow/unreliable and led to flaky tests when the locale setting did not take effect in time.
     * Instead, this method uses the {@link Configuration} class to update the locale on the {@link Resources}
     * object of the {@link Context} from the application under test. As long as apps are checking the
     * current locale from the current {@link Configuration}, this approach will be reliable.
     *
     * @param language the language code for the new locale, as expected by {@link Locale#Locale(String, String)}
     * @param country  the country code for the new locale, as expected by {@link Locale#Locale(String, String)}
     * @param context  the "target context"; i.e. Context of the app under test (not the test apk context!)
     */
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
    public static void setLocale(@NonNull String language, @NonNull String country, @NonNull Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR1) {
            throw new IllegalStateException("Cannot change locale before API 17");
        }

        context = context.getApplicationContext();
        Resources resources = context.getResources();
        Configuration configuration = resources.getConfiguration();
        configuration.setLocale(new Locale(language, country));
        resources.updateConfiguration(configuration, resources.getDisplayMetrics());
    }

    /**
     * Change the screen rotation of the emulator
     *
     * @param rotation one of the {@link Rotation} IntDef values
     */
    public static void setRotation(@Rotation int rotation) {
        verifyApiReady();
        try {
            if (!butlerApi.setRotation(rotation)) {
                throw new IllegalStateException("Failed to set rotation!");
            }
        } catch (RemoteException e) {
            throw new IllegalStateException("Failed to communicate with ButlerService", e);
        }
    }

    /**
     * Enable or disable the system spell checker
     *
     * @param enabled What state the spell checker should be set to
     */
    public static void setSpellCheckerState(boolean enabled) {
        try {
            if (!butlerApi.setSpellCheckerState(enabled)) {
                throw new IllegalStateException("Failed to set spell checker!");
            }
        } catch (RemoteException e) {
            throw new IllegalStateException("Failed to communicate with ButlerService", e);
        }
    }

    /**
     * Enable or disable the system software keyboard
     *
     * @param enabled What state the software keyboard should be set to
     */
    public static void setSoftKeyboardState(boolean enabled) {
        try {
            if (!butlerApi.setShowImeWithHardKeyboard(enabled)) {
                throw new IllegalStateException("Failed to set software keyboard!");
            }
        } catch (RemoteException e) {
            throw new IllegalStateException("Failed to communicate with ButlerService", e);
        }
    }

    /**
     * A helper method for granting runtime permissions to apps under test on API 23+
     * <p>
     * Note: Before API 23, this method is a no-op
     * <p>
     * A common use case for this is when running tests from Android Studio where you can't (currently) pass the
     * -g flag to adb install when installing the app to run tests.
     *
     * @param context the "target context"; i.e. Context of the app under test (not the test apk context!)
     * @see <a href="https://code.google.com/p/android/issues/detail?id=198813">
     * https://code.google.com/p/android/issues/detail?id=198813</a>
     */
    public static void grantPermission(@NonNull Context context, @NonNull String permission) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            Log.i(TAG, "No need to grantPermission before API 23");
            return;
        }
        verifyApiReady();
        try {
            if (!butlerApi.grantPermission(context.getPackageName(), permission)) {
                throw new IllegalArgumentException("Failed to grant permission " + permission);
            }
        } catch (RemoteException e) {
            throw new IllegalStateException("Failed to communicate with ButlerService", e);
        }
    }

    private static void verifyApiReady() {
        if (butlerApi == null) {
            throw new IllegalStateException("ButlerService is not started!");
        }
    }
}
