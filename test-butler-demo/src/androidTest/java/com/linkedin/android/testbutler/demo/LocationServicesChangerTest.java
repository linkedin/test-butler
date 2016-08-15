package com.linkedin.android.testbutler.demo;

import android.content.Context;
import android.location.LocationManager;
import android.provider.Settings;
import android.support.test.InstrumentationRegistry;
import com.linkedin.android.testbutler.TestButler;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class LocationServicesChangerTest {

    private LocationManager manager;

    @Before
    public void setup() {
        Context context = InstrumentationRegistry.getTargetContext();
        manager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
    }

    @Test
    public void alwaysLeavePassiveProviderEnabled() {
        // the passive provider is always enabled, regardless of the user's location preferences
        TestButler.setLocationMode(Settings.Secure.LOCATION_MODE_OFF);
        assertTrue(manager.isProviderEnabled(LocationManager.PASSIVE_PROVIDER));

        TestButler.setLocationMode(Settings.Secure.LOCATION_MODE_SENSORS_ONLY);
        assertTrue(manager.isProviderEnabled(LocationManager.PASSIVE_PROVIDER));

        TestButler.setLocationMode(Settings.Secure.LOCATION_MODE_BATTERY_SAVING);
        assertTrue(manager.isProviderEnabled(LocationManager.PASSIVE_PROVIDER));

        TestButler.setLocationMode(Settings.Secure.LOCATION_MODE_HIGH_ACCURACY);
        assertTrue(manager.isProviderEnabled(LocationManager.PASSIVE_PROVIDER));
    }

    @Test
    public void neverEnableNetworkProvider() {
        // the network provider doesn't work on emulators, so nothing we do should be able to turn it on
        // http://stackoverflow.com/questions/2424564/activating-network-location-provider-in-the-android-emulator
        TestButler.setLocationMode(Settings.Secure.LOCATION_MODE_OFF);
        assertFalse(manager.isProviderEnabled(LocationManager.NETWORK_PROVIDER));

        TestButler.setLocationMode(Settings.Secure.LOCATION_MODE_SENSORS_ONLY);
        assertFalse(manager.isProviderEnabled(LocationManager.NETWORK_PROVIDER));

        TestButler.setLocationMode(Settings.Secure.LOCATION_MODE_BATTERY_SAVING);
        assertFalse(manager.isProviderEnabled(LocationManager.NETWORK_PROVIDER));

        TestButler.setLocationMode(Settings.Secure.LOCATION_MODE_HIGH_ACCURACY);
        assertFalse(manager.isProviderEnabled(LocationManager.NETWORK_PROVIDER));
    }

    @Test
    public void enableGpsProviderForSensorsAndHighAccuracyOnly() {
        TestButler.setLocationMode(Settings.Secure.LOCATION_MODE_OFF);
        assertFalse(manager.isProviderEnabled(LocationManager.GPS_PROVIDER));

        TestButler.setLocationMode(Settings.Secure.LOCATION_MODE_SENSORS_ONLY);
        assertTrue(manager.isProviderEnabled(LocationManager.GPS_PROVIDER));

        TestButler.setLocationMode(Settings.Secure.LOCATION_MODE_BATTERY_SAVING);
        assertFalse(manager.isProviderEnabled(LocationManager.GPS_PROVIDER));

        TestButler.setLocationMode(Settings.Secure.LOCATION_MODE_HIGH_ACCURACY);
        assertTrue(manager.isProviderEnabled(LocationManager.GPS_PROVIDER));
    }
}
