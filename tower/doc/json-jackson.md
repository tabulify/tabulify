# Json


Json is created/modified, serialized and deserialized with Jackson.

This is the library used by default by Vertx.

# Rules

## No JsonIgnore

We don't use JsonIgnore because we clone object to avoid circular reference via serialization/deserialization.

Example of chain that you may have:
```
net.bytle.tower.eraldy.model.openapi.User["realm"]
->net.bytle.tower.eraldy.model.openapi.Realm["defaultApp"]
->net.bytle.tower.eraldy.model.openapi.App["user"]
->net.bytle.tower.eraldy.model.openapi.User["realm"]
```
If the first and second realm are the same object

```yaml
x-extra-annotation: '@com.fasterxml.jackson.annotation.JsonIgnore'
```

https://fasterxml.github.io/jackson-annotations/javadoc/2.8/com/fasterxml/jackson/annotation/JsonIdentityInfo.html
```java
@JsonIdentityInfo(
// Object Identifier to use comes from a POJO property (getter method or field)
generator = ObjectIdGenerators.PropertyGenerator.class,
property = "guid",
scope = Realm.class
)
```
