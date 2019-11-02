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

import android.accessibilityservice.AccessibilityServiceInfo;
import android.app.Service;
import android.content.Intent;
import android.net.wifi.WifiManager;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;
import android.view.accessibility.AccessibilityManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Collections;
import java.util.List;

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

    private GsmDataDisabler gsmDataDisabler;
    private PermissionGranter permissionGranter;
    private CommonDeviceLocks locks;
    private AccessibilityServiceEnabler accessibilityServiceEnabler;

    private ButlerApiStubBase butlerApi = new ButlerApiStubBase() {
        @Override
        public boolean setWifiState(boolean enabled) throws RemoteException {
            WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
            return wifiManager.setWifiEnabled(enabled);
        }

        @Override
        public boolean setGsmState(boolean enabled) throws RemoteException {
            return gsmDataDisabler.setGsmState(ButlerService.this, enabled);
        }

        @Override
        public boolean grantPermission(String packageName, String permission) throws RemoteException {
            return permissionGranter.grantPermission(ButlerService.this, packageName, permission);
        }

        @Override
        public boolean setAccessibilityServiceState(boolean enabled) throws RemoteException {
            return accessibilityServiceEnabler.setAccessibilityServiceEnabled(enabled);
        }

        @Override
        public void onDestroy() {
            super.onDestroy();
            // Turn the accessibility service off it we enabled it
            try {
                accessibilityServiceEnabler.setAccessibilityServiceEnabled(false);
            } catch (RemoteException ignored) { }
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();

        Log.d(TAG, "ButlerService starting up...");

        AppSettingsAccessor settings = new AppSettingsAccessor(getContentResolver());

        gsmDataDisabler = new GsmDataDisabler();
        permissionGranter = new PermissionGranter();
        InstalledAccessibilityServiceProvider serviceProvider = new InstalledAccessibilityServiceProvider() {
            @NonNull
            @Override
            public List<AccessibilityServiceInfo> getInstalledAccessibilityServiceList() {
                AccessibilityManager manager = (AccessibilityManager) getApplicationContext()
                        .getSystemService(ACCESSIBILITY_SERVICE);
                if (manager == null) {
                    return Collections.emptyList();
                }
                return manager.getInstalledAccessibilityServiceList();
            }
        };
        accessibilityServiceEnabler = new AccessibilityServiceEnabler(serviceProvider, settings);
        locks = new CommonDeviceLocks();
        locks.acquire(this);

        butlerApi.onCreate(settings);

        // Install custom IActivityController to prevent system dialogs from appearing if apps crash or ANR
        NoDialogActivityController.install();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        Log.d(TAG, "ButlerService shutting down...");

        // Uninstall our IActivityController to resume normal Activity behavior
        NoDialogActivityController.uninstall();

        butlerApi.onDestroy();
        locks.release();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return butlerApi;
    }
}
