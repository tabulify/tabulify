# Vertx Open API Generator


## About

Vertx does not have an Open API generator.
They have a contract-first design.

This repository creates a OpenAPI file from the Router.

## Annotation

https://swagger.io/docs/specification/about/

```java
@Operation(summary = "Find products by ID", method = "GET", operationId = "product/:productId",
    tags = {
      "Product"
    },
    parameters = {
      @Parameter(in = ParameterIn.PATH, name = "productId",
        required = true, description = "The unique ID belonging to the product", schema = @Schema(type = "string"))
    },
    responses = {
      @ApiResponse(responseCode = "200", description = "OK",
        content = @Content(
          mediaType = "application/json",
          encoding = @Encoding(contentType = "application/json"),
          schema = @Schema(name = "product", example =
            "{" +
              "'_id':'abc'," +
              "'title':'Red Truck'," +
              "'image_url':'https://images.pexels.com/photos/1112597/pexels-photo-1112597.jpeg'," +
              "'from_date':'2018-08-30'," +
              "'to_date':'2019-08-30'," +
              "'price':'125.00'," +
              "'enabled':true" +
              "}",
            implementation = Product.class)
        )
      ),
      @ApiResponse(responseCode = "404", description = "Not found."),
      @ApiResponse(responseCode = "500", description = "Internal Server Error.")
    }
  )
```



## Inspiration

  * https://github.com/outofcoffee/vertx-oas
  * https://github.com/ckaratzas/vertx-openapi-spec-generator
  * [https://github.com/anupsaund/vertx-auto-swagger](https://github.com/anupsaund/vertx-auto-swagger)

## Usage

  * Start the main method of [verticle](src/test/java/net/bytle/vertx/openapi/app/OpenApiVerticleTest.java)
  * Go to [http://localhost:8082/openapi.json](http://localhost:8082/openapi.json)
  * Go to [http://localhost:8082/openapi.yml](http://localhost:8082/openapi.yml)
  * Discover the api, run the docker command and go to [http://localhost:8888](http://localhost:8888)
```bash
docker run -itd --name openapi --rm -e URL="http://localhost:8082/openapi.json" -p 8888:8080 swaggerapi/swagger-ui
```

## Editor and validator

  * Go to https://editor.swagger.io/
  * And open [http://localhost:8082/openapi.json](http://localhost:8082/openapi.json)

## Note from the Web

  * See also this REST implementation: https://github.com/zandero/rest.vertx
