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

import android.app.KeyguardManager;
import android.content.Context;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.PowerManager;

import androidx.annotation.NonNull;

import static android.content.Context.KEYGUARD_SERVICE;
import static android.content.Context.POWER_SERVICE;
import static android.content.Context.WIFI_SERVICE;


/**
 * Handles acquiring and releasing all device locks used by TestButler.
 */
class CommonDeviceLocks {

    private WifiManager.WifiLock wifiLock;
    private PowerManager.WakeLock wakeLock;
    private KeyguardManager.KeyguardLock keyguardLock;

    /**
     * Create locks for the given application context.
     * @param context The application context
     */
    void acquire(@NonNull Context context) {
        // Acquire a WifiLock to prevent wifi from turning off and breaking tests
        // NOTE: holding a WifiLock does NOT override a call to setWifiEnabled(false)
        WifiManager wifiManager = (WifiManager) context.getSystemService(WIFI_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            wifiLock = wifiManager.createWifiLock(WifiManager.WIFI_MODE_FULL_HIGH_PERF, "ButlerWifiLock");
        } else {
            wifiLock = wifiManager.createWifiLock(WifiManager.WIFI_MODE_FULL, "ButlerWifiLock");
        }
        wifiLock.acquire();

        // Acquire a keyguard lock to prevent the lock screen from randomly appearing and breaking tests
        // KeyguardManager has been restricted in Q, so we don't use to avoid breaking all test runs on Q emulators
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
            KeyguardManager keyguardManager = (KeyguardManager) context.getSystemService(KEYGUARD_SERVICE);
            keyguardLock = keyguardManager.newKeyguardLock("ButlerKeyguardLock");
            keyguardLock.disableKeyguard();
        }

        // Acquire a wake lock to prevent the cpu from going to sleep and breaking tests
        PowerManager powerManager = (PowerManager) context.getSystemService(POWER_SERVICE);
        wakeLock = powerManager.newWakeLock(PowerManager.FULL_WAKE_LOCK
                | PowerManager.ACQUIRE_CAUSES_WAKEUP
                | PowerManager.ON_AFTER_RELEASE, "TestButlerApp:WakeLock");
        wakeLock.acquire();
    }

    /**
     * Release the previously-acquired locks.
     */
    void release() {
        // Release all the locks we were holding
        wifiLock.release();
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
            keyguardLock.reenableKeyguard();
        }
        wakeLock.release();
    }
}
