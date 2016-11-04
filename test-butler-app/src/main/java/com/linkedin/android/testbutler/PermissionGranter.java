package com.linkedin.android.testbutler;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.UserHandle;
import android.support.annotation.NonNull;
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
