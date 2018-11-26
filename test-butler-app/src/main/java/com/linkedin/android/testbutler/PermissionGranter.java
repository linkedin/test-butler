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

import android.annotation.TargetApi;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.UserHandle;
import androidx.annotation.NonNull;
import android.util.Log;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * A helper class for granting runtime permissions to apps under test on API 23+
 * <p>
 * A common use case for this is when running tests from Android Studio where you can't (currently) pass the -g flag
 * to adb install when installing the app to run tests.
 *
 * @see <a href="https://code.google.com/p/android/issues/detail?id=198813">
 *     https://code.google.com/p/android/issues/detail?id=198813</a>
 */
final class PermissionGranter {

    private static final String TAG = PermissionGranter.class.getSimpleName();

    @TargetApi(Build.VERSION_CODES.M)
    boolean grantPermission(@NonNull Context context, @NonNull String packageName, @NonNull String permission) {
        try {
            PackageManager packageManager = context.getPackageManager();
            Method method = packageManager.getClass()
                    .getMethod("grantRuntimePermission", String.class, String.class, UserHandle.class);

            method.invoke(packageManager, packageName, permission, android.os.Process.myUserHandle());
        } catch (NoSuchMethodException e) {
            Log.e(TAG, "NoSuchMethodException while granting permission", e);
            return false;
        } catch (InvocationTargetException e) {
            Log.e(TAG, "InvocationTargetException while granting permission", e);
            return false;
        } catch (IllegalAccessException e) {
            Log.e(TAG, "IllegalAccessException while granting permission", e);
            return false;
        }

        return true;
    }
}
