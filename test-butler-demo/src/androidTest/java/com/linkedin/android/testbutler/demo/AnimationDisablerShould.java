package com.linkedin.android.testbutler.demo;

import android.annotation.TargetApi;
import android.content.ContentResolver;
import android.os.Build;
import android.provider.Settings;
import android.support.test.InstrumentationRegistry;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

public class AnimationDisablerShould {

    @Test
    public void disableAnimations() throws Exception {
        ContentResolver resolver = InstrumentationRegistry.getTargetContext().getContentResolver();
        boolean isTransitionAnimationDisabled = isZero(getTransitionAnimationScale(resolver));
        boolean isWindowAnimationDisabled = isZero(getWindowAnimationScale(resolver));
        boolean isAnimatorDisabled = isZero(getAnimatorDurationScale(resolver));

        assertTrue(isTransitionAnimationDisabled);
        assertTrue(isWindowAnimationDisabled);
        assertTrue(isAnimatorDisabled);
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
