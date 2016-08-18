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

import android.content.Context;
import android.content.ContextWrapper;
import android.net.ConnectivityManager;
import android.os.Build;
import android.os.RemoteException;

import com.linkedin.android.testbutler.utils.ExceptionCreator;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * A helper class which can enable or disable data transferring via GSM networks
 */
final class GsmDataDisabler {
    private static final String TAG = GsmDataDisabler.class.getSimpleName();

    public boolean setGsmState(Context context, boolean enabled) throws RemoteException {
        Object manager;
        Method method;

        if(Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
           if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
               try {
                   manager = context.getSystemService(Context.CONNECTIVITY_SERVICE);
                   if (manager != null) {
                       method = ConnectivityManager.class.getDeclaredMethod("setMobileDataEnabled", boolean.class);
                   } else {
                       throw ExceptionCreator.createRemoteException(TAG, "No service " + ContextWrapper.CONNECTIVITY_SERVICE + " (ConnectivityManager) found on device", null);
                   }

                   if (method != null) {
                       method.setAccessible(true);
                       method.invoke(manager, enabled);
                       method.setAccessible(false);
                   } else {
                       throw ExceptionCreator.createRemoteException(TAG, "No setMobileDataEnabled(boolean) method inside ConnectivityManager", null);
                   }
               } catch (NoSuchMethodException e) {
                   throw ExceptionCreator.createRemoteException(TAG, "NoSuchMethodException exception in setMobileDataEnabled(boolean) method inside ConnectivityManager", e);
               } catch (InvocationTargetException e) {
                   throw ExceptionCreator.createRemoteException(TAG, "InvocationTargetException exception in setMobileDataEnabled(boolean) method inside ConnectivityManager", e);
               } catch (IllegalAccessException e) {
                   throw ExceptionCreator.createRemoteException(TAG, "IllegalAccessException exception in setMobileDataEnabled(boolean) method inside ConnectivityManager", e);
               }
           } else {
               throw ExceptionCreator.createRemoteException(TAG, "Api before " + Build.VERSION_CODES.KITKAT + " not supported because of WTF", null);
           }
        } else {
            try {
                manager = context.getSystemService(ContextWrapper.TELEPHONY_SERVICE);
                if (manager != null) {
                    method = manager.getClass().getDeclaredMethod("setDataEnabled", boolean.class);
                } else {
                    throw ExceptionCreator.createRemoteException(TAG, "No service " + ContextWrapper.TELEPHONY_SERVICE + " (TelephonyManager) found on device", null);
                }

                if (method != null) {
                    method.invoke(manager, enabled);
                } else {
                    throw ExceptionCreator.createRemoteException(TAG, "No setDataEnabled(boolean) method inside TelephonyManager", null);
                }
            } catch (NoSuchMethodException e) {
                throw ExceptionCreator.createRemoteException(TAG, "NoSuchMethodException exception in setDataEnabled(boolean) method inside TelephonyManager", e);
            } catch (InvocationTargetException e) {
                throw ExceptionCreator.createRemoteException(TAG, "InvocationTargetException exception in exception in setDataEnabled(boolean) method inside TelephonyManager", e);
            } catch (IllegalAccessException e) {
                throw ExceptionCreator.createRemoteException(TAG, "IllegalAccessException exception in exception in setDataEnabled(boolean) method inside TelephonyManager", e);
            }
        }
        return true;
    }
}
