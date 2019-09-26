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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;


/**
 * Implementations of this class allow access to, and modification of, {@link Settings}.
 */
public interface SettingsAccessor {
    /**
     * @return Accessor for {@link Settings.Global}
     */
    @NonNull
    Namespace global();

    /**
     * @return Accessor for {@link Settings.System}
     */
    @NonNull
    Namespace system();

    /**
     * @return Accessor for {@link Settings.Secure}
     */
    @NonNull
    Namespace secure();

    /**
     * Accessor for a specific {@link Settings} namespace (one of {@link Settings.Global},
     * {@link Settings.System}, or {@link Settings.Secure}).
     */
    interface Namespace {
        @Nullable
        String getString(@NonNull String key);
        boolean putString(@NonNull String key, @Nullable String value);
        int getInt(@NonNull String key) throws Settings.SettingNotFoundException;
        boolean putInt(@NonNull String key, int value);
    }
}
