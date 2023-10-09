# Model


## Entity

* An organization is the business unit (facturation level) - not yet implemented
* A realm is the user level (one user by realm) (login, registration, ...)
* An application is the website level (content, ...)

## Relationship

* An organization may have several realms
* A [realm](realm.md) is a repository
* An app belongs to a realm
* A list belongs to a app lists
* A user belongs to a list


## Storage

For backup and storage:
* all organization and the realm definition table are in a schema known as the landlord (because this is a multi-tenancy architecture)
* a realm has its own schema
* an application has its own schema

Therefore, we may back up and apply features by realm and/or an application.

## Schema

See [Schema](schema.md) for the schema definitions
