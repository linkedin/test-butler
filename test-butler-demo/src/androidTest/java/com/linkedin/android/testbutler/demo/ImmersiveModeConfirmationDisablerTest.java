package com.linkedin.android.testbutler.demo;

import androidx.test.rule.ActivityTestRule;
import com.linkedin.android.testbutler.TestButler;
import org.junit.Rule;
import org.junit.Test;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;

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
