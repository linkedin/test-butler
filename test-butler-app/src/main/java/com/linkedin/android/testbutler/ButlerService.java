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

import android.app.IntentService;
import android.app.KeyguardManager;
import android.content.Intent;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.RemoteException;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import static android.provider.Settings.Secure.LOCATION_MODE;

/**
 * Main entry point into the Test Butler application.
 * <p>
 * Runs while instrumentation tests are running and handles modifying several system settings in order
 * to make test runs more reliable, as well as enable tests to modify system settings on the fly
 * to test application behavior under a given configuration.
 */
@SuppressWarnings("deprecation")
public class ButlerService extends IntentService {

    private static final String TAG = ButlerService.class.getSimpleName();

	/**
     * A boolean extra indicating
     */
    public static final String DISABLE_ANIMATIONS = "disable_animations";

    private AnimationDisabler animationDisabler;
    private RotationChanger rotationChanger;
    private LocationServicesChanger locationServicesChanger;

    private WifiManager.WifiLock wifiLock;
    private PowerManager.WakeLock wakeLock;
    private KeyguardManager.KeyguardLock keyguardLock;

    private boolean restoreLocationMode = true;
    private boolean restoreAnimations = true;

    private final ButlerApi.Stub butlerApi = new ButlerApi.Stub() {
        @Override
        public boolean setWifiState(boolean enabled) throws RemoteException {
            WifiManager wifiManager = (WifiManager) getSystemService(WIFI_SERVICE);
            return wifiManager.setWifiEnabled(enabled);
        }

        @Override
        public boolean setLocationMode(int locationMode) throws RemoteException {
            return locationServicesChanger.setLocationServicesState(getContentResolver(), locationMode);
        }

        @Override
        public boolean setRotation(int rotation) throws RemoteException {
            return rotationChanger.setRotation(getContentResolver(), rotation);
        }
    };

    public ButlerService() {
        super(TAG);
    }

    @Override
    public void onCreate() {
        super.onCreate();

        Log.d(TAG, "ButlerService starting up...");

        // Save current device rotation so we can restore it after tests complete
        rotationChanger = new RotationChanger();
        rotationChanger.saveRotationState(getContentResolver());

        // Save current location services setting so we can restore it after tests complete
        locationServicesChanger = new LocationServicesChanger();
        locationServicesChanger.saveLocationServicesState(getContentResolver());

        // Disable animations on the device so tests can run reliably
        animationDisabler = new AnimationDisabler();
        animationDisabler.disableAnimations();

        // Acquire a WifiLock to prevent wifi from turning off and breaking tests
        // NOTE: holding a WifiLock does NOT override a call to setWifiEnabled(false)
        WifiManager wifiManager = (WifiManager) getSystemService(WIFI_SERVICE);
        wifiLock = wifiManager.createWifiLock(WifiManager.WIFI_MODE_FULL, "ButlerWifiLock");
        wifiLock.acquire();

        // Acquire a keyguard lock to prevent the lock screen from randomly appearing and breaking tests
        KeyguardManager keyguardManager = (KeyguardManager) getSystemService(KEYGUARD_SERVICE);
        keyguardLock = keyguardManager.newKeyguardLock("ButlerKeyguardLock");
        keyguardLock.disableKeyguard();

        // Acquire a wake lock to prevent the cpu from going to sleep and breaking tests
        PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        wakeLock = powerManager.newWakeLock(PowerManager.FULL_WAKE_LOCK
                | PowerManager.ACQUIRE_CAUSES_WAKEUP
                | PowerManager.ON_AFTER_RELEASE, "ButlerWakeLock");
        wakeLock.acquire();

        // Install custom IActivityController to prevent system dialogs from appearing if apps crash or ANR
        NoDialogActivityController.install();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        Log.d(TAG, "ButlerService shutting down...");

        // Release all the locks we were holding
        wakeLock.release();
        keyguardLock.reenableKeyguard();
        wifiLock.release();

        // Re-enable animations on the emulator
        if (restoreAnimations)
            animationDisabler.enableAnimations();

        // Reset location services state to whatever it originally was
        if (restoreLocationMode)
            locationServicesChanger.restoreLocationServicesState(getContentResolver());

        // Reset rotation from the accelerometer to whatever it originally was
        rotationChanger.restoreRotationState(getContentResolver());

        // Uninstall our IActivityController to resume normal Activity behavior
        NoDialogActivityController.uninstall();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return butlerApi;
    }

    @Override
    protected void onHandleIntent(@NonNull Intent intent) {
        final Bundle extras = intent.getExtras();
        if (extras != null) {
            for (String key : extras.keySet()) {
                switch (key) {
                    case LOCATION_MODE:
                        try {
                            butlerApi.setLocationMode(extras.getInt(key, -1));
                        } catch (RemoteException e) {
                            e.printStackTrace();
                        }
                        restoreLocationMode = false;
                        break;
                    case DISABLE_ANIMATIONS:
                        if (extras.getBoolean(key, false))
                            animationDisabler.disableAnimations();
                        else
                            animationDisabler.enableAnimations();
                        restoreAnimations = false;
                        break;
                    default:
                }
            }
        }
    }
}
