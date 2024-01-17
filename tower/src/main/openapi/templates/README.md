
For more information, see [the documentation](../../../../doc/openapi-server-generator.md)

These templates are customization comes from
[JavaVertXWebServer](https://github.com/OpenAPITools/openapi-generator/tree/master/modules/openapi-generator/src/main/resources/JavaVertXWebServer)

## List of changes

* [enumOuterClass](enumOuterClass.mustache), we just have made the value final
* [enumClass](enumClass.mustache), we have:
  * made the value final
  * added the type of value
* [pojo](pojo.mustache), we have added:
  * the description
  * the custom field `x-fields-identity` and `x-fields-to-string` in the `pojo.mustache`
