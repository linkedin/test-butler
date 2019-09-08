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
package com.linkedin.android.testbutler.shell;

import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.Settings;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.linkedin.android.testbutler.SettingsAccessor;

import java.io.Closeable;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

import static com.linkedin.android.testbutler.shell.ShellButlerService.SHELL_PACKAGE;


/**
 * Allows access to device settings outside of an application context via direct calls to underlying
 * Settings content provider APIs. Must be used by the Shell user.
 */
class ShellSettingsAccessor implements SettingsAccessor, Closeable {

    private static final String TAG = ShellSettingsAccessor.class.getSimpleName();

    private static final String AUTHORITY = "settings";

    private final Object provider;
    private final Method call;
    private final Method getPairValue;
    private final Method removeContentProviderExternal;
    private final Object activityManager;
    private final Binder token;

    private final ShellLocationModeSetting locationModeSetting;

    private final Namespace global;
    private final Namespace system;
    private final Namespace secure;

    private ShellSettingsAccessor(Object provider, Method call, Method getPairValue,
                                  Method removeContentProviderExternal, Object activityManager,
                                  Binder token, UiAutomationConnectionWrapper uiAutomation) {
        this.provider = provider;
        this.call = call;
        this.getPairValue = getPairValue;
        this.removeContentProviderExternal = removeContentProviderExternal;
        this.activityManager = activityManager;
        this.token = token;

        this.global = new Global();
        this.system = new System();
        this.secure = new Secure();

        this.locationModeSetting = new ShellLocationModeSetting(this, uiAutomation);
    }

    @NonNull
    static ShellSettingsAccessor newInstance(@Nullable UiAutomationConnectionWrapper uiAutomation) throws Exception {
        try {
            Class<?> activityManagerClass = Class.forName("android.app.ActivityManagerNative");
            Class<?> iActivityManagerClass = Class.forName("android.app.IActivityManager");
            Class<?> iContentProviderClass = Class.forName("android.content.IContentProvider");

            Method getDefault = activityManagerClass.getMethod("getDefault");
            getDefault.setAccessible(true);  // not sure if this is necessary

            Method getContentProviderExternal;
            Binder token = new Binder();
            Object[] getContentProviderExternalArgs;
            Method callMethod;

            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                getContentProviderExternal = iActivityManagerClass.getMethod("getContentProviderExternal", String.class, int.class, IBinder.class);
                getContentProviderExternalArgs = new Object[] {AUTHORITY, 0, token};
                callMethod = iContentProviderClass.getMethod("call", String.class, String.class, String.class, Bundle.class);
            } else {
                getContentProviderExternal = iActivityManagerClass.getMethod("getContentProviderExternal", String.class, int.class, IBinder.class, String.class);
                getContentProviderExternalArgs = new Object[] {AUTHORITY, 0, token, null};
                callMethod = iContentProviderClass.getMethod("call", String.class, String.class, String.class, String.class, Bundle.class);
            }

            Method removeContentProviderExternal = iActivityManagerClass.getMethod("removeContentProviderExternal", String.class, IBinder.class);
            Method getPairValue = Bundle.class.getMethod("getPairValue");

            Object activityManager = getDefault.invoke(null);
            Object providerHolder = getContentProviderExternal.invoke(activityManager, getContentProviderExternalArgs);

            try {
                Field providerField = providerHolder.getClass().getField("provider");
                providerField.setAccessible(true);
                Object provider = providerField.get(providerHolder);

                return new ShellSettingsAccessor(provider, callMethod, getPairValue,
                        removeContentProviderExternal, activityManager, token, uiAutomation);
            } catch (Exception e) {
                removeContentProviderExternal.invoke(activityManager, "settings", token);
                throw e;
            }
        } catch (Exception e) {
            throw new Exception("Failed to initialize SettingsWrapper", e);
        }
    }

    public void close() {
        try {
            removeContentProviderExternal.invoke(this.activityManager, "settings", this.token);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @NonNull
    @Override
    public Namespace global() {
        return global;
    }

    @NonNull
    @Override
    public Namespace system() {
        return system;
    }

    @NonNull
    @Override
    public Namespace secure() {
        return secure;
    }

    class Namespace implements SettingsAccessor.Namespace {
        private final String name;
        private final String getMethod;
        private final String putMethod;

        private Namespace(String name, String getMethod, String putMethod) {
            this.name = name;
            this.getMethod = getMethod;
            this.putMethod = putMethod;
        }

        @Override
        public boolean putString(@NonNull String key, @Nullable String value) {
            Bundle arg = new Bundle();
            arg.putString(Settings.NameValueTable.VALUE, value);
            arg.putInt("_user", 0);

            try {
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                    call.invoke(provider, SHELL_PACKAGE, putMethod, key, arg);
                } else {
                    call.invoke(provider, SHELL_PACKAGE, AUTHORITY, putMethod, key, arg);
                }
                return true;
            } catch (Exception e) {
                Log.w(TAG, String.format("Failed to put setting: %s.%s = %s", name, key, value), e);
                return false;
            }
        }

        @Nullable
        @Override
        public String getString(@NonNull String key) {
            Bundle arg = new Bundle();
            arg.putInt("_user", 0);

            try {
                Bundle b;
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                    b = (Bundle) call.invoke(provider, SHELL_PACKAGE, getMethod, key, arg);
                } else {
                    b = (Bundle) call.invoke(provider, SHELL_PACKAGE, AUTHORITY, getMethod, key, arg);
                }
                if (b == null) {
                    return null;
                }
                return (String) getPairValue.invoke(b);
            } catch (Exception e) {
                Log.w(TAG, String.format("Failed to get setting: %s.%s", name, key), e);
                return null;
            }
        }

        @Override
        public int getInt(@NonNull String key) throws Settings.SettingNotFoundException {
            String value = getString(key);
            if (value == null || "null".equals(value)) {
                throw new Settings.SettingNotFoundException(key + " not found");
            }
            return Integer.parseInt(value);
        }

        @Override
        public boolean putInt(@NonNull String key, int value) {
            return putString(key, String.valueOf(value));
        }
    }

    class Global extends Namespace {
        private Global() {
            super("global", "GET_global", "PUT_global");
        }
    }

    class System extends Namespace {
        private System() {
            super("system", "GET_system", "PUT_system");
        }
    }

    class Secure extends Namespace {

        private Secure() {
            super("secure", "GET_secure", "PUT_secure");
        }

        @Override
        public int getInt(@NonNull String key) throws Settings.SettingNotFoundException {
            /* Android's Settings class does this mapping from location mode to providers.
             * This class bypasses the Settings class, we have to do that here too. */
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT
                    && Build.VERSION.SDK_INT < Build.VERSION_CODES.Q
                    && Settings.Secure.LOCATION_MODE.equals(key)) {
                return locationModeSetting.getLocationMode();
            }
            return super.getInt(key);
        }

        @Override
        public boolean putInt(@NonNull String key, int value) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT
                    && Build.VERSION.SDK_INT < Build.VERSION_CODES.Q
                    && Settings.Secure.LOCATION_MODE.equals(key)) {
                return locationModeSetting.setLocationMode(value);
            }

            return super.putInt(key, value);
        }
    }
}
