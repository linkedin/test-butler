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

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.location.LocationManager;
import android.os.Build;
import android.provider.Settings;
import android.util.Log;

import androidx.annotation.NonNull;

/**
 * Helper class for modifying the location services mode on the emulator
 * <p>
 * NOTE: the settings for location services changed in Android 4.4 (KitKat). This class handles
 * both the pre and post KitKat methods for modifying location services.
 */
@SuppressLint("InlinedApi")
class LocationServicesChanger {

    private static final String TAG = LocationServicesChanger.class.getSimpleName();

    private final SettingsAccessor settings;

    private int originalLocationMode;
    private String originalLocationProviders;

    LocationServicesChanger(@NonNull SettingsAccessor settings) {
        this.settings = settings;
    }

    /**
     * Should be called before starting tests, to save original location services values
     */
    @SuppressWarnings("deprecation")
    void saveLocationServicesState() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
            originalLocationProviders = settings.secure().getString(Settings.Secure.LOCATION_PROVIDERS_ALLOWED);
        } else {
            try {
                originalLocationMode = settings.secure().getInt(Settings.Secure.LOCATION_MODE);
            } catch (Settings.SettingNotFoundException e) {
                Log.e(TAG, "Error reading location mode settings!", e);
            }
        }
    }

    /**
     * Should be called after testing completes, to restore original location services values
     */
    void restoreLocationServicesState() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
            setLocationProviders(originalLocationProviders);
        } else {
            setLocationMode(originalLocationMode);
        }
    }

    /**
     * Set a custom location mode
     *
     * @param locationMode    the desired location mode value to be set
     * @return true if the new value was set, false on database errors
     */
    boolean setLocationServicesState(int locationMode) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
            switch (locationMode) {
                case Settings.Secure.LOCATION_MODE_OFF:
                    return setLocationProviders("");
                case Settings.Secure.LOCATION_MODE_BATTERY_SAVING:
                    return setLocationProviders(LocationManager.PASSIVE_PROVIDER);
                case Settings.Secure.LOCATION_MODE_SENSORS_ONLY:
                    return setLocationProviders(
                            String.format("%s,%s",
                                    LocationManager.PASSIVE_PROVIDER,
                                    LocationManager.GPS_PROVIDER));
                case Settings.Secure.LOCATION_MODE_HIGH_ACCURACY:
                    return setLocationProviders(
                            String.format("%s,%s,%s",
                                    LocationManager.PASSIVE_PROVIDER,
                                    LocationManager.NETWORK_PROVIDER,
                                    LocationManager.GPS_PROVIDER));
                default:
                    throw new IllegalArgumentException("Unknown location mode: " + locationMode);
            }
        } else {
            return setLocationMode(locationMode);
        }
    }

    @SuppressWarnings("deprecation")
    protected boolean setLocationProviders(@NonNull String providers) {
        return settings.secure().putString(Settings.Secure.LOCATION_PROVIDERS_ALLOWED, providers);
    }

    @TargetApi(Build.VERSION_CODES.KITKAT)
    protected boolean setLocationMode(int locationMode) {
        return settings.secure().putInt(Settings.Secure.LOCATION_MODE, locationMode);
    }
}
