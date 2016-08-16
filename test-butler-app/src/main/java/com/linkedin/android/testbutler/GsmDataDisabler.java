package com.linkedin.android.testbutler;

import android.content.ContextWrapper;
import android.os.Build;
import android.os.RemoteException;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.telephony.TelephonyManager;
import android.util.Log;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * A helper class which can enable or disable data transferring
 */
/* package */ final class GsmDataDisabler {
    private static final String TAG = GsmDataDisabler.class.getSimpleName();

    public boolean setGsmState(ContextWrapper contextWrapper, boolean enabled) throws RemoteException {
        TelephonyManager telephonyManager;
        Method setMobileDataEnabledMethod;

        telephonyManager = (TelephonyManager) contextWrapper.getSystemService(ContextWrapper.TELEPHONY_SERVICE);

        if (telephonyManager != null) {
            Log.v(TAG, "TelephonyManager successfully received");
            try {
                setMobileDataEnabledMethod = telephonyManager.getClass().getDeclaredMethod("setDataEnabled", boolean.class);

                if (setMobileDataEnabledMethod != null) {
                    setMobileDataEnabledMethod.invoke(telephonyManager, enabled);
                } else {
                    throw createException("No setDataEnabled(boolean) method inside TelephonyManager", null);
                }
            } catch (NoSuchMethodException e) {
                throw createException("No setDataEnabled(boolean) method inside TelephonyManager", e);
            } catch (InvocationTargetException e) {
                throw createException("Invocation exception in setDataEnabled(boolean) method inside TelephonyManager", e);
            } catch (IllegalAccessException e) {
                throw createException("IllegalAccessException exception in setDataEnabled(boolean) method inside TelephonyManager", e);
            }
        } else {
            throw createException("No service " + ContextWrapper.TELEPHONY_SERVICE + " found on device (TelephonyManager)", null);
        }

        return true;
    }

    private RemoteException createException(@NonNull String message, @Nullable Exception exception) {
        RemoteException remoteException;
        if(Build.VERSION.SDK_INT > Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            remoteException = new RemoteException(message);
        } else {
            Log.e(TAG, message, exception);
            remoteException = new RemoteException();
        }

        if(exception != null) {
            if(Build.VERSION.SDK_INT > Build.VERSION_CODES.KITKAT) {
                remoteException.addSuppressed(exception);
            } else {
                Log.e(TAG, message, exception);
            }
        }
        return remoteException;
    }
}
