package com.linkedin.android.testbutler.demo;

import android.content.Context;
import android.view.accessibility.AccessibilityManager;

import androidx.test.core.app.ApplicationProvider;

import com.linkedin.android.testbutler.TestButler;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class AccessibilityEnablerTest {

    private AccessibilityManager accessibilityManager;

    @Before
    public void setup() {
        Context context = ApplicationProvider.getApplicationContext();
        this.accessibilityManager =
                (AccessibilityManager) context.getSystemService(Context.ACCESSIBILITY_SERVICE);
    }

    @Test
    public void startAndStopAccessibility() {
        TestButler.setAccessibilityServiceState(true);
        assertTrue(accessibilityManager.isEnabled());

        TestButler.setAccessibilityServiceState(false);
        assertFalse(accessibilityManager.isEnabled());
    }
}
