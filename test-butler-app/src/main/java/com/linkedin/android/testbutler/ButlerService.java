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

import android.app.KeyguardManager;
import android.app.Service;
import android.content.Intent;
import android.net.wifi.WifiManager;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.RemoteException;
import android.support.annotation.Nullable;
import android.util.Log;

/**
 * Main entry point into the Test Butler application.
 * <p>
 * Runs while instrumentation tests are running and handles modifying several system settings in order
 * to make test runs more reliable, as well as enable tests to modify system settings on the fly
 * to test application behavior under a given configuration.
 */
@SuppressWarnings("deprecation")
public class ButlerService extends Service {

    private static final String TAG = ButlerService.class.getSimpleName();

    private AnimationDisabler animationDisabler;
    private RotationChanger rotationChanger;
    private LocationServicesChanger locationServicesChanger;
    private GsmDataDisabler gsmDataDisabler;
    private PermissionGranter permissionGranter;
    private SpellCheckerDisabler spellCheckerDisabler;

    private WifiManager.WifiLock wifiLock;
    private PowerManager.WakeLock wakeLock;
    private KeyguardManager.KeyguardLock keyguardLock;

    private final ButlerApi.Stub butlerApi = new ButlerApi.Stub() {
        @Override
        public boolean setWifiState(boolean enabled) throws RemoteException {
            WifiManager wifiManager = (WifiManager) getSystemService(WIFI_SERVICE);
            return wifiManager.setWifiEnabled(enabled);
        }

        @Override
        public boolean setGsmState(boolean enabled) throws RemoteException {
            return gsmDataDisabler.setGsmState(ButlerService.this, enabled);
        }

        @Override
        public boolean setLocationMode(int locationMode) throws RemoteException {
            return locationServicesChanger.setLocationServicesState(getContentResolver(), locationMode);
        }

        @Override
        public boolean setRotation(int rotation) throws RemoteException {
            return rotationChanger.setRotation(getContentResolver(), rotation);
        }

        @Override
        public boolean grantPermission(String packageName, String permission) throws RemoteException {
            return permissionGranter.grantPermission(ButlerService.this, packageName, permission);
        }

        @Override
        public boolean setSpellCheckerState(boolean enabled) {
            return spellCheckerDisabler.setSpellChecker(getContentResolver(), enabled);
        }
    };

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

        gsmDataDisabler = new GsmDataDisabler();
        permissionGranter = new PermissionGranter();

        spellCheckerDisabler = new SpellCheckerDisabler();
        spellCheckerDisabler.saveSpellCheckerState(getContentResolver());
        // Disable spell checker by default
        spellCheckerDisabler.setSpellChecker(getContentResolver(), false);

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
        animationDisabler.enableAnimations();

        // Reset location services state to whatever it originally was
        locationServicesChanger.restoreLocationServicesState(getContentResolver());

        // Reset rotation from the accelerometer to whatever it originally was
        rotationChanger.restoreRotationState(getContentResolver());

        // Uninstall our IActivityController to resume normal Activity behavior
        NoDialogActivityController.uninstall();

        // Reset the spell checker to the original state
        spellCheckerDisabler.restoreSpellCheckerState(getContentResolver());
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return butlerApi;
    }
}
