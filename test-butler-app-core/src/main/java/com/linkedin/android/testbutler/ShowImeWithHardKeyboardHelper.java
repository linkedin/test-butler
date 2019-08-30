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

/**
 * A helper class for setting the ime keyboard
 *
 * This can be used to disable the software ime when the device already has a hardware keyboard. By default on API 22+
 * emulators, the software keyboard will still be enabled even if the device is configured to include a hardware keyboard.
 * Note that you must still configure your emulator to have a hardware keyboard before this setting will have any effect.
 *
 * Requires API 22+
 */
public class ShowImeWithHardKeyboardHelper {

    private static final String TAG = ShowImeWithHardKeyboardHelper.class.getSimpleName();

    // The constant in Settings.Secure is marked with @hide, so we can't use it
    private static final String SHOW_IME_SETTING = "show_ime_with_hard_keyboard";

    private final SettingsAccessor settings;
    private boolean originalShowImeMode;

    public ShowImeWithHardKeyboardHelper(SettingsAccessor settings) {
        this.settings = settings;
    }

    /**
     * Should be called before starting tests, to save original ime state
     */
    void saveShowImeState() {
        try {
            originalShowImeMode = settings.secure().getInt(SHOW_IME_SETTING) == 1;
        } catch (Settings.SettingNotFoundException e) {
            Log.e(TAG, "Error reading soft keyboard (" + SHOW_IME_SETTING + ") setting!", e);
        }
    }

    /**
     * Should be called after testing completes, to restore original ime state
     */
    void restoreShowImeState() {
        setShowImeWithHardKeyboardState(originalShowImeMode);
    }

    /**
     * Tell the system to prefer the hardware IME
     *
     * This method has no effect on api levels below API 22
     *
     * You must have your emulator configured with a hardware IME, or this method has no effect
     * @param enabled Whether to require the hardware keyboard or not
     * @return true if the value was set, false otherwise
     */
    public boolean setShowImeWithHardKeyboardState(boolean enabled) {
        int val = enabled ? 1 : 0;
        return settings.secure().putInt(SHOW_IME_SETTING, val);
    }
}
