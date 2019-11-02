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
package com.linkedin.android.testbutler;

import android.accessibilityservice.AccessibilityServiceInfo;
import android.os.Build;
import android.os.RemoteException;
import android.provider.Settings;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.List;

/**
 * Helper class for turning on and off the {@link ButlerAccessibilityService} to simulate having
 * an accessibility service enabled during tests.
 */
public class AccessibilityServiceEnabler {

    private final SettingsAccessor settingsAccessor;
    private final InstalledAccessibilityServiceProvider serviceProvider;

    public AccessibilityServiceEnabler(@NonNull InstalledAccessibilityServiceProvider serviceProvider,
                                       @NonNull SettingsAccessor settingsAccessor) {
        this.serviceProvider = serviceProvider;
        this.settingsAccessor = settingsAccessor;
    }

    /**
     * Enable the {@link ButlerAccessibilityService}.
     *
     * @param enabled True to enable the service if it is not already started, false to disable it.
     * @return True if the requested action was performed. False if it was not.
     */
    public boolean setAccessibilityServiceEnabled(boolean enabled) throws RemoteException {
        if (enabled) {
            final String enabledServices = settingsAccessor.secure().getString(
                    Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);

            if (enabledServices != null
                    && enabledServices.contains(ButlerAccessibilityService.SERVICE_NAME)) {
                return false;
            }


            String id = findButlerAccessibilityServiceId();
            if (id != null && id.endsWith(ButlerAccessibilityService.SERVICE_NAME)) {
                StringBuilder builder = new StringBuilder();
                if (enabledServices != null) {
                    builder.append(enabledServices);
                    builder.append(ButlerAccessibilityService.COMPONENT_NAME_SEPARATOR);
                }
                builder.append(id);

                settingsAccessor.secure().putString(
                        Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
                        builder.toString());
                settingsAccessor.secure().putInt(
                        Settings.Secure.ACCESSIBILITY_ENABLED,
                        1);

                if (!ButlerAccessibilityService.waitForLaunch()) {
                    settingsAccessor.secure().putString(
                            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
                            enabledServices);

                    throw new RuntimeException("Starting Butler accessibility service took " +
                            "longer than " + ButlerAccessibilityService.CREATE_DESTROY_TIMEOUT
                            + "ms");
                }

                return true;
            }
            return false;
        } else {
            if (Build.VERSION.SDK_INT >= 24) {
                return ButlerAccessibilityService.disable();
            } else {
                final String enabledServices = settingsAccessor.secure().getString(
                        Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);

                if (enabledServices == null ||
                        !enabledServices.contains(ButlerAccessibilityService.SERVICE_NAME)) {
                    return false;
                }

                String id = findButlerAccessibilityServiceId();
                if (id != null && id.endsWith(ButlerAccessibilityService.SERVICE_NAME)) {
                    if (!enabledServices.contains(id)) {
                        return false;
                    }
                    String newServices = enabledServices.replaceAll(id + ":?", "");
                    settingsAccessor.secure().putString(
                            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
                            newServices);

                    if (!ButlerAccessibilityService.waitForDestroy()) {
                        throw new RuntimeException("Shutting down Butler accessibility " +
                                "service took longer than "
                                + ButlerAccessibilityService.CREATE_DESTROY_TIMEOUT + "ms");
                    }

                    return true;
                }
                return false;
            }
        }
    }

    @Nullable
    private String findButlerAccessibilityServiceId() throws RemoteException {
        List<AccessibilityServiceInfo> accessibilityServices =
                serviceProvider.getInstalledAccessibilityServiceList();
        for (AccessibilityServiceInfo service : accessibilityServices) {
            String serviceId = service.getId();
            if (serviceId.endsWith(ButlerAccessibilityService.SERVICE_NAME)) {
                return serviceId;
            }
        }
        return null;
    }
}
