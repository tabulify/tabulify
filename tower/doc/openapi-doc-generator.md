# OpenAPI Documentation generation

## About
The API documentation is generated on the fly via Javascript.
with the [index file](../src/main/resources/openapi-doc)

## Mount

The doc:
* is mounted at the versioned root path with the `/doc` path.
* is shown if you hit the root path of the api

The mount is done via the [ApiDoc class](../src/main/java/net/bytle/tower/eraldy/api/ApiDoc.java)

## Why?

Because:
* this is a most beautiful doc than the standard generated doc of `openapi-generator.tech`
* it does not require any build step
