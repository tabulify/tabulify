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

### No Recursive Persistence Problem

Because a user has a realm that has a user, the object build have recursion
error with Jackson.
To avoid this, we use Jackson Mixin but with GraphQL, you don't need it at all
because this is the client burden (ie the query that determine it)

### Schema based
No need to search for the endpoint path.
You work by entity. You search it, you update it.

### Query Aggregation and Transformation

* Multiple API fetches. Every field and nested object can get its own set of arguments while in Rest, you can only pass a single set of arguments (ie the query parameters and URL segments in your request).
* Data transformations: you can even pass arguments into scalar fields, to implement data transformations (Called [directive](#transformation-on-field-with-directive-date-formatting))

```graphql
{
  human(id: "1000") {
    name
    height(unit: FOOT)
  }
}
```

### Facade Persistence

No need to create multiple objects.

* Private field: For instance, we can have an object with a private database id, and
we don't need to use mixin or other functionality to not publish/cache it.
* Dynamic Listing: Due to field selection, we can just create a SQL and send it back, no need to create a pojo for the endpoint to advertise a fixed set of fields returned. One pojo less.

### Easy to find

Example creation path for a mailing of a list:
`/list/{listIdentifier}/mailing`
or here
`/mailing/list/{listIdentifier}`

with a function:
`mailingCreate(listIdentifier, xxx)`


### Modeling

* Match much more the code (no request body)
* less boilerplate
* Post or patch? Just create and update

### Incremental Graph Building

No need to build the whole object
Ie book has an owner if the owner is not requested don't make the request

### Hierarchical building is cost-efficient

Because the building happens hierarchically:
* if you do a batch select (ie select items to deliver), you are not building for each item, the order. In our case, the mailing, the app, the realm.
* you also do type/not found verification because the parent is build before.

### Partial data on errors

If there is an error to retrieve a sub-property, the
main property is returned.

### Partial Fetching / Object building

The building is hierarchical, query driven.

When building an object, you don't need to make extra select
to build a sub-object.

You build from the row and GraphQL takes care of the building of
the sub-object (known as connection) if wired.

You don't deal with it. You just build from the database row.
And it's so quieter in your head.

### Not blocking between connection

Because of the hierarchical nature of the object building,
if you use a connection, it's scoped to a unique rows.

You don't run the error that you may use 2 connections
that may block each other.

### Enforce not null / Avoid undefined

GraphQl will check if your value is conforming and throw an error if not.

You will minimize the common error:
```
Cannot read properties of undefined (reading 'substring')
```

If the returned data is null and should not, you'll get an error.

At request time, but you don't need to find it in the frontend where undefined / null should not be null.


```
2 GraphQL errors. Errors:
 - The field at path '/mailing/items[0]/failureCount' was declared as a non null type, but the code involved in retrieving data has wrongly returned a null value.  The graphql specification requires that the parent field be set to null, or if that is non nullable that it bubble up null to its parent and so on. The non-nullable type is 'Int' within parent type 'MailingItem',
 - The field at path '/mailing/items[1]/failureCount' was declared as a non null type, but the code involved in retrieving data has wrongly returned a null value.  The graphql specification requires that the parent field be set to null, or if that is non nullable that it bubble up null to its parent and so on. The non-nullable type is 'Int' within parent type 'MailingItem' (HttpStatus: 200)
```

### Private / Public Field Access

You may want to update aggregate such as the user count when a user is added.

The problem is that it's a private operation.
How do you do it?

You can always use the same input object without publicly advertising it.

### No need to have multiple API Endpoint for me vs user

When you have a API, you need to query information for the user logged in (ie `me`)
With Grapqhl, you need to make the distinction once, to return a user.
Thanks to the source you don't need to create multiple endpoint.

Example:
* realm owned by me
* realm owned by user

You just have
```graphql
type User {
  ownedRealms:[Realm]
}
type Query {
userMe:User
user(userGuid:ID!):User
}
```

### Less endpoint than a Rest API

Example:
```
/organization/:orgGuid/
/organization/:orgGuid/users
```
just
```graphql
type Query{
  organization(orgGuid:ID!): Organization
}
```

Example:
```
/user/me
/user/me/owned-realm
```
```
/realm/guid
/realm/guid/apps
```

### No endpoint confusion (ie endpoint disambiguation)

Where do you find the apps of a realm?
here:
```
/realm/guid/apps
```
or here:
```
/apps/guidRealm
```

Just here:
```graphql
query Realm {
  guid,
  apps {
    name,
    guid
  }
}
```

### Federated query

ie
```graphql
query Realm {
  guid,
  apps {
    name,
    guid
  }
}
```
and not:
  * first get realm
  * then get apps

## Implementation

See GraphQLService.java


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


### Mutation

[Mutation](https://www.graphql-java.com/documentation/execution#mutations)
Every mutation's response may include the modified data
to avoid a followup query of the client (or to simply update the cache)
```graphql
type Mutation {
  # This mutation takes id and email parameters and responds with a User
  updateUserEmail(id: ID!, email: String!): User
}
```

```graphql
mutation {
  mailingInsert(listGuid:"guid",props:{ name:"New Name" }){
    name
  }
}
```

Mutation can insert or update


### Authorization

See:
* https://www.graphql-java.com/documentation/sdl-directives
* https://www.graphql-java.com/documentation/field-visibility


### Transformation on field with directive (Date formatting,)

https://www.graphql-java.com/documentation/sdl-directives

Example: Date to String: https://www.graphql-java.com/documentation/sdl-directives#another-example---date-formatting

### Interface / Union (Inheritance)

There is no type resolver defined for interface / union 'MutationResponse' type
For inheritance, you need to define a resolver. See
https://www.graphql-java.com/documentation/schema/#datafetcher-and-typeresolver


### Exception / Error

GraphQL returns always:
  * `400` for a bad request (missing query or variable)
  * otherwise `200` with errors.

Errors can be found in the `errors` field

Example:
```json
{
    "errors": [
        {
            "message": "The field at path '/mailingUpdate/emailAuthor/emailAddress' was declared as a non null type, but the code involved in retrieving data has wrongly returned a null value.  The graphql specification requires that the parent field be set to null, or if that is non nullable that it bubble up null to its parent and so on. The non-nullable type is 'String' within parent type 'User'",
            "path": [
                "mailingUpdate",
                "emailAuthor",
                "emailAddress"
            ],
            "extensions": {
                "classification": "NullValueInNonNullableField"
            }
        }
    ],
    "data": { ... }
}
```
https://www.graphql-java.com/documentation/execution#exceptions-while-fetching-data


### Pojo / Generator

They are all Spring based.
Parser comes from GraphQL Java. See [Example](
https://github.com/kobylynskyi/graphql-java-codegen/blob/main/src/main/java/com/kobylynskyi/graphql/codegen/parser/GraphQLDocumentParser.java#L155)


## Exception handling

See https://www.graphql-java.com/documentation/execution#exceptions-while-fetching-data
