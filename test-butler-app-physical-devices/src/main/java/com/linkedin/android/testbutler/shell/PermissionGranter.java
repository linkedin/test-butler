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

import android.annotation.TargetApi;
import android.os.Build;
import android.os.RemoteException;
import android.os.UserHandle;

import androidx.annotation.NonNull;

import com.linkedin.android.testbutler.utils.ExceptionCreator;
import com.linkedin.android.testbutler.utils.ReflectionUtils;

import java.lang.reflect.Field;
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

    private final ServiceManagerWrapper serviceManager;

    PermissionGranter(ServiceManagerWrapper serviceManager) {
        this.serviceManager = serviceManager;
    }

    @TargetApi(Build.VERSION_CODES.M)
    boolean grantPermission(@NonNull String packageName, @NonNull String permission) throws RemoteException {
        Object iPackageManager = serviceManager.getIService("package", "android.content.pm.IPackageManager");

        Method method = ReflectionUtils.getMethod(iPackageManager.getClass(),
                "grantRuntimePermission", String.class, String.class, int.class);

        int uid;
        try {
            Field user;
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
                user = UserHandle.class.getField("USER_OWNER");
            } else {
                user = UserHandle.class.getField("USER_SYSTEM");
            }
            uid = (int) user.get(null);
        } catch (NoSuchFieldException e) {
            throw ExceptionCreator.createRemoteException(TAG, "NoSuchFieldException during grantRuntimePermission", e);
        } catch (IllegalAccessException e) {
            throw ExceptionCreator.createRemoteException(TAG, "IllegalAccessException during grantRuntimePermission", e);
        }

        ReflectionUtils.invoke(method, iPackageManager, packageName, permission, uid);

        return true;
    }
}
