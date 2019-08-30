/**
 * Copyright (C) 2019 LinkedIn Corp.
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

import android.os.RemoteException;

/**
 * Base implementation for ButlerApi.Stub. Handles all basic Settings-based calls.
 * {@link #onCreate(SettingsAccessor)} and {@link #onDestroy()} <b>must</b> be called from the
 * corresponding methods in the TestButler service!
 */
abstract class ButlerApiStubBase extends ButlerApi.Stub {

    private AnimationDisabler animationDisabler;
    private RotationChanger rotationChanger;
    private LocationServicesChanger locationServicesChanger;
    private SpellCheckerDisabler spellCheckerDisabler;
    private ShowImeWithHardKeyboardHelper showImeWithHardKeyboardHelper;
    private ImmersiveModeConfirmationDisabler immersiveModeDialogDisabler;
    private AlwaysFinishActivitiesChanger alwaysFinishActivitiesChanger;

    @Override
    public boolean setLocationMode(int locationMode) throws RemoteException {
        return locationServicesChanger.setLocationServicesState(locationMode);
    }

    @Override
    public boolean setRotation(int rotation) throws RemoteException {
        return rotationChanger.setRotation(rotation);
    }

    @Override
    public boolean setSpellCheckerState(boolean enabled) {
        return spellCheckerDisabler.setSpellChecker(enabled);
    }

    @Override
    public boolean setShowImeWithHardKeyboardState(boolean enabled) {
        return showImeWithHardKeyboardHelper.setShowImeWithHardKeyboardState(enabled);
    }

    @Override
    public boolean setImmersiveModeConfirmation(boolean enabled) throws RemoteException {
        return immersiveModeDialogDisabler.setState(enabled);
    }

    @Override
    public boolean setAlwaysFinishActivitiesState(boolean enabled) throws RemoteException {
        return alwaysFinishActivitiesChanger.setAlwaysFinishActivitiesState(enabled);
    }

    void onCreate(SettingsAccessor settings) {
        // Save current device rotation so we can restore it after tests complete
        rotationChanger = new RotationChanger(settings);
        rotationChanger.saveRotationState();

        // Save current location services setting so we can restore it after tests complete
        locationServicesChanger = new LocationServicesChanger(settings);
        locationServicesChanger.saveLocationServicesState();

        // Disable animations on the device so tests can run reliably
        animationDisabler = new AnimationDisabler();
        animationDisabler.disableAnimations();

        spellCheckerDisabler = new SpellCheckerDisabler(settings);
        spellCheckerDisabler.saveSpellCheckerState();
        // Disable spell checker by default
        spellCheckerDisabler.setSpellChecker(false);

        showImeWithHardKeyboardHelper = new ShowImeWithHardKeyboardHelper(settings);
        showImeWithHardKeyboardHelper.saveShowImeState();
        showImeWithHardKeyboardHelper.setShowImeWithHardKeyboardState(false);

        immersiveModeDialogDisabler = new ImmersiveModeConfirmationDisabler(settings);

        alwaysFinishActivitiesChanger = new AlwaysFinishActivitiesChanger(settings);
        alwaysFinishActivitiesChanger.saveAlwaysFinishActivitiesState();
    }

    void onDestroy() {
        // Re-enable animations on the emulator
        animationDisabler.enableAnimations();

        // Reset location services state to whatever it originally was
        locationServicesChanger.restoreLocationServicesState();

        // Reset rotation from the accelerometer to whatever it originally was
        rotationChanger.restoreRotationState();

        // Reset the spell checker to the original state
        spellCheckerDisabler.restoreSpellCheckerState();

        // Restore the original keyboard setting
        showImeWithHardKeyboardHelper.restoreShowImeState();

        // Restore immersive mode confirmation
        immersiveModeDialogDisabler.restoreOriginalState();

        // Restore always finish activities state to whatever it originally was
        alwaysFinishActivitiesChanger.restoreAlwaysFinishActivitiesState();
    }
}
