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

import android.content.ContentResolver;
import android.content.Context;
import android.provider.Settings;

import com.linkedin.android.testbutler.TestButler;

import org.junit.Before;
import org.junit.Test;

import androidx.annotation.NonNull;
import androidx.test.core.app.ApplicationProvider;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class AlwaysFinishActivitiesChangerTest {
    private ContentResolver contentResolver;

    @Before
    public void setup() {
        Context context = ApplicationProvider.getApplicationContext();
        contentResolver = context.getContentResolver();
    }

    @Test
    public void changeAlwaysFinishActivitiesSetting() {
        TestButler.setAlwaysFinishActivities(true);
        assertTrue(getAlwaysFinishActivitiesState(contentResolver));

        TestButler.setAlwaysFinishActivities(false);
        assertFalse(getAlwaysFinishActivitiesState(contentResolver));
    }

    private boolean getAlwaysFinishActivitiesState(@NonNull ContentResolver contentResolver) {
        try {
            return Settings.Global.getInt(contentResolver, Settings.Global.ALWAYS_FINISH_ACTIVITIES) != 0;
        } catch (Settings.SettingNotFoundException e) {
            fail();
            return false;
        }
    }
}
