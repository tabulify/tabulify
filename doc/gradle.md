# Gradle


## Version

The version can be seen in the [gradle-wrapper.properties](../gradle/wrapper/gradle-wrapper.properties).
While writing this words, the version was 7.5.1.

## Command line script

You need to use the `gradlew` wrapper as it will call the good version
for you.

## Transitive Dependency

We don't use any transitive dependency rule, but we should.

For info: `dependency version alignment` supported with the concept of platform.
A platform is a set of modules which "work well together".

[Doc](https://docs.gradle.org/current/userguide/dependency_version_alignment.html#handling_inconsistent_module_versions)

## Daemon

See [gradle daemon](gradle-daemon.md)

## Debug, more info

* Run with --info or --debug option to get more log output.
* Run with --scan to get full insights.
