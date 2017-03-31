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

import java.lang.reflect.Field;

/**
 * A helper class for controlling Immersive mode confirmation (displayed first time activity starts in
 * immersive mode) before running Espresso tests.
 */

class ImmersiveModeConfirmationDisabler {
    private static final String TAG = ImmersiveModeConfirmationDisabler.class.getSimpleName();

    private String immersiveModeConfirmationKey;

    ImmersiveModeConfirmationDisabler() {
        immersiveModeConfirmationKey = null;
        try {
            Field fieldImmersiveModeConfirmation = Settings.Secure.class.getDeclaredField("IMMERSIVE_MODE_CONFIRMATIONS");
            immersiveModeConfirmationKey = (String) fieldImmersiveModeConfirmation.get(null);
        } catch (NoSuchFieldException e) {
            Log.e(TAG, "Error getting immersive mode dialog key:", e);
        } catch (IllegalAccessException e) {
            Log.e(TAG, "Error accessing immersive mode dialog key", e);
        }
    }

    boolean setState(@NonNull ContentResolver contentResolver, boolean enabled) {
        if (immersiveModeConfirmationKey != null) {
            Settings.Secure.putString(contentResolver, immersiveModeConfirmationKey, enabled ? "" : "confirmed");
            return true;
        }
        return false;
    }
}
