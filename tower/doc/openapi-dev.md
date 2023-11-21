# How to develop

## About
This page is a getting started on how to develop/modify the [applications](openapi.md)

## Step by step

### Modify the Spec file Contract first development

Modify the [specification file](openapi.md)

### Run the generator

* Run the corresponding generator with gradle with the idea or via the command line

Example:
* for the [combo private api app](openapi.md)
```dos
cd tower
..\gradlew openapi
```
* for the model only pojo in [eraldy-openapi-common.yaml](../src/main/openapi/eraldy-common-openapi.yaml)
```dos
cd tower
..\gradlew ModelGenerate
```

For more info, see [openapi-server-generator](openapi-server-generator.md)
## Idea and generated file quality

Idea is not happy with the import of the generated files.
We have set in idea, on save.
* the `Optimize import` on the fly
* remove `trailing space`

It would be better to have a process that normalize the files
(run prettier)

See [file-post-processing](https://openapi-generator.tech/docs/file-post-processing)
