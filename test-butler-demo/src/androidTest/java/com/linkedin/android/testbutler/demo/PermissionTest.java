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
import android.util.Log;

import com.linkedin.android.testbutler.TestButler;

import junit.framework.Assert;

import org.junit.Before;
import org.junit.Test;

public class PermissionTest {
    private static final String TAG = PermissionTest.class.getSimpleName();
    private final Context targetContext = InstrumentationRegistry.getTargetContext();

    @Before
    public void beforeTest(){
        TestButler.revokePermission(targetContext.getPackageName(), Manifest.permission.CALL_PHONE);
    }

    @Test
    public void alwaysLeavePassiveProviderEnabled() {
        //Precondition
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            int permission = targetContext.checkSelfPermission(Manifest.permission.CALL_PHONE);
            Log.v(TAG, "Permission granting " + permission);

            Assert.assertTrue("Precondition: permission already granted ", permission == PackageManager.PERMISSION_DENIED);
        } else {
            Log.e(TAG, "Permission test is useless on device prior api " + Build.VERSION_CODES.M);
        }

        //Action
        String result = TestButler.grantPermission(targetContext.getPackageName(), Manifest.permission.CALL_PHONE);
        Log.v(TAG, "Permission granting " + result);

        //Check
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Assert.assertTrue("Precondition: permission already granted ", targetContext.checkSelfPermission(Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED);
        }
        Log.v(TAG, "Finished");
    }
}
