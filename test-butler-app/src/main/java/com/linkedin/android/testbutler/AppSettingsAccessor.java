package com.linkedin.android.testbutler;

import android.content.ContentResolver;
import android.os.Build;
import android.provider.Settings;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;


/**
 * Allows access to device settings from the context of an application via the standard
 * {@link Settings} APIs. Requires the application or service {@link ContentResolver}.
 */
public class AppSettingsAccessor implements SettingsAccessor {
    private final ContentResolver contentResolver;
    private final Namespace global;
    private final System system;
    private final Secure secure;

    public AppSettingsAccessor(@NonNull ContentResolver contentResolver) {
        this.contentResolver = contentResolver;

        system = new System();
        secure = new Secure();

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR1) {
            global = system;
        } else {
            global = new Global();
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

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR1)
    private final class Global implements Namespace {
        @Nullable
        @Override
        public String getString(@NonNull String key) {
            return Settings.Global.getString(contentResolver, key);
        }

        @Override
        public boolean putString(@NonNull String key, @Nullable String value) {
            return Settings.Global.putString(contentResolver, key, value);
        }

        @Override
        public int getInt(@NonNull String key) throws Settings.SettingNotFoundException {
            return Settings.Global.getInt(contentResolver, key);
        }

        @Override
        public boolean putInt(@NonNull String key, int value) {
            return Settings.Global.putInt(contentResolver, key, value);
        }
    }

    private final class System implements Namespace {
        @Nullable
        @Override
        public String getString(@NonNull String key) {
            return Settings.System.getString(contentResolver, key);
        }

        @Override
        public boolean putString(@NonNull String key, @Nullable String value) {
            return Settings.System.putString(contentResolver, key, value);
        }

        @Override
        public int getInt(@NonNull String key) throws Settings.SettingNotFoundException {
            return Settings.System.getInt(contentResolver, key);
        }

        @Override
        public boolean putInt(@NonNull String key, int value) {
            return Settings.System.putInt(contentResolver, key, value);
        }
    }

    private final class Secure implements Namespace {
        @Nullable
        @Override
        public String getString(@NonNull String key) {
            return Settings.Secure.getString(contentResolver, key);
        }

        @Override
        public boolean putString(@NonNull String key, @Nullable String value) {
            return Settings.Secure.putString(contentResolver, key, value);
        }

        @Override
        public int getInt(@NonNull String key) throws Settings.SettingNotFoundException {
            return Settings.Secure.getInt(contentResolver, key);
        }

        @Override
        public boolean putInt(@NonNull String key, int value) {
            return Settings.Secure.putInt(contentResolver, key, value);
        }
    }
}
