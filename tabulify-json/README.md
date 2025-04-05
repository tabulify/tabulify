# Json (xml, yaml ??)


## About
This implementation implements the JSON format.

It should ultimately support also other hierarchical based format
(xml, yml, ...)

## Transformation

  * SQL: [Sqlite Json function](https://www.sqlite.org/json1.html)
## Standard Process

  * [Generating JSON from Tabular Data on the Web](https://www.w3.org/TR/csv2json/)

## Library
  * From the [library](https://mvnrepository.com/search?q=json&sort=popular),
  * https://stedolan.github.io/jq/ - Apply JSONPath expression to JSON.


### Gson
Gson was chosen because it implements a [JsonObject](https://github.com/google/gson/blob/master/gson/src/main/java/com/google/gson/JsonObject.java)
that is ordered and easy to use.

[DeepCopy](https://github.com/google/gson/blob/master/gson/src/main/java/com/google/gson/JsonObject.java#L41) is so beautiful

### Jackson

Jackson read and write from Object, making it popular
as format of exchange (json to xml, ...)

Jackson is popular and have the notion of a Json tree node
but there is several different node
  * JSONNode can read but not write
  * ObjectNode can write but you don't get write away the fact that this is a JSON object

### Org.json
Org.json was not chosen because it does not conserve the order of property insertion.
This is not required but pretty useful when debugging.

Advantage:
  * There is a single entry point to the JSON tree: ie the JSONObject
  * It can parse and add property easily and has an obvious name
  * Is also used as [Android](https://developer.android.com/reference/org/json/JSONObject)

### Vertx

Adding Vertx core to get JSON feature was a little bit too much.
Based on Jackson.


