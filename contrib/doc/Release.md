# Release


## About

Release are tag based.

Tag the commit:
* after it was successfully built by CI
* or after it was manually tested

You can tag one commit several time:
* for example first time I tag it version-1.0.0-RC3
* after all manual tests are successfully done, the same commit can be tagged as final version-1.0.0.

The version is set in the `~/.gradle/gradle.properties` to `snapshot`, you
can override ti with the `Pversion` option.
```bash
gradlew -Pversion=x.y.z release
```

