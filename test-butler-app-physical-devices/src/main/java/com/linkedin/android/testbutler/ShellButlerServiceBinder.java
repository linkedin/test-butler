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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Parcel;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.BundleCompat;

import com.linkedin.android.testbutler.shell.ShellButlerService;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;


/**
 * Manages ShellButlerService from the ButlerService process. Responsible for starting, binding to,
 * and stopping ShellButlerService.
 *
 * To start and 'bind' to a service running as the shell user, we use a number of tricks:
 *
 * 1) ShellButlerServiceBinder directly makes ADB calls.
 * ***To do this, you must run "adb reverse tcp:5038 tcp:5037"***, which lets this app connect
 * to the ADB server on the host.
 *
 * 2) ShellButlerService is started via 'app_process', which is a shell command which lets you
 * start Java processes. We pass in this APK itself as the classpath and use ShellButlerService
 * as the main class.
 *
 * 3) ShellButlerService isn't a real 'service' -- we can't register it in the manifest and bind
 * to it directly, because it needs to be started by the shell user... Luckily, Binders
 * (such as AIDL stubs) can be passed around via Intent. That means instead of ButlerService
 * (or TestButler) binding to ShellButlerService, ShellButlerService sends its ButlerApi stub
 * back to ButlerService. Just like 'bindService', the ButlerApi Stub is still executed in the
 * shell process, and the intent receiver gets a proxy for it. We just need to make sure that
 * ShellButlerService stays alive as long as ButlerService is bound.
 *
 * 4) Most intents come through on the main thread. Because TestButler starts ButlerService
 * first, its 'onBind' call would come through before any ShellButlerService intent containing
 * the ButlerApi to return. However, BroadcastReceivers can be run on a separate handler. Thus
 * ShellButlerService uses sendBroadcast to send the ButlerApi, and ButlerService's onCreate
 * blocks until that ButlerApi is received in a separate handler thread.
 *
 * 5) ShellButlerService is notified of shutdown via the binder. However, we didn't want to
 * change the AIDL that the user sees, so we call 'transact' explicitly with an unused
 * transaction code.
 * */
class ShellButlerServiceBinder {

    private static final String TAG = ShellButlerServiceBinder.class.getSimpleName();

    private static final String ADB_HOST = "localhost";
    private static final int ADB_REVERSE_PORT = 5038;

    private final Context context;

    private HandlerThread thread;
    private ButlerApiBroadcastReceiver receiver;
    private AdbDevice.AdbCommandTask shellProcessTask;

    private volatile ButlerApi butlerApi;

    ShellButlerServiceBinder(@NonNull Context context) {
        this.context = context;
    }

    @Nullable
    ButlerApi bind(long timeout, @NonNull TimeUnit unit) throws InterruptedException {
        AdbDevice adbDevice = AdbDevice.getCurrentDevice(ADB_HOST, ADB_REVERSE_PORT);

        // note: must use separate thread to receive, as we block ButlerService's main thread in
        // onCreate waiting for the butler api broadcast.
        thread = new HandlerThread("ButlerServiceStarted");
        thread.start();

        receiver = new ButlerApiBroadcastReceiver();
        IntentFilter filter = new IntentFilter(ShellButlerService.BROADCAST_BUTLER_API_ACTION);
        context.registerReceiver(receiver, filter, null, new Handler(thread.getLooper()));

        Log.d(TAG, "Registered ShellButlerService receiver, launching ShellButlerService");

        // Execute this apk itself, invoking main() in ShellButlerService
        String apkPath = context.getApplicationInfo().publicSourceDir;
        shellProcessTask = adbDevice.shellCommand("CLASSPATH=" + apkPath, "app_process", "/", ShellButlerService.class.getName());

        Log.d(TAG, "ShellButlerService launched, waiting for ButlerApi broadcast");

        if (!receiver.received.await(timeout, unit)) {
            Log.e(TAG, "Timed out waiting for ShellButlerService");
        } else {
            Log.d(TAG, "Received ButlerApi from ShellButlerService");
        }

        return butlerApi;
    }

    void unbind() {
        try {
            if (butlerApi == null) {
                if (shellProcessTask != null) {
                    // This is a rare case -- we started the shell process but never received the
                    // ButlerApi we use to stop it. Have to close the socket to kill the process.
                    shellProcessTask.closeSocket();
                }
            } else {
                Parcel data = Parcel.obtain();
                try {
                    butlerApi.asBinder().transact(ShellButlerService.KILL_CODE, data, null, 0);
                } finally {
                    data.recycle();
                }
                shellProcessTask.get();
            }
        } catch (InterruptedException e) {
            Log.e(TAG, "Interrupted while shutting down ShellButlerService, future tests may fail!", e);
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            Log.e(TAG, "Failed to shut down ShellButlerService cleanly, future tests may fail!", e);
        }

        context.unregisterReceiver(receiver);
        receiver = null;
        thread.quit();
        thread = null;
        butlerApi = null;
    }

    private class ButlerApiBroadcastReceiver extends BroadcastReceiver {
        private final CountDownLatch received = new CountDownLatch(1);

        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG, "ButlerApiBroadcastReceiver#onReceive was called");
            Bundle bundle = intent.getBundleExtra(ShellButlerService.BUTLER_API_BUNDLE_KEY);
            if (bundle != null) {
                IBinder binder = BundleCompat.getBinder(bundle, ShellButlerService.BUTLER_API_BUNDLE_KEY);
                butlerApi = ButlerApi.Stub.asInterface(binder);
                received.countDown();
            }
        }
    }
}
