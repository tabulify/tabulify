# Javascript Web App

## About
Javascript Web App are not yet used in production but there is a build system
in gradle.

## How we build
We use the [gradle node plugin](https://github.com/srs/gradle-node-plugin/blob/master/docs/node.md) to build

* a `javascriptBuild` operations to build the app
* a `javascriptInstall` operations to copy the app
* a process resource hook to copy start the copy when process resources start.

```kotlin
val processResources by tasks.getting(ProcessResources::class) {
  dependsOn(copyToWebRoot)
}
```

## Configuration / Plugin

We don't use the [gradle node plugin](https://github.com/node-gradle/gradle-node-plugin/blob/master/docs/usage.md#configuring-the-plugin)
because it does [not support gradle3](https://github.com/node-gradle/gradle-node-plugin/issues/176)
