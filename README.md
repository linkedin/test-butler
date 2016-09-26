# Test Butler
[![Build Status](https://travis-ci.org/linkedin/test-butler.svg?branch=master)](https://travis-ci.org/linkedin/test-butler)

Reliable Android testing, at your service.

## Motivation

Test Butler was inspired by the Google presentation "[Going Green: Cleaning up the Toxic Mobile Environment](https://www.youtube.com/watch?v=aHcmsK9jfGU)".
For more background, read the Test Butler announcement [blog post](https://engineering.linkedin.com/blog/2016/08/introducing-and-open-sourcing-test-butler--reliable-android-test).

## What does it do?

* Stabilizes the Android emulator, to help prevent test failures caused by emulator issues.
  * **Disables animations** so Espresso tests can run properly.
  * **Disables crash & ANR dialogs** so a system app misbehaving won't trigger a popup that causes your UI tests to fail.
  * **Locks the keyguard, WiFi radio, and CPU** to ensure they don't go to sleep unexpectedly while tests are running.
* Handles changing global emulator settings and holds relevant permissions so your app doesn't have to.
  * **Enable/disable WiFi:** Test Butler allows tests to simulate a situation where WiFi is not connected or changes availability at some point.
  * **Change device orientation:** Tests can manually set the orientation of the device during test execution.
  * **Set location services mode:** Test Butler lets your code simulate different location services modes, like battery saver or high accuracy.
  * **Set application locale:** Tests can set a custom `Locale` object for their application to simulate running the app in another language.

## How does it work?

Test Butler is a two-part project. It includes an Android library that your test code can depend on, as well as a companion Android app apk that is installed on your Android emulator before running tests.

The Test Butler library is a thin wrapper around an [AIDL interface](https://developer.android.com/guide/components/aidl.html) to give your tests a safe way to talk to the Test Butler app's service running in the background on the emulator.

The Test Butler app is signed using the system keystore for the Android emulator, so it is automatically granted `signature`-level permissions when installed. This means granting permissions via adb is not necessary. It also means that this app **can only be installed on emulators that use the stock Android keystore!**

Being a system app makes Test Butler much easier to use than existing Android solutions for changing emulator settings. To disable animations, you just need a single line of code in your app; no extra permissions in your debug manifest, no granting permissions via adb, no Gradle plugin to integrate.

Test Butler can even use permissions that can't be granted via adb, like the [`SET_ACTIVITY_WATCHER`](https://github.com/android/platform_frameworks_base/blob/master/core/res/AndroidManifest.xml#L1902) permission, which lets Test Butler disable crash & ANR dialogs during tests.

## Any "gotchas" to look out for?

Only one (and it's minor). Test Butler adds a custom [`IActivityController`](https://github.com/android/platform_frameworks_base/blob/master/core/java/android/app/IActivityController.aidl) to the system to be able to suppress crash & ANR dialogs. This technique is also used internally by the [`Monkey`](https://github.com/android/platform_development/blob/master/cmds/monkey/src/com/android/commands/monkey/Monkey.java#L255) tool. Unfortunately, the implementation of the [`isUserAMonkey()`](https://developer.android.com/reference/android/app/ActivityManager.html#isUserAMonkey()) method takes advantage of the fact that the `Monkey` class is the only thing inside Android that sets an `IActivityController` and [returns true whenever one is set](https://github.com/android/platform_frameworks_base/blob/master/services/core/java/com/android/server/am/ActivityManagerService.java#L10718).

This means that `isUserAMonkey()` **will return true** while Test Butler is running! If your app uses this method to invoke different behavior during actual monkey testing, you may encounter issues while running tests with Test Butler. An easy fix is to create a helper method in your app to call instead of `isUserAMonkey()`, which returns `false` while instrumentation tests are running and calls through to the real `isUserAMonkey()` when the app is not being instrumented.


## Download

Download the latest .apk and .aar via Maven:
```xml
    <dependency>
      <groupId>com.linkedin.testbutler</groupId>
      <artifactId>test-butler-library</artifactId>
      <version>1.0.0</version>
      <type>pom</type>
    </dependency>
    <dependency>
      <groupId>com.linkedin.testbutler</groupId>
      <artifactId>test-butler-app</artifactId>
      <version>1.0.0</version>
      <type>pom</type>
    </dependency>
```

or Gradle:
```
    androidTestCompile 'com.linkedin.testbutler:test-butler-library:1.0.0'
```

You can also download the apk file manually from [Bintray](https://bintray.com/linkedin/maven/test-butler-app/) if you prefer.

## Getting Started

Install the Test Butler apk on your emulator prior to running tests, then add the following to your test runner class:

```java

public class ExampleTestRunner extends AndroidJUnitRunner {
  @Override
  public void onStart() {
      TestButler.setup(InstrumentationRegistry.getTargetContext());
      super.onStart();
  }

  @Override
  public void finish(int resultCode, Bundle results) {
      TestButler.teardown(InstrumentationRegistry.getTargetContext());
      super.finish(resultCode, results);
  }
}
```

To change settings on the device from your tests, just use the methods in the `TestButler` class. For example:

```java
@BeforeClass
public static void setupClass() {
  TestButler.setLocationMode(Settings.Secure.LOCATION_MODE_OFF);
}

@Test
public void verifyBehaviorWithNoLocation() {
  // ...
}

@AfterClass
public static void teardownClass() {
  TestButler.setLocationMode(Settings.Secure.LOCATION_MODE_HIGH_ACCURACY);
}
```
