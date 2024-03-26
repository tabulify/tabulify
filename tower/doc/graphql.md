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

## Implementation

See GraphQLService

Implementation function:
* The returned class field names must match those of the corresponding GraphQL schema type.
*
