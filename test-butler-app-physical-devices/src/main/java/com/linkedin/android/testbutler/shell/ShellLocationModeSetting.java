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

import android.location.LocationManager;
import android.os.Build;
import android.provider.Settings;
import android.util.Log;
import android.view.accessibility.AccessibilityNodeInfo;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;


/**
 * Helper class for {@link ShellSettingsAccessor} which specifically handles
 * {@link Settings.Secure#LOCATION_MODE}. On some API versions, {@link Settings} has some
 * conditional logic for handling location mode and remapping it to location_provider_allowed calls.
 * In addition, on physical devices, enabling the network location provider causes a consent dialog
 * to be displayed, which the user must click 'accept' on in order to actually enable the provider.
 * This class handles the remapping and, when necessary, clicking accept on the dialog.
 */
class ShellLocationModeSetting {

    private static final String TAG = ShellLocationModeSetting.class.getSimpleName();

    private static final String NETWORK_CONSENT_ACTIVITY_PACKAGE_NAME = "com.google.android.gms";
    private static final String NETWORK_CONSENT_ACTIVITY_CLASS_NAME = "com.google.android.location.network.NetworkConsentActivity";
    private static final String NETWORK_CONSENT_ACCEPT_BUTTON_VIEW_ID = "android:id/button1";
    private static final int NETWORK_CONTENT_CLICK_ATTEMPTS = 4;
    private static final int NETWORK_CONTENT_SLEEP_BETWEEN_ATTEMPTS_MS = 500;

    private final ShellSettingsAccessor settings;

