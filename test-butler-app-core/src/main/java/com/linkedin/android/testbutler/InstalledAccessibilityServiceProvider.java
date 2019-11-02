package com.linkedin.android.testbutler;

import android.accessibilityservice.AccessibilityServiceInfo;
import android.os.RemoteException;

import androidx.annotation.NonNull;

import java.util.List;

/**
 * Interface that returns the list of installed {@link AccessibilityServiceInfo} objects on the
 * device or emulator.
 */
public interface InstalledAccessibilityServiceProvider {

    @NonNull
    List<AccessibilityServiceInfo> getInstalledAccessibilityServiceList() throws RemoteException;

}
