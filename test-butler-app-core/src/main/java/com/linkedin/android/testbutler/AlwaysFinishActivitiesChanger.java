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

import android.os.Build;
import android.provider.Settings;
import android.util.Log;

/**
 * Helper class for modifying the always finish activities setting on the emulator
 */
class AlwaysFinishActivitiesChanger
{

    private static final String TAG = AlwaysFinishActivitiesChanger.class.getSimpleName();

    private final SettingsAccessor settings;

    private boolean originalAlwaysFinishActivitiesMode;

    AlwaysFinishActivitiesChanger(SettingsAccessor settings) {
        this.settings = settings;
    }

    /**
     * Should be called before starting tests, to save original always finish activities setting
     */
    void saveAlwaysFinishActivitiesState() {
        originalAlwaysFinishActivitiesMode = getAlwaysFinishActivitiesState();
    }

    /**
     * Should be called after testing completes, to restore original always finish activities setting
     */
    void restoreAlwaysFinishActivitiesState() {
        setAlwaysFinishActivitiesState(originalAlwaysFinishActivitiesMode);
    }

    /**
     * Set the always finish activities setting
     *
     * @param value           the desired always finish activities mode value to be set
     * @return true if the new value was set, false on database errors
     */
    boolean setAlwaysFinishActivitiesState(boolean value) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR1) {
            return settings.system().putInt(Settings.System.ALWAYS_FINISH_ACTIVITIES, value ? 1 : 0);
        } else {
            return settings.global().putInt(Settings.Global.ALWAYS_FINISH_ACTIVITIES, value ? 1 : 0);
        }
    }

    private boolean getAlwaysFinishActivitiesState() {
        try {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR1) {
                return settings.system().getInt(Settings.System.ALWAYS_FINISH_ACTIVITIES) != 0;
            } else {
                return settings.global().getInt(Settings.Global.ALWAYS_FINISH_ACTIVITIES) != 0;
            }
        } catch (Settings.SettingNotFoundException e) {
            Log.e(TAG, "Error reading always finish activities settings!", e);
            return false;
        }
    }
}
