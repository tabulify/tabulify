# GraphQL


## Why?

### Form Granularity
You may split:
* the UI (form and view) by logic entity
* while the database has only one row

Example:
Mailing has:
* a email subject
* a email preview
* a email body
* a name
* a sender
You may create a form for:
* the email parts (sender, subject)
* the scalar parts
* only one for the body (due to complex UI)
* or you may decide to update only the name.
With graphQL, it's up to the front end developer
to choose and not to the backend (Rest API)

### Listing Granularity

Listing granularity is pretty difficult to define in advance.
With a rest api, if you don't want to send all data, you create a specialised point.


### Schema based
No need to search for the endpoint path.
You work by entity. You search it, you update it.

### Query Aggregation and Transformation

* Multiple API fetches. Every field and nested object can get its own set of arguments while in Rest, you can only pass a single set of arguments (ie the query parameters and URL segments in your request).
* Data transformations: you can even pass arguments into scalar fields, to implement data transformations

```graphql
{
  human(id: "1000") {
    name
    height(unit: FOOT)
  }
}
```
## Implementation

See GraphQLService


### Fetching / RuntimeWiring

To [fetch](https://www.graphql-java.com/documentation/data-fetching), you need to map:
  * operation
  * field (optional)
to a dataFetcher method with the `RuntimeWiring` builder object

If a field of a type is not mapped, by default, it gets the `graphql.schema.PropertyDataFetcher`
that supports
  * pojo (searches for a getter `public String getField()`)
  * map (searches for the key `field`)

Each DataFetcher is passed a [graphql.schema.DataFetchingEnvironment object](https://www.graphql-java.com/documentation/data-fetching#the-interesting-parts-of-the-datafetchingenvironment) which contains:
* what field is being fetched,
* what arguments have been supplied to the field
* and other information such as:
  * the field's type,
  * its parent type,
  * the query root object
  * or the query context object.


### New Schema field type such as Email

See [Custom scalar](https://www.graphql-java.com/documentation/scalars#writing-your-own-custom-scalars)
