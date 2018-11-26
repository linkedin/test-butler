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

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.ContentResolver;
import android.location.LocationManager;
import android.os.Build;
import android.provider.Settings;
import androidx.annotation.NonNull;
import android.util.Log;

/**
 * Helper class for modifying the location services mode on the emulator
 * <p>
 * NOTE: the settings for location services changed in Android 4.4 (KitKat). This class handles
 * both the pre and post KitKat methods for modifying location services.
 */
@SuppressLint("InlinedApi")
class LocationServicesChanger {

    private static final String TAG = LocationServicesChanger.class.getSimpleName();

    private int originalLocationMode;
    private String originalLocationProviders;

    /**
     * Should be called before starting tests, to save original location services values
     */
    @SuppressWarnings("deprecation")
    void saveLocationServicesState(@NonNull ContentResolver contentResolver) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
            originalLocationProviders = Settings.Secure.getString(contentResolver,
                    Settings.Secure.LOCATION_PROVIDERS_ALLOWED);
        } else {
            try {
                originalLocationMode = Settings.Secure.getInt(contentResolver, Settings.Secure.LOCATION_MODE);
            } catch (Settings.SettingNotFoundException e) {
                Log.e(TAG, "Error reading location mode settings!", e);
            }
        }
    }

    /**
     * Should be called after testing completes, to restore original location services values
     */
    void restoreLocationServicesState(@NonNull ContentResolver contentResolver) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
            setLocationServicesStatePreKitKat(contentResolver, originalLocationProviders);
        } else {
            setLocationServicesState(contentResolver, originalLocationMode);
        }
    }

    /**
     * Set a custom location mode
     *
     * @param contentResolver the {@link ContentResolver} used to modify settings
     * @param locationMode    the desired location mode value to be set
     * @return true if the new value was set, false on database errors
     */
    boolean setLocationServicesState(@NonNull ContentResolver contentResolver, int locationMode) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
            switch (locationMode) {
                case Settings.Secure.LOCATION_MODE_OFF:
                    return setLocationServicesStatePreKitKat(contentResolver, "");
                case Settings.Secure.LOCATION_MODE_BATTERY_SAVING:
                    return setLocationServicesStatePreKitKat(contentResolver, LocationManager.PASSIVE_PROVIDER);
                case Settings.Secure.LOCATION_MODE_SENSORS_ONLY:
                    return setLocationServicesStatePreKitKat(
                            contentResolver, String.format("%s,%s",
                                    LocationManager.PASSIVE_PROVIDER,
                                    LocationManager.GPS_PROVIDER));
                case Settings.Secure.LOCATION_MODE_HIGH_ACCURACY:
                    return setLocationServicesStatePreKitKat(
                            contentResolver, String.format("%s,%s,%s",
                                    LocationManager.PASSIVE_PROVIDER,
                                    LocationManager.NETWORK_PROVIDER,
                                    LocationManager.GPS_PROVIDER));
                default:
                    throw new IllegalArgumentException("Unknown location mode: " + locationMode);
            }
        } else {
            return setLocationServicesStateKitKat(contentResolver, locationMode);
        }
    }

    @SuppressWarnings("deprecation")
    private boolean setLocationServicesStatePreKitKat(@NonNull ContentResolver contentResolver,
                                                      @NonNull String providers) {
        return Settings.Secure.putString(contentResolver, Settings.Secure.LOCATION_PROVIDERS_ALLOWED, providers);
    }

    @TargetApi(Build.VERSION_CODES.KITKAT)
    private boolean setLocationServicesStateKitKat(@NonNull ContentResolver contentResolver, int locationMode) {
        return Settings.Secure.putInt(contentResolver, Settings.Secure.LOCATION_MODE, locationMode);
    }
}
