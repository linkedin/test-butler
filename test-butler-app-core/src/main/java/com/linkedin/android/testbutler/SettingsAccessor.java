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


/**
 * Implementations of this class allow access to, and modification of, {@link Settings}.
 */
interface SettingsAccessor {
    /**
     * @return Accessor for {@link Settings.Global}
     */
    Namespace global();

    /**
     * @return Accessor for {@link Settings.System}
     */
    Namespace system();

    /**
     * @return Accessor for {@link Settings.Secure}
     */
    Namespace secure();

    /**
     * Accessor for a specific {@link Settings} namespace (one of {@link Settings.Global},
     * {@link Settings.System}, or {@link Settings.Secure}).
     */
    interface Namespace {
        String getString(String key);
        boolean putString(String key, String value);
        int getInt(String key) throws Settings.SettingNotFoundException;
        boolean putInt(String key, int value);
    }
}
