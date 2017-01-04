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


import android.os.Build;
import android.support.test.filters.SdkSuppress;
import android.support.test.rule.ActivityTestRule;
import android.widget.EditText;
import com.linkedin.android.testbutler.TestButler;
import org.junit.Rule;
import org.junit.Test;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.matcher.ViewMatchers.withId;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

public class ShowImeWithHardKeyboardHelperTest {

    @Rule public ActivityTestRule<MainActivity> testRule = new ActivityTestRule<>(MainActivity.class);

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.LOLLIPOP_MR1)
    @Test
    public void keyboardDoesNotShow() {
        TestButler.setShowImeWithHardKeyboardState(false);
        EditText editText = (EditText) testRule.getActivity().findViewById(R.id.editText);

        int before[] = new int[2];
        editText.getLocationOnScreen(before);

        onView(withId(R.id.editText)).perform(click());

        int after[] = new int[2];
        editText.getLocationOnScreen(after);

        // Check that the position has not changed at all
        assertEquals(before[0], after[0]);
        assertEquals(before[1], after[1]);
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.LOLLIPOP_MR1)
    @Test
    public void keyboardDoesShow() {
        TestButler.setShowImeWithHardKeyboardState(true);
        EditText editText = (EditText) testRule.getActivity().findViewById(R.id.editText);

        int before[] = new int[2];
        editText.getLocationOnScreen(before);

        onView(withId(R.id.editText)).perform(click());

        int after[] = new int[2];
        editText.getLocationOnScreen(after);

        // Only check that the y position changed
        assertNotEquals(before[1], after[1]);
    }
}
