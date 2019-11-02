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

import android.accessibilityservice.AccessibilityServiceInfo;
import android.content.Context;
import android.os.RemoteException;

import androidx.annotation.NonNull;

import com.linkedin.android.testbutler.InstalledAccessibilityServiceProvider;
import com.linkedin.android.testbutler.utils.ExceptionCreator;
import com.linkedin.android.testbutler.utils.ReflectionUtils;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;

class AccessibilityManagerWrapper implements InstalledAccessibilityServiceProvider {
    private static final String TAG = AccessibilityManagerWrapper.class.getSimpleName();

    private final ServiceManagerWrapper serviceManager;

    AccessibilityManagerWrapper(ServiceManagerWrapper serviceManager) {
        this.serviceManager = serviceManager;
    }

    @SuppressWarnings("unchecked")
    @NonNull
    @Override
    public List<AccessibilityServiceInfo> getInstalledAccessibilityServiceList() throws RemoteException {
        try {
            Object iAccessibilityService = serviceManager.getIService(Context.ACCESSIBILITY_SERVICE,
                    "android.view.accessibility.IAccessibilityManager");
            Method method = ReflectionUtils.getMethod(iAccessibilityService.getClass(),
                    "getInstalledAccessibilityServiceList", int.class);
            List<AccessibilityServiceInfo> serviceInfos =
                    (List<AccessibilityServiceInfo>) method.invoke(iAccessibilityService, -1);
            if (serviceInfos != null) {
                return serviceInfos;
            }
            return Collections.emptyList();
        } catch (IllegalAccessException e) {
            throw ExceptionCreator.createRemoteException(TAG,
                    "IllegalAccessException during getInstalledAccessibilityServiceList", e);
        } catch (InvocationTargetException e) {
            throw ExceptionCreator.createRemoteException(TAG,
                    "InvocationTargetException during getInstalledAccessibilityServiceList", e);
        }
    }
}
