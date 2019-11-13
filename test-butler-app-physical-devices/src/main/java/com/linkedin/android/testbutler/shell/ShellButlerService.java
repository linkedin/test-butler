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

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcel;
import android.os.RemoteException;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.BundleCompat;

import com.linkedin.android.testbutler.AccessibilityServiceEnabler;
import com.linkedin.android.testbutler.ButlerApi;
import com.linkedin.android.testbutler.ButlerApiStubBase;
import com.linkedin.android.testbutler.NoDialogActivityController;

import java.io.Closeable;
import java.util.concurrent.CountDownLatch;


/**
 * Contains the ButlerApi implementation that is run as the shell user to gain access to privileged
 * APIs. Acts like a typical Service in that it contains an AIDL stub implementation which is called
 * via Binder, but it cannot be bound or started via service APIs. Instead, once this class is run
 * from the shell, it sends a Broadcast containing the ButlerApi binder back to the real
 * ButlerService.
 */
public class ShellButlerService implements Closeable {

    private static final String TAG = ShellButlerService.class.getSimpleName();

    public static final String BROADCAST_BUTLER_API_ACTION = "com.linkedin.android.testbutler.BROADCAST_BUTLER_API";
    public static final String BUTLER_API_BUNDLE_KEY = "ButlerApi";
    public static final int KILL_CODE = ButlerApi.Stub.LAST_CALL_TRANSACTION;

    static final String SHELL_PACKAGE = "com.android.shell";

    private final CountDownLatch stop = new CountDownLatch(1);
    private final ShellSettingsAccessor settings;

    private GsmDataDisabler gsmDataDisabler;
    private PermissionGranter permissionGranter;
    private WifiManagerWrapper wifiManager;
    private AccessibilityServiceEnabler accessibilityServiceEnabler;

    private final ButlerApiStubBase butlerApi = new ButlerApiStubBase() {
        @Override
        public boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
            if (code == KILL_CODE) {
                stop.countDown();
                return true;
            }
            return super.onTransact(code, data, reply, flags);
        }

        @Override
        public boolean setWifiState(boolean enabled) throws RemoteException {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O_MR1) {
                Log.e(TAG, "setWifiState should not be called on ShellButlerService before 8.1");
                return false;
            }
            return wifiManager.setWifiEnabled(enabled);
        }

        @Override
        public boolean setGsmState(boolean enabled) throws RemoteException {
            return gsmDataDisabler.setGsmState(enabled);
        }

        @Override
        public boolean grantPermission(String packageName, String permission) throws RemoteException {
            return permissionGranter.grantPermission(packageName, permission);
        }

        @Override
        public boolean setAccessibilityServiceState(boolean enabled) throws RemoteException {
            return accessibilityServiceEnabler.setAccessibilityServiceEnabled(enabled);
        }
    };

    private ShellButlerService(@NonNull ShellSettingsAccessor settings) {
        this.settings = settings;
    }

    private void onCreate() {
        Log.d(TAG, "ShellButlerService starting up...");

        ServiceManagerWrapper serviceManager = ServiceManagerWrapper.newInstance();

        gsmDataDisabler = new GsmDataDisabler(serviceManager);
        permissionGranter = new PermissionGranter(serviceManager);
        AccessibilityManagerWrapper accessibilityWrapper = new AccessibilityManagerWrapper(serviceManager);
        accessibilityServiceEnabler = new AccessibilityServiceEnabler(accessibilityWrapper, settings);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            wifiManager = WifiManagerWrapper.getInstance(serviceManager);
        }

        butlerApi.onCreate(settings);

        // Install custom IActivityController to prevent system dialogs from appearing if apps crash or ANR
        NoDialogActivityController.install();
    }

    private void onDestroy() {
        Log.d(TAG, "ShellButlerService shutting down...");

        butlerApi.onDestroy();

        // Uninstall our IActivityController to resume normal Activity behavior
        NoDialogActivityController.uninstall();

        // Turn the accessibility service off it we enabled it
        try {
            accessibilityServiceEnabler.setAccessibilityServiceEnabled(false);
        } catch (RemoteException ignored) { }

        Log.d(TAG, "ShellButlerService shut down completed");
    }

    @Override
    public void close() {
        onDestroy();
    }

    private void broadcastButlerApi() throws Exception {
        Intent intent = new Intent(BROADCAST_BUTLER_API_ACTION);

        Bundle bundle = new Bundle();
        BundleCompat.putBinder(bundle, BUTLER_API_BUNDLE_KEY, butlerApi);
        intent.putExtra(BUTLER_API_BUNDLE_KEY, bundle);

        ActivityManagerWrapper.newInstance().broadcastIntent(intent);
    }

    public static void main(String[] args) {
        try (ShellSettingsAccessor settings = ShellSettingsAccessor.newInstance();
             ShellButlerService shellButlerService = new ShellButlerService(settings)) {
            shellButlerService.onCreate();
            shellButlerService.broadcastButlerApi();
            Log.d(TAG, "ButlerApi sent, waiting for stop");
            shellButlerService.stop.await();
        } catch (Exception e) {
            Log.e(TAG, "Exception in ShellButlerService, exiting...", e);
        }
    }
}
