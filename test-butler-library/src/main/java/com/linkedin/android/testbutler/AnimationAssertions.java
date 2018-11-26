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

import android.annotation.TargetApi;
import android.content.ContentResolver;
import android.content.Context;
import android.os.Build;
import android.provider.Settings;
import androidx.annotation.NonNull;

import static junit.framework.Assert.assertTrue;
import static junit.framework.Assert.fail;

class AnimationAssertions {

    static void verifyAnimationsDisabled(@NonNull Context context) {
        ContentResolver resolver = context.getContentResolver();

        try {
            boolean isTransitionAnimationDisabled = isZero(getTransitionAnimationScale(resolver));
            boolean isWindowAnimationDisabled = isZero(getWindowAnimationScale(resolver));
            boolean isAnimatorDisabled = isZero(getAnimatorDurationScale(resolver));

            assertTrue(isTransitionAnimationDisabled);
            assertTrue(isWindowAnimationDisabled);
            assertTrue(isAnimatorDisabled);
        } catch (Exception e) {
            fail("Could not check current status of animation scales");
        }
    }

    private static boolean isZero(float value) {
        return Float.compare(Math.abs(value), 0.0f) == 0;
    }

    @SuppressWarnings("deprecation")
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
    private static float getTransitionAnimationScale(ContentResolver resolver) throws Exception {
        return getSetting(resolver, Settings.Global.TRANSITION_ANIMATION_SCALE,
                Settings.System.TRANSITION_ANIMATION_SCALE);
    }

    @SuppressWarnings("deprecation")
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
    private static float getWindowAnimationScale(ContentResolver resolver) throws Exception {
        return getSetting(resolver, Settings.Global.WINDOW_ANIMATION_SCALE,
                Settings.System.WINDOW_ANIMATION_SCALE);
    }

    @SuppressWarnings("deprecation")
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
    private static float getAnimatorDurationScale(ContentResolver resolver) throws Exception {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            return getSetting(resolver, Settings.Global.ANIMATOR_DURATION_SCALE,
                    Settings.System.ANIMATOR_DURATION_SCALE);
        }
        return 0f;
    }

    private static float getSetting(ContentResolver resolver, String current, String deprecated) throws Exception {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            return Settings.Global.getFloat(resolver, current);
        } else {
            return Settings.System.getFloat(resolver, deprecated);
        }
    }
}
