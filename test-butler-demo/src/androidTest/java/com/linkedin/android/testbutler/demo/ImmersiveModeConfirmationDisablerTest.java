package com.linkedin.android.testbutler.demo;

import android.support.test.rule.ActivityTestRule;
import com.linkedin.android.testbutler.TestButler;
import org.junit.Rule;
import org.junit.Test;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withId;

public class ImmersiveModeConfirmationDisablerTest {
    @Rule public ActivityTestRule<MainActivity> testRule = new ActivityTestRule<>(MainActivity.class);

    @Test
    public void immersiveModeConfirmationDoesNotShow() {
        TestButler.setImmersiveModeConfirmation(false);

        onView(withId(R.id.button_switch_to_immersive_mode)).perform(click());
        onView(withId(R.id.button_call_butler)).perform(click());

        onView(withId(R.id.text_butler_response)).check(matches(isDisplayed()));
    }
}
