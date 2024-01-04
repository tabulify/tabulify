# OpenAPI


## About
The OpenApi specification is used to define the [applications](app.md)
that are build.

## How to

  * To know how to generate, see the [openapi dev](openapi-dev.md)
  * To know more about the server generator customization, see the [OpenApi Server Generator](openapi-server-generator.md)

## Spec files

This section has some information about the content of the spec files.

### Version

The version is the data. Example: `v2023-06-27`

We have added a `v` prefix because our generator deletes the quote
around the value in the generated openapi file making it not a string but a date.
It causes then an error inside Vertx as it expects a string.

There is actually no version used in the path or elsewhere by default.
If there is a need at some point to manage two versions, it can be
* a parameter for the operation
* a new operation with a deprecation
* a HTTP header. GitHub uses the `X-GitHub-Api-Version` [HTTP header](https://docs.github.com/en/rest/overview/api-versions?apiVersion=2022-11-28#specifying-an-api-version)

For the specification file, the version is mandatory. We use the date of release.

### Tag

In the [openapi.yaml](../src/main/openapi/eraldy-api-openapi.yaml), there is a tag by object type.
It will create an API interface and for each interface, we have the corresponding test class.

The goal is to be able to create test that are independent in order:
* First all realm test (in RealmApi),
* then all user test (in UserApi). You can't create a user without a realm,
* then all app test (in AppApi). You can't create an app without a user,
* then all list test (in ListApi). You can't create a list without an app,
* then all registration test (in RegistrationApi). You can't create a registration without a list.

### Data Type / Data Format supported / Validation

How to define data type and data validation in a OpenApi file.

The data type are detected via the [transformation](https://github.com/OpenAPITools/openapi-generator/blob/master/modules/openapi-generator/src/main/java/org/openapitools/codegen/languages/JavaVertXWebServerCodegen.java)

* [type](https://github.com/OpenAPITools/openapi-generator/blob/master/docs/generators/java-vertx-web.md#user-content-data-type-feature)
* [OpenAPI Format](https://github.com/eclipse-vertx/vertx-json-schema/blob/master/src/main/java/io/vertx/json/schema/openapi3/FormatValidatorFactory.java#L32) [Specification](https://datatracker.ietf.org/doc/html/draft-wright-json-schema-validation-00#section-7.3)
* [Validation Specification](https://datatracker.ietf.org/doc/html/draft-wright-json-schema-validation-00#section-7.3)

FYI: For date:
```yaml
properties:
  mydate:
    type: string
    format: date-time
```

For Long
```yaml
schema:
  type: integer
  format: int64
```

For Integer
```yaml
schema:
  type: integer
```

### Default

Default value is in the schema

```yaml
schema:
  default: true
```

### Enum
```yaml
- name: orderBy
  description: "A user attribute to order by"
  in: query
  schema:
    type: string
    enum:
      - 'asc'
      - 'desc'
```

May be more sophisticated:
https://openapi-generator.tech/docs/templating#all-generators-core

```yaml
WeatherType:
  type: integer
  format: int32
  enum:
    - 42
    - 18
    - 56
  x-enum-descriptions:
    - 'Blue sky'
    - 'Slightly overcast'
    - 'Take an umbrella with you'
  x-enum-varnames:
    - Sunny
    - Cloudy
    - Rainy
```

### File Upload

Example with the list import functionality that accepts a csv

```yaml
requestBody:
  description: The file containing the users to import
  required: true
  content:
    multipart/form-data:
      schema:
        type: object
        properties:
          fileBinary:
            type: string
            format: binary
            description: The file
      encoding: # https://github.com/OAI/OpenAPI-Specification/blob/main/versions/3.0.2.md#considerations-for-file-uploads
        fileBinary:
          contentType: text/csv # default is application/octet-stream
```

Ref:
  * https://vertx.io/docs/vertx-web/java/#_handling_file_uploads
  * https://github.com/vert-x3/vertx-examples/blob/4.x/web-examples/src/main/java/io/vertx/example/web/upload/Server.java
  * From [](https://vertx.io/docs/vertx-web-openapi/java/#_multipartform_data_validation), if the parameter has type: string and format: base64 or format: binary is a file upload with content-type application/octet-stream

## Schema composition

For schema composition, we use the [REF_AS_PARENT_IN_ALLOF](https://openapi-generator.tech/docs/customization#openapi-normalizer)

```kotlin
openapiNormalizer.set(
  mapOf(
    "REF_AS_PARENT_IN_ALLOF" to "true"
  )
)
```

The below object will `extend` the first Ref (ie Realm)
```yaml
RealmAnalytics:
  description: "Realm Analytics (count, ...)"
  allOf:
    - $ref: "#/components/schemas/Realm"
    - type: object
      properties:
        userCount:
          type: integer
          description: The number of users for the realm
```

FYI: See the spec for [Composition](https://spec.openapis.org/oas/v3.1.0#models-with-composition)

## Model / Pojo

We restrict the fields in the hashCode, equality and toString
because they can cause a circular reference and to be sure of the identity.

### Identity and Equality

The fields that identifies an object should be listed in the field `x-fields-identity`

```yaml
User:
  title: A user
  description: A user
  type: object
  x-fields-identity: # hashCode and equality
    - guid
```

### ToString

The fields for the toString function should be listed in the field `x-fields-to-string`


```yaml
User:
  title: A user
  description: A user
  type: object
  x-fields-to-string:
    - guid
    - email
    - handle
```

### Bring your own model
#### Third object

Example:
```yaml
schema:
  JsonObject:
    description: "A Json object"
    type: JsonObject # Th external type
```

Steps:
* Mapping as configuration (example in the configuration file)
```yaml
schemaMappings:
  JsonObject: "JsonObject" # The name of the type
importMappings:
  JsonObject: "io.vertx.core.json.JsonObject" # the string used in the import statement
```

#### Third Open Api Spec

The analytics event model is in another openapi file and other dependency, and we don't want it to be generated twice.

Example:
```yaml
schema:
  type: array
  items:
    $ref: '../../../../vertx/src/main/openapi/analytics-openapi.yaml#/components/schemas/AnalyticsEvent'
```


Steps:
* Mapping as configuration (example in gradle)
```kotlin
importMappings.set(
    mapOf(
      // Import Analytics objects from the common vertx module
      "AnalyticsEvent" to "net.bytle.vertx.analytics.model.AnalyticsEvent"
    )
)
```
* Ignore model in the ignore file [openapi-generator-ignore](../.openapi-generator-ignore)
```ignorelang
**/Analytics*.java
```

## Extra info

## Vertx Web OpenAPI Documentation

[Vertx OpenAPI module](https://vertx.io/docs/vertx-web-openapi/java/)
is used in the generated code.




### Editor / Schema Validation

  * Idea
  * OnlineEditor: https://editor-next.swagger.io/
  * https://apitools.dev/swagger-parser/online/



## Specification

https://spec.openapis.org/oas/latest.html

## List Problem because of Reserved words

`List` is a reserved word and the generator will create a `ModelList` class.
For now, we need to rename it to `List` manually.

```bash
# config
openapi-generator-cli config-help -g java-vertx-web
# reserved words listing
openapi-generator-cli config-help -g java-vertx-web --reserved-words
```
