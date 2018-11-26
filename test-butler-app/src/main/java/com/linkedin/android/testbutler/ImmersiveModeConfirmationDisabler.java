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
import androidx.annotation.NonNull;
import android.text.TextUtils;
import android.util.Log;

import java.lang.reflect.Field;

/**
 * A helper class for controlling Immersive mode confirmation (displayed first time activity starts in
 * immersive mode) before running Espresso tests.
 */
class ImmersiveModeConfirmationDisabler {
    private static final String TAG = ImmersiveModeConfirmationDisabler.class.getSimpleName();

    private static final String SETTING_VALUE_CONFIRMED = "confirmed";

    private ContentResolver contentResolver;

    private String immersiveModeConfirmationKey;
    private boolean originalImmersiveModeConfirmationFlag;

    ImmersiveModeConfirmationDisabler(@NonNull ContentResolver contentResolver) {
        this.contentResolver = contentResolver;

        immersiveModeConfirmationKey = null;
        try {
            Field fieldImmersiveModeConfirmation = Settings.Secure.class.getDeclaredField("IMMERSIVE_MODE_CONFIRMATIONS");
            immersiveModeConfirmationKey = (String) fieldImmersiveModeConfirmation.get(null);
            originalImmersiveModeConfirmationFlag = !TextUtils.equals(Settings.Secure.getString(contentResolver, immersiveModeConfirmationKey),
                                                                      SETTING_VALUE_CONFIRMED);
        } catch (NoSuchFieldException e) {
            Log.e(TAG, "Error getting immersive mode confirmation key:", e);
        } catch (IllegalAccessException e) {
            Log.e(TAG, "Error accessing immersive mode confirmation key", e);
        }
    }

    boolean setState(boolean enabled) {
        if (immersiveModeConfirmationKey != null) {
            Settings.Secure.putString(contentResolver, immersiveModeConfirmationKey, enabled ? "" : SETTING_VALUE_CONFIRMED);
            return true;
        }
        return false;
    }

    boolean restoreOriginalState() {
        return setState(originalImmersiveModeConfirmationFlag);
    }
}
