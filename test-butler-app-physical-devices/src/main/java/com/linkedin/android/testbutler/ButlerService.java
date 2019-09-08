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
import android.app.Service;
import android.content.Intent;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

import androidx.annotation.Nullable;

import java.util.concurrent.TimeUnit;


/**
 * Main entry point into the Test Butler application.
 * <p>
 * Runs while instrumentation tests are running and handles modifying several system settings in order
 * to make test runs more reliable, as well as enable tests to modify system settings on the fly
 * to test application behavior under a given configuration.
 */
@SuppressWarnings("deprecation")
public class ButlerService extends Service {

    /* When running on physical devices, ButlerService doesn't have a lot of privileges that it
     * would have on emulators, which it needs to do most operations. However, the ADB shell user
     * does :)
     *
     * The 'privileged' ButlerService implementation is in ShellButlerService. We start that service
     * via ADB shell so that we have all permissions needed. ButlerService (this class) is an
     * intermediate which starts the ShellButlerService, wraps its binder, and handles
     * non-privileged calls such as device locks and wifi state.
     *
     * See ShellButlerServiceBinder for details in how this works. In summary:
     *
     * 1) Find this device via ADB
     * 2) Start a BroadcastReceiver
     * 3) Start ShellButlerService via adb shell app_process
     * 4) Wait for ShellButlerService broadcast containing ButlerApi Binder
     * 5) Wrap that ButlerApi in onBind()
     * 6) Send kill command to ShellButlerService in onDestroy()
     */

    private static final String TAG = ButlerService.class.getSimpleName();

    private ShellButlerServiceBinder shellBinder;
    private ButlerApi butlerApi;
    private CommonDeviceLocks locks;
    private KeyguardManager.KeyguardLock keyguardLock;

    @Override
    public void onCreate() {
        super.onCreate();

        Log.d(TAG, "ButlerService starting up...");

        try {
            shellBinder = new ShellButlerServiceBinder(this);
            butlerApi = shellBinder.bind(5, TimeUnit.SECONDS);

            locks = new CommonDeviceLocks();
            locks.acquire(this);

            // CommonDeviceLocks doesn't enable the Keyguard Lock on Q due to compatibility issues.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                KeyguardManager keyguardManager = (KeyguardManager) getSystemService(KEYGUARD_SERVICE);
                keyguardLock = keyguardManager.newKeyguardLock("ButlerKeyguardLock");
                keyguardLock.disableKeyguard();
            }

            Log.d(TAG, "ButlerService startup completed...");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        Log.d(TAG, "ButlerService shutting down...");

        shellBinder.unbind();
        locks.release();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            keyguardLock.reenableKeyguard();
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            return butlerApi.asBinder();
        }

        // Before 8.1, shell user doesn't have CHANGE_WIFI_STATE privileges, so we have to do it
        // here instead of in ShellButlerService.
        // Note that after Android 10, you cannot call setWifiEnabled from an app, so it *must* be
        // done in ShellButlerService (covered by check above).
        return new ButlerApi.Stub() {
            @Override
            public boolean setWifiState(boolean enabled) throws RemoteException {
                WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
                return wifiManager.setWifiEnabled(enabled);
            }

            @Override
            public boolean setLocationMode(int locationMode) throws RemoteException {
                return butlerApi.setLocationMode(locationMode);
            }

            @Override
            public boolean setRotation(int rotation) throws RemoteException {
                return butlerApi.setRotation(rotation);
            }

            @Override
            public boolean setGsmState(boolean enabled) throws RemoteException {
                return butlerApi.setGsmState(enabled);
            }

            @Override
            public boolean grantPermission(String packageName, String permission) throws RemoteException {
                return butlerApi.grantPermission(packageName, permission);
            }

            @Override
            public boolean setSpellCheckerState(boolean enabled) throws RemoteException {
                return butlerApi.setSpellCheckerState(enabled);
            }

            @Override
            public boolean setShowImeWithHardKeyboardState(boolean enabled) throws RemoteException {
                return butlerApi.setShowImeWithHardKeyboardState(enabled);
            }

            @Override
            public boolean setImmersiveModeConfirmation(boolean enabled) throws RemoteException {
                return butlerApi.setImmersiveModeConfirmation(enabled);
            }

            @Override
            public boolean setAlwaysFinishActivitiesState(boolean enabled) throws RemoteException {
                return butlerApi.setAlwaysFinishActivitiesState(enabled);
            }
        };
    }
}
