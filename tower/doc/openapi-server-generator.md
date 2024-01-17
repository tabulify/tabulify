# OpenApi Server Generator


## About
We use [openapi-generator](https://openapi-generator.tech/)
to generate the class


## Generation

You can generate the server code with gradle task.

See [the openapi dev](openapi-dev.md)

## Generation Details

Check the [gradle file](../tower.gradle.kts) and search for the tasks to see the configuration.

The generated code goes into packages called `openapi`. If you see a package with this name,
you know that the code is generated.

For instance, for the [combo private API](../src/main/openapi/eraldy-combo-private-openapi.yaml):
* the generated interface goes [there](../src/main/java/net/bytle/tower/eraldy/app/comboprivateapi/openapi/interfaces).
* the generated invoker (the code that ties the interface to vertx together) goes [there](../src/main/java/net/bytle/tower/eraldy/app/comboprivateapi/openapi/invoker).
* the generated model (the pojos) goes [there](../src/main/java/net/bytle/tower/eraldy/model/openapi). They are shared between the combo apps
* the implementation, the code that we wrote, goes [there](../src/main/java/net/bytle/tower/eraldy/app/comboprivateapi/openapi)
* the generated root OpenAPI document `openapi.yaml` goes [there](../src/main/resources/openapi-spec-file/eraldy/combo-private/openapi.yaml)


## Custom

`openapi-generator` uses [Mustache Template](https://openapi-generator.tech/docs/templating) to generate the code.

We have made some custom change to adapt to our workflow. This section is about these changes.

We have customized the generation with:
* [the ignore file (to ignore generated files)](#openapi-generator-ignore)
* [custom mustache template (the code is generated via template)](#custom-mustache-template)

### Custom Mustache template

We have:
* added a SupportingFiles: [ApiVertxSupport.mustache](../src/main/openapi/templates/supportFiles/ApiVertxSupport.mustache) to generate the mount automatically (See the [customization doc](https://openapi-generator.tech/docs/customization)
* customized the [vertx mustaches templates](../src/main/openapi/templates/README.md)

The added custom templates (SupportingFiles) are defined in the [openapi-generator-config.yaml](../.openapi-generator-eraldy-api-config.yaml)

See [Vertx Web generator](https://github.com/OpenAPITools/openapi-generator/blob/master/docs/generators/java-vertx-web.md) where you can see the option and the data type mapping

### OffsetDateTime to LocalDateTime

We have changed the default `OffsetDateTime` to `LocalDateTime`
to show that it's a local UTC timestamp.
(ie LocalDateTime is a UTC time. The toString method outputs the UTC+0 time)

As asked for
* [Mixpanel](https://docs.mixpanel.com/docs/other-bits/tutorials/developers/mixpanel-for-developers-fundamentals#supported-data-types)
* Postgres
* ...

```kotlin
typeMappings.set(
    mapOf(
      "OffsetDateTime" to "LocalDateTime"
    )
  )
importMappings.set(
  mapOf(
    "java.time.OffsetDateTime" to "java.time.LocalDateTime"
  )
)
```

### For object inheritance
* For inheritance, see [](openapi.md#schema-composition)

```kotlin
openapiNormalizer.set(
  mapOf(
  "REF_AS_PARENT_IN_ALLOF" to "true"
  )
)
```

### .openapi-generator-ignore

`.openapi-generator-ignore` is an ignore file that contains
the file that will not be generated.
It's located at the root of the project.

```
pom.xml
README.md
```

## Debug: Get data model

To see the data model that is passed to the template, you can set the following
properties in the gradle task.
```kotlin
globalProperties.set(
  mapOf(
    // print the data model passed to template
    "debugModels" to "true"
  )
)
```