    ShellLocationModeSetting(@NonNull ShellSettingsAccessor settings) {
        this.settings = settings;
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    int getLocationMode() {
        String locationProviders = getLocationProviders();
        boolean gps = isProviderEnabled(locationProviders, LocationManager.GPS_PROVIDER);
        boolean network = isProviderEnabled(locationProviders, LocationManager.NETWORK_PROVIDER);

        if (gps && network) {
            return Settings.Secure.LOCATION_MODE_HIGH_ACCURACY;
        } else if (gps) {
            return Settings.Secure.LOCATION_MODE_SENSORS_ONLY;
        } else if (network) {
            return Settings.Secure.LOCATION_MODE_BATTERY_SAVING;
        } else {
            return Settings.Secure.LOCATION_MODE_OFF;
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    boolean setLocationMode(int locationMode) {
        boolean gps = false;
        boolean network = false;
        switch (locationMode) {
            case Settings.Secure.LOCATION_MODE_OFF:
                break;
            case Settings.Secure.LOCATION_MODE_BATTERY_SAVING:
                network = true;
                break;
            case Settings.Secure.LOCATION_MODE_SENSORS_ONLY:
                gps = true;
                break;
            case Settings.Secure.LOCATION_MODE_HIGH_ACCURACY:
                network = true;
                gps = true;
                break;
            default:
                throw new IllegalArgumentException("Unknown location mode: " + locationMode);
        }

        return setLocationProviders(gps, network);
    }

    @Nullable
    private String getLocationProviders() {
        return settings.secure().getString(Settings.Secure.LOCATION_PROVIDERS_ALLOWED);
    }

    private boolean setLocationProviders(@Nullable String providers) {
        return settings.secure().putString(Settings.Secure.LOCATION_PROVIDERS_ALLOWED, providers);
    }

    private boolean isProviderEnabled(@Nullable String allowedProviders, @NonNull String wantedProvider) {
        if (allowedProviders == null) {
            return false;
        }
        for (String provider : allowedProviders.split(",")) {
            if (wantedProvider.equals(provider)) {
                return true;
            }
        }
        return false;
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    private boolean setLocationProviders(boolean enableGps, boolean enableNetwork) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
            String providers = getLocationProviders();
            Log.d(TAG, "Current location providers: " + providers);

            boolean networkWasEnabled = isProviderEnabled(providers, LocationManager.NETWORK_PROVIDER);
            boolean gpsWasEnabled = isProviderEnabled(providers, LocationManager.GPS_PROVIDER);

            boolean success = true;
            if (networkWasEnabled != enableNetwork) {
                success = setLocationProviders((enableNetwork ? "+" : "-") + LocationManager.NETWORK_PROVIDER);

                if (enableNetwork) {
                    success = clickNetworkConsentAgreeButton() && success;
                }
            }

            if (gpsWasEnabled != enableGps) {
                success = setLocationProviders((enableGps ? "+" : "-") + LocationManager.GPS_PROVIDER) && success;
            }

            return success;
        } else {
            return setLocationProviders(String.format("%s%s,%s%s",
                    enableNetwork ? "+" : "-", LocationManager.NETWORK_PROVIDER,
                    enableGps ? "+" : "-", LocationManager.GPS_PROVIDER));
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    private boolean clickNetworkConsentAgreeButton() {
        try {
            // This is race-y... after turning on the network provider, something
            // else *might* turn it back off until you click an Accept button...
            // Not sure how long to wait for that other thing. If it's too long, we
            // unnecessarily block tests for a pop-up that'll never come. Too short,
            // and we think our setting worked but after some time it's reset.
            Log.d(TAG, "Network provider turned on, waiting for consent dialog");
            Thread.sleep(500);
            // todo: use ActivityController to wait for the activity and/or ContentObserver to wait for setting?

            if (isProviderEnabled(getLocationProviders(), LocationManager.NETWORK_PROVIDER)) {
                Log.d(TAG, "Network provider still enabled, assuming no consent dialog will be displayed");
                return true;
            }

            Log.d(TAG, "Network provider was disabled, looking for consent dialog");

            boolean clickedAgree = false;
            for (int i = 0; i < NETWORK_CONTENT_CLICK_ATTEMPTS; i++) {
                clickedAgree = clickedAgree || tryClickingConsentAgreeButton(i);

                if (clickedAgree) {
                    if (isProviderEnabled(getLocationProviders(), LocationManager.NETWORK_PROVIDER)) {
                        return true;
                    }

                    Log.d(TAG, String.format("Waiting for network provider to be re-enabled... (%s/4)", i + 1));
                    Thread.sleep(NETWORK_CONTENT_SLEEP_BETWEEN_ATTEMPTS_MS);
                } else if (i < NETWORK_CONTENT_CLICK_ATTEMPTS - 1) {
                    // don't bother waiting on last iteration if we didn't click accept yet...
                    Thread.sleep(NETWORK_CONTENT_SLEEP_BETWEEN_ATTEMPTS_MS);
                }
            }

            return isProviderEnabled(getLocationProviders(), LocationManager.NETWORK_PROVIDER);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        } catch (Exception e) {
            Log.e(TAG, "Failed to click network consent dialog", e);
            return false;
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    private static boolean tryClickingConsentAgreeButton(int attemptNumber) throws Exception {
        AccessibilityNodeInfo rootInActiveWindow = UiAutomationConnectionWrapper.getRootInActiveWindow();
        Log.d(TAG, "Current view: " + rootInActiveWindow.getPackageName());

        if (!NETWORK_CONSENT_ACTIVITY_PACKAGE_NAME.equalsIgnoreCase(rootInActiveWindow.getPackageName().toString())) {
            Log.d(TAG, String.format("Accept network provider consent dialog not found... (%s/4)", attemptNumber + 1));
            return false;
        }

        AccessibilityNodeInfo acceptButton = getNetworkConsentAgreeButton(rootInActiveWindow);
        if (acceptButton == null) {
            Log.d(TAG, String.format("Did not find accept button on network provider consent dialog... (%s/4)", attemptNumber + 1));
            return false;
        }

        acceptButton.performAction(AccessibilityNodeInfo.ACTION_CLICK);
        Log.d(TAG, "Clicked accept on the network provider consent dialog");
        return true;
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    @Nullable
    private static AccessibilityNodeInfo getNetworkConsentAgreeButton(@NonNull AccessibilityNodeInfo node) {
        if (node.isClickable()) {
            Log.d(TAG, "Consent dialog button found. Text: " + node.getText() + ", ID: " + node.getViewIdResourceName());
            // Text is locale dependent, but viewId seems to be consistent...
            if (NETWORK_CONSENT_ACCEPT_BUTTON_VIEW_ID.equals(node.getViewIdResourceName())) {
                return node;
            }
        }

        int count = node.getChildCount();
        for (int i = 0; i < count; i++) {
            AccessibilityNodeInfo button = getNetworkConsentAgreeButton(node.getChild(i));
            if (button != null) {
                return button;
            }
        }

        return null;
    }
}
