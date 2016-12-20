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

import android.content.ContentResolver;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.util.Log;

/**
 * A helper class for setting the ime keyboard
 *
 * This can be used to force the hardware ime keyboard, preventing most occurences of the software keyboard showing on
 * the screen in tests
 *
 * Requires API 22+
 */
public class ShowImeWithHardKeyboardHelper {

    private static final String TAG = ShowImeWithHardKeyboardHelper.class.getSimpleName();

    // The constant in Settings.Secure is marked with @hide, so we can't use it
    private static final String SOFT_KEYBOARD_SETTING = "show_ime_with_hard_keyboard";

    private boolean originalSoftKeyboardMode;

    /**
     * Should be called before starting tests, to save original ime keyboard state
     */
    void saveShowImeState(@NonNull ContentResolver contentResolver) {
        try {
            originalSoftKeyboardMode = Settings.Secure.getInt(contentResolver, SOFT_KEYBOARD_SETTING) == 1;
        } catch (Settings.SettingNotFoundException e) {
            Log.e(TAG, "Error reading soft keyboard (" + SOFT_KEYBOARD_SETTING + ") setting!", e);
        }
    }

    /**
     * Should be called after testing completes, to restore original ime keyboard state
     */
    void restoreShowImeState(@NonNull ContentResolver contentResolver) {
        setShowImeWithHardKeyboard(contentResolver, originalSoftKeyboardMode);
    }

    /**
     * Force the system to use the hardware keyboard
     * @param resolver the {@link ContentResolver} used to modify settings
     * @param enabled Whether to require the hardware keyboard or not
     * @return true if the value was set, false otherwise
     */
    public boolean setShowImeWithHardKeyboard(@NonNull ContentResolver resolver, boolean enabled) {
        int val = enabled ? 1 : 0;
        return Settings.Secure.putInt(resolver, SOFT_KEYBOARD_SETTING, val);
    }
}
