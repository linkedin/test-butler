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
