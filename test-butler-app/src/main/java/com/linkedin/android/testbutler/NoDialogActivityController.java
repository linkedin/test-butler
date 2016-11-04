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

import android.app.IActivityController;
import android.content.Intent;
import android.os.RemoteException;
import android.support.annotation.Nullable;
import android.util.Log;

import java.lang.reflect.Method;

/**
 * {@link NoDialogActivityController} disables the system dialogs for app crashes and ANRs so that other apps crashing
 * on an emulator will not prevent Espresso UI tests from running.
 * <p>
 * {@link IActivityController} is an interface provided by the Android OS to facilitate testing.
 * If set, the class is called by the Android system and can prevent activities from starting
 * or resuming, as well as disable the default system dialogs that appear when an app crashes or
 * ANRs. An example of a class implementing this interface can be found in the AOSP class Monkey.java:
 * https://github.com/android/platform_development/blob/master/cmds/monkey/src/com/android/commands/monkey/Monkey.java
 * <p>
 * This class is passed to the ActivityManagerNative, which requires the SET_ACTIVITY_WATCHER permission. This
 * permission is only granted to apps with the "signature" permission level, which requires the app to be signed
 * with the system signing key. The signing key for the stock emulators is openly available, so we can sign this
 * app with that key, but that means it can only be installed on stock emulators.
 * <p>
 * Note: In order to implement the {@link IActivityController} interface, a copy of the .aidl file is included
 * in this project.
 */
class NoDialogActivityController extends IActivityController.Stub {
    private static final String TAG = NoDialogActivityController.class.getSimpleName();

    @Override
    public boolean activityStarting(Intent intent, String pkg) throws RemoteException {
        Log.v(TAG, "activityStarting: " + pkg + " " + intent.toString());
        // allow all activities to start
        return true;
    }

    @Override
    public boolean activityResuming(String pkg) throws RemoteException {
        Log.v(TAG, "activityResuming: " + pkg);
        // allow all activities to resume
        return true;
    }

    @Override
    public boolean appCrashed(String processName, int pid, String shortMsg, String longMsg,
                              long timeMillis, String stackTrace) throws RemoteException {
        Log.v(TAG, "appCrashed: " + processName + ":" + pid + " " + shortMsg + " " + longMsg + " " + stackTrace);
        // return false to prevent the system dialog from appearing
        return false;
    }

    @Override
    public int appNotResponding(String processName, int pid, String processStats) throws RemoteException {
        Log.v(TAG, "appNotResponding: " + processName + ":" + pid + " " + processStats);
        // return -1 to kill the ANR-ing app immediately and prevent the system dialog from appearing
        return -1;
    }

    @Override
    public int systemNotResponding(String msg) throws RemoteException {
        Log.v(TAG, "systemNotResponding: " + msg);
        // return -1 to let the system continue with its normal kill
        return -1;
    }

    /**
     * Install an instance of this class as the {@link IActivityController} to monitor the ActivityManager
     */
    static void install() {
        setActivityController(new NoDialogActivityController());
    }

    /**
     * Remove any installed {@link IActivityController} to reset the ActivityManager to the default state
     */
    static void uninstall() {
        setActivityController(null);
    }

    /**
     * Use reflection to call the hidden api and set a custom {@link IActivityController}:
     * ActivityManagerNative.getDefault().setActivityController(activityController);
     */
    private static void setActivityController(@Nullable IActivityController activityController) {
        try {
            Class<?> amClass = Class.forName("android.app.ActivityManagerNative");
            Method getDefault = amClass.getMethod("getDefault");
            Object am = getDefault.invoke(null);
            Method setMethod = am.getClass().getMethod("setActivityController", IActivityController.class);
            setMethod.invoke(am, activityController);
        } catch (Throwable e) {
            Log.e(TAG, "Failed to install custom IActivityController: " + e.getMessage(), e);
        }
    }
}
