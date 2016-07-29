Releasing
========

 1. Change the version in `gradle.properties` to a non-SNAPSHOT version.
 2. Update the `CHANGELOG.md` for the impending release.
 3. Update the `README.md` with the new version.
 4. `git commit -am "Prepare for release X.Y.Z."` (where X.Y.Z is the new version)
 5. `git tag -a X.Y.Z -m "Version X.Y.Z"` (where X.Y.Z is the new version)
 6. `./gradlew clean bintrayUpload`
 7. Update the `gradle.properties` to the next SNAPSHOT version.
 8. `git commit -am "Prepare next development version."`
 9. `git push && git push --tags`


Prerequisites
-------------

In `local.properties`, set the following:

 * `bintray.user` - Bintray username for releasing to `com.linkedin.testbutler`.
 * `bintray.apikey` - Bintray API key for releasing to `com.linkedin.testbutler` (under edit profile).
 * `bintray.gpg.password` - GPG password for releasing to `com.linkedin.testbutler`.