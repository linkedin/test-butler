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

import android.provider.Settings;
import android.util.Log;
import android.view.Surface;

/**
 * Helper class for modifying the orientation of the emulator
 * <p>
 * Rotation is tracked by both the accelerometer sensor and an optional user override that can lock the device
 * in a particular orientation. This class disables accelerometer rotation to prevent unexpected sensor data
 * from breaking tests, and also allows test authors to modify the user rotation setting to test application
 * behavior under a given rotation.
 */
class RotationChanger {

    private static final String TAG = RotationChanger.class.getSimpleName();

    private final SettingsAccessor settings;
    private int originalAccelerometer;
    private int originalUserRotation;

    RotationChanger(SettingsAccessor settings) {
        this.settings = settings;
    }

    /**
     * Should be called before starting tests, to save original rotation values
     */
    void saveRotationState() {
        // Disable rotation from the accelerometer; 0 means off, 1 means on
        try {
            originalAccelerometer = settings.system().getInt(Settings.System.ACCELEROMETER_ROTATION);
        } catch (Settings.SettingNotFoundException e) {
            Log.d(TAG, "Could not read accelerometer rotation setting: " + e.getMessage());
        }
        try {
            originalUserRotation = settings.system().getInt(Settings.System.USER_ROTATION);
        } catch (Settings.SettingNotFoundException e) {
            Log.d(TAG, "Could not read user rotation setting: " + e.getMessage());
        }

        // Make sure we start the test in portrait and disable the accelerometer
        settings.system().putInt(Settings.System.USER_ROTATION, Surface.ROTATION_0);
        settings.system().putInt(Settings.System.ACCELEROMETER_ROTATION, 0);
    }

    /**
     * Should be called after testing completes, to restore original rotation values
     */
    void restoreRotationState() {
        settings.system().putInt(Settings.System.ACCELEROMETER_ROTATION, originalAccelerometer);
        settings.system().putInt(Settings.System.USER_ROTATION, originalUserRotation);
    }

    /**
     * Set a custom device rotation
     *
     * @param rotation        the desired rotation value to be set
     * @return true if the new value was set, false on database errors
     */
    boolean setRotation(int rotation) {
        if (rotation != Surface.ROTATION_0
                && rotation != Surface.ROTATION_90
                && rotation != Surface.ROTATION_180
                && rotation != Surface.ROTATION_270) {
            throw new IllegalArgumentException("Invalid parameter for screen rotation");
        }

        Log.d(TAG, "Setting screen orientation to " + rotation);

        // Use any of the Surface.ROTATION_ constants
        return settings.system().putInt(Settings.System.USER_ROTATION, rotation);
    }
}
