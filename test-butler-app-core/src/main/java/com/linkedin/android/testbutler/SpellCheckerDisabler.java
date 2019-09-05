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

import androidx.annotation.NonNull;

/**
 * A helper class for disabling or enabling the system spell checker
 */
public class SpellCheckerDisabler {

    private static final String TAG = SpellCheckerDisabler.class.getSimpleName();

    // The constant in Settings.Secure is marked with @hide, so we can't use it
    private static final String SPELL_CHECKER_SETTING = "spell_checker_enabled";

    private final SettingsAccessor settings;
    private boolean originalSpellCheckerMode;

    public SpellCheckerDisabler(@NonNull SettingsAccessor settings) {
        this.settings = settings;
    }

    /**
     * Should be called before starting tests, to save original spell checker state
     */
    void saveSpellCheckerState() {
        try {
            originalSpellCheckerMode = settings.secure().getInt(SPELL_CHECKER_SETTING) == 1;
        } catch (Settings.SettingNotFoundException e) {
            Log.e(TAG, "Error reading spell checker (" + SPELL_CHECKER_SETTING + ") setting!", e);
        }
    }

    /**
     * Should be called after testing completes, to restore original spell checker state
     */
    void restoreSpellCheckerState() {
        setSpellChecker(originalSpellCheckerMode);
    }

    /**
     * Enable or disable the system spell checker
     * @param enabled The desired state of the Spell Checker service
     * @return true if the value was set, false otherwise
     */
    public boolean setSpellChecker(boolean enabled) {
        int val = enabled ? 1 : 0;
        return settings.secure().putInt(SPELL_CHECKER_SETTING, val);
    }
}
