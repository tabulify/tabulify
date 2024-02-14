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

## Primary Key

All primary key are a compound of:
  * a realm id
  * and an object id.

Even if a list is dependent of an app, the primary key is not realm id, app id and list id, but
realm and list.

Why ? Why not a full compound pk?
Because if in a list, we used have a full compound pk (ie realm id, app id and list id),
a list user would also have this three columns.
Meaning that if a list change of app, we need to modify all the child tables.

## Sequence

Because by realm, we may get a lot of users and this is our app partition,
we use our own dependent realm sequence for the direct realm child id.

Example for the user table:

^ Realm Id ^ User Id ^
| 1 | 1 |
| 1 | 2 |
| 2 | 1 |
| 2 | 2 |

See the RealmSequenceProvider.java


## Storage

For backup and storage:
* all organization and the realm definition table are in a schema known as the landlord (because this is a multi-tenancy architecture)
* a realm has its own schema
* an application has its own schema

Therefore, we may back up and apply features by realm and/or an application.

## Schema

See [Schema](schema-flyway.md) for the schema definitions
