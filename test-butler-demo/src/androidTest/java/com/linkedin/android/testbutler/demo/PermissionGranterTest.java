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
package com.linkedin.android.testbutler.demo;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.support.test.InstrumentationRegistry;
import android.support.test.filters.SdkSuppress;
import android.support.v4.content.ContextCompat;
import com.linkedin.android.testbutler.TestButler;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class PermissionGranterTest {

    /**
     * This is not a great test...it will only pass once, unless you revoke the permission externally between test runs
     */
    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.M)
    public void successfullyGrantPermission() {
        Context context = InstrumentationRegistry.getTargetContext();
        String permission = Manifest.permission.WRITE_EXTERNAL_STORAGE;

        long result = ContextCompat.checkSelfPermission(context, permission);
        assertEquals(PackageManager.PERMISSION_DENIED, result);

        TestButler.grantPermission(context, permission);

        long newResult = ContextCompat.checkSelfPermission(context, permission);
        assertEquals(PackageManager.PERMISSION_GRANTED, newResult);
    }

    @Test(expected = IllegalArgumentException.class)
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.M)
    public void failWhenTryingToGrantNonDangerousPermission() {
        Context context = InstrumentationRegistry.getTargetContext();
        String permission = Manifest.permission.ACCESS_WIFI_STATE;

        TestButler.grantPermission(context, permission);
    }

    @Test(expected = IllegalArgumentException.class)
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.M)
    public void failWhenTryingToGrantPermissionNotInManifest() {
        Context context = InstrumentationRegistry.getTargetContext();
        String permission = Manifest.permission.ADD_VOICEMAIL;

        TestButler.grantPermission(context, permission);
    }
}
