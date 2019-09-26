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

import android.content.Context;
import android.content.ContextWrapper;
import android.net.ConnectivityManager;
import android.os.Build;
import android.os.RemoteException;

import com.linkedin.android.testbutler.utils.ExceptionCreator;

import java.lang.reflect.Method;

import static com.linkedin.android.testbutler.utils.ReflectionUtils.getMethod;
import static com.linkedin.android.testbutler.utils.ReflectionUtils.invoke;

/**
 * A helper class which can enable or disable data transferring via GSM networks
 */
final class GsmDataDisabler {
    private static final String TAG = GsmDataDisabler.class.getSimpleName();

    private final ServiceManagerWrapper serviceManager;

    GsmDataDisabler(ServiceManagerWrapper serviceManager) {
        this.serviceManager = serviceManager;
    }

    boolean setGsmState(boolean enabled) throws RemoteException {
        Object manager;
        Method method;

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
            throw ExceptionCreator.createRemoteException(TAG, "Api before " + Build.VERSION_CODES.KITKAT + " not supported because of WTF", null);
        } else if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            manager = serviceManager.getIService(Context.CONNECTIVITY_SERVICE, "android.net.IConnectivityManager");
            method = getMethod(ConnectivityManager.class, "setMobileDataEnabled", boolean.class);
            method.setAccessible(true);
            invoke(method, manager, enabled);
            method.setAccessible(false);
        } else {
            manager = serviceManager.getIService(ContextWrapper.TELEPHONY_SERVICE, "com.android.internal.telephony.ITelephony");

            if (enabled) {
                invoke(getMethod(manager.getClass(), "enableDataConnectivity"), manager);
            } else {
                invoke(getMethod(manager.getClass(), "disableDataConnectivity"), manager);
            }
        }
        return true;
    }
}
