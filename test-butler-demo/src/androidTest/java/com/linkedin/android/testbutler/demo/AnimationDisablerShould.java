package com.linkedin.android.testbutler.demo;

import android.support.test.InstrumentationRegistry;
import com.linkedin.android.testbutler.TestButler;
import org.junit.Test;

public class AnimationDisablerShould {

    @Test
    public void disableAnimations() throws Exception {
        TestButler.verifyAnimationsDisabled(InstrumentationRegistry.getTargetContext());
    }
}
