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
import android.os.IBinder;
import android.os.RemoteException;

import androidx.annotation.NonNull;

import com.linkedin.android.testbutler.utils.ExceptionCreator;

import java.lang.reflect.Method;

import static com.linkedin.android.testbutler.utils.ReflectionUtils.classForName;
import static com.linkedin.android.testbutler.utils.ReflectionUtils.getMethod;
import static com.linkedin.android.testbutler.utils.ReflectionUtils.invoke;


/**
 * A wrapper to expose hidden APIs in ServiceManager via reflection.
 */
class ServiceManagerWrapper {

    private static final String TAG = ServiceManagerWrapper.class.getSimpleName();

    private final Method getService;

    private ServiceManagerWrapper(Method getService) {
        this.getService = getService;
    }

    private IBinder getServiceBinder(String name, String serviceClassName) throws RemoteException {
        Object binder = invoke(getService, null, name);
        if (binder == null) {
            throw ExceptionCreator.createRemoteException(TAG, "No service " + name + " (" + serviceClassName + ") found on device", null);
        }
        return (IBinder) binder;
    }

    /**
     * Like ServiceManager#getService, but automatically wraps the returned generic Binder via the
     * given AIDL stub class's "asInterface(Binder)" method.
     * @param name The name of the service to fetch (See constants in {@link Context})
     * @param serviceClassName The fully qualified name of the (typically hidden) generated AIDL
     *                         class (e.g. "android.net.IConnectivityManager")
     * @return A proxy instance of the given service class.
     * @throws RemoteException if the given service does not exist
     */
    @NonNull Object getIService(@NonNull String name, @NonNull String serviceClassName) throws RemoteException {
        IBinder binder = getServiceBinder(name, serviceClassName);
        Class<?> stubClass = classForName(serviceClassName + "$Stub");
        Method asInterface = getMethod(stubClass, "asInterface", IBinder.class);
        return invoke(asInterface, null, binder);
    }

    @NonNull
    static ServiceManagerWrapper newInstance() {
        try {
            Class<?> serviceManagerClass = Class.forName("android.os.ServiceManager");
            Method getService = serviceManagerClass.getMethod("getService", String.class);
            return new ServiceManagerWrapper(getService);
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize ServiceManagerWrapper", e);
        }
    }
}
