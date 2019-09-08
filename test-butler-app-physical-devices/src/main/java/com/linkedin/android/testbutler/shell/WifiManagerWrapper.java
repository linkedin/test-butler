package com.linkedin.android.testbutler.shell;

import android.content.Context;
import android.os.RemoteException;
import android.util.Log;

import androidx.annotation.NonNull;

import com.linkedin.android.testbutler.utils.ReflectionUtils;

import java.lang.reflect.Method;


/**
 * A wrapper to invoke WifiManager methods from a shell context.
 */
class WifiManagerWrapper {

    private static final String TAG = WifiManagerWrapper.class.getSimpleName();

    private final Object wifiService;
    private final Method setWifiEnabled;

    private WifiManagerWrapper(Object wifiService, Method setWifiEnabled) {
        this.wifiService = wifiService;
        this.setWifiEnabled = setWifiEnabled;
    }

    @NonNull
    static WifiManagerWrapper getInstance(@NonNull ServiceManagerWrapper serviceManager) {
        Object wifiService = null;
        Method setWifiEnabled = null;
        try {
            wifiService = serviceManager.getIService(Context.WIFI_SERVICE, "android.net.wifi.IWifiManager");
            setWifiEnabled = ReflectionUtils.getMethod(wifiService.getClass(), "setWifiEnabled", String.class, boolean.class);
        } catch (RemoteException e) {
            Log.w(TAG, "Failed to create WifiManagerWrapper. setWifiEnabled will not work.", e);
        }
        return new WifiManagerWrapper(wifiService, setWifiEnabled);
    }

    boolean setWifiEnabled(boolean state) throws RemoteException {
        if (setWifiEnabled == null) {
            Log.w(TAG, "setWifiEnabled is disabled. See previous logcat error.");
            return false;
        }

        return (boolean) ReflectionUtils.invoke(setWifiEnabled, wifiService, ShellButlerService.SHELL_PACKAGE, state);
    }
}
