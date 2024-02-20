# Model


## Entity

* An organization is the business unit (facturation level) - not yet implemented
* A realm is the user level (one user by realm) (login, registration, ...)
* An application is the website level (content, ...)

## Relationship

* An organization
  * is the root container
  * id is unique in the global scope
* A [realm](realm.md):
  * belongs to an org
  * id is unique in the global scope
* A user:
  * belongs to a realm
  * id is unique in the realm
* An app:
  * belongs to a realm
  * id is unique in the realm
* A list
  * belongs to an app
  * id is unique in the realm
  * User (user of a list)
    * belongs to a list and a user
    * id is the combination of realm, user and list id
* A mailing
  * belongs to an app and list
  * id is unique in the realm
...

## De-normalization

We have denormalized the realm and orga columns

### Realm

* The unit of storage and request redirection is the realm.
* The sequence of object id (known as id in the database or local id) starts
at one in each realm. (The most rows that we may get is the number of users).
* All object guid are the realm id and the object id.

### Organization

Why the org is present in table while it can be derived from the realm?
Because all owner user are organizational user, and we want to have a constraint
on that.

The organization value should not change at all. We may implement this feature,
but the change is pretty, pretty low.


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
