# Change Log

## Version 2.2.1 (2021-02-10)

- Fix signing code to sign the correct publications [#110](https://github.com/linkedin/test-butler/pull/110)
- Note: Older versions were incorrectly exposing some transitive dependencies such as junit as `api` dependencies.
  They have now been properly made `implementation` dependencies, so if you were depending on these through test-butler,
  you will need to add direct dependencies instead.

## Version 2.2.0 (2021-02-10)

- Moved artifact publishing from JCenter to Maven Central [#109](https://github.com/linkedin/test-butler/pull/109)

## Version 2.1.1 (2020-11-10)

- Increase timeout for TestButler service binding to avoid timeouts on some API levels [#106](https://github.com/linkedin/test-butler/pull/106)
- Fix physical device artifacts for API 21 and 22 [#101](https://github.com/linkedin/test-butler/pull/106)

## Version 2.1.0 (2019-11-19)

- Allow TestButler to simulate an AccessibilityService during tests

## Version 2.0.2 (2019-11-8)

- Fix crashes in test-butler library by publishing new `test-butler-api` artifact

## Version 2.0.1 (2019-11-5)

- NOTE: This release doesn't work since the `test-butler-api` artifact was not published. Use 2.0.2 instead.

- (Experimental) Add Test Butler APK for Physical Devices
- Allow Test Butler to run on Android Q(10) [#89](https://github.com/linkedin/test-butler/issues/89)
- Use instrumentation context to avoid crashes [#86](https://github.com/linkedin/test-butler/issues/86)

## Version 2.0.0 (2018-11-27)

- Migrate to AndroidX libraries
- Fix NoClassDefFoundError crash on API 15 [#81](https://github.com/linkedin/test-butler/issues/81)

## Version 1.4.0 (2018-07-30)

- Add support for changing "Always finish activities" dev setting [#78](https://github.com/linkedin/test-butler/issues/78)

## Version 1.3.2 (2018-03-24)

- Use new method for setting IActivityController on API 26+ [#70](https://github.com/linkedin/test-butler/pull/70)
- Fix crash when setting IActivityController on API 25+ [#66](https://github.com/linkedin/test-butler/issues/66)

## Version 1.3.1 (2017-05-25)

- Fix crash due to missing appEarlyNotResponding method [#60](https://github.com/linkedin/test-butler/issues/60)

## Version 1.3.0 (2017-04-05)

- Add api for enabling and disabling the immersive mode confirmation

## Version 1.2.0 (2017-01-06)

- Add api for enabling and disabling the spell checker during tests
- Disable spell checker by default during tests
- Add api for changing show_ime_with_hard_keyboard system setting during tests
- Disable show_ime_with_hard_keyboard system setting by default during tests

## Version 1.1.0 (2016-11-07)

- Add api for granting runtime permissions on API 23+
- Reduce log spam from NoDialogActivityController
- Preserve original values of animation scales after using Test Butler [#25](https://github.com/linkedin/test-butler/issues/25)
- Expose TestButler.verifyAnimationsDisabled for apps that want to validate that animations were actually disabled and fail fast [#1](https://github.com/linkedin/test-butler/issues/1)

## Version 1.0.1

- Update minSdkVersions to 14 - #6
- Add option for disabling GSM data - #13

## Version 1.0.0

- Initial release.
