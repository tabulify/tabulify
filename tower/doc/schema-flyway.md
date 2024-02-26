# Schema / Database Management (FlyWay)

## About

Schema are defined and managed via the Flyway utility.

Prerequisites: [Logical data-model](data_model.md)

## Schema

There is 2 schema:
* [Realms](../src/main/resources/db/cs-realms) for the realm
* [Ip](../../vertx/src/main/resources/db/cs-ip) an application without any realm


## Architecture - one schema by realm vs one realm column

One schema by realm:
* The realm tables could have been created in a apart schema
but this data structure would have created a schema for every combo user.
* Because most of the users check
the product and don't use it, we would have an empty schema for each user.
* Furthermore, it's pretty difficult to check the schema with the IDE
if there is a long serie of schema to choose from.

For all this reasons, we choose to add a `realm_id` column to all `realm` tables.

## Flyway Usage
### Creation of Sql files for migration


The sql files that should be applied to migrate the schema
should go in each directory and follows the [flyway naming](https://flywaydb.org/documentation/concepts/migrations.html#naming):

#### Versioned script

Example for version:
* `v1`: [v1__create_ip_table.sql](../../vertx/src/main/resources/db/cs-ip/v1__create_ip_table.sql)
* `v1.1`: v1.1__xxx.sql
* `v1.1.1`: v1.1.1__xxx.sql

#### Repeatable Script

[Repeatable](https://flywaydb.org/documentation/concepts/migrations.html#repeatable-migrations)  `create or replace` script
We use the following naming: `R__order__name.sql`

They are run:
* last, after all pending versioned migrations have been executed.
* when their checksum changes.

Instead of being run just once, they are (re-)applied every time their checksum changes.

Within a single migration run, repeatable migrations are always applied last,
after all pending versioned migrations have been executed.

### Version History Table

The version history table is `version_log`

### Migration occurs when the server start

Flyway is called when the server starts.
via the [JdbMigration class](../../vertx/src/main/java/net/bytle/vertx/JdbcSchemaManager.java)

### Migration occurs with the following Gradle task

When developing the sql scripts, you may want to run it manually
to clean for instance.

The flyway conf is in the [gradle file](../tower.gradle.kts)
where you can see the local database configuration

Clean and migrate from the command line
* go to the tower directory
```bash
cd tower
```
* then call the local gradle command line tool (f12 in intellij) with
```bash
..\gradlew flywayClean flywayRealms
..\gradlew flywayClean flywayIp
```
where:
- `flywayClean` will delete the schemas
- `flywayRealms` will create the realms schema
- `flywayIp` will create/migrate the ip test schema

Other commands are also available.

## Principles

### Column Name

Every column name has the table `prefix`.
Why ? Because:
* it allows code completion (sql writing starts with the column list, not with the table, make it difficult for the intellisense to help typing)
* it permits to avoid name conflict (ie `name` for instance is a reserved word and should be quoted)
* it permits to see quickly for who is the attribute (ie `realm_name` is for the realm)
* it's consistent with the java naming (`realName`)
* it's used in the [guid identifier](identifier.md#guid) as header and to not mix id between tables.

Except for the `REALM_ID` and `ID` because
we use a trigger to generate the `ID` and the column name should be known
as pgsql is a static language and that the `record` data type does
not allow an access by array name.
See [identifier](identifier.md) for more information

### Don't use SQL Upsert

We don't use SQL `upsert` because they will increase the sequence
and create gap.
ie an insert is performed and rolled-back if the record already exists,
eating a number in the sequence.

### Identifier

See [identifier](identifier.md)

### Json data type

We:
  * use JSON storage to store the whole object in map/collection structure (ie to persist between restart or for archivage)
  * don't use Json datatype as storage structure for an object in a relational model

Why not in a relation model?

#### Polluted Json data, possible data loss

Because if you transform an existing object into json, say a list, you have to :
* exclude all data that is already stored in the row
* exclude all derived data served (for instance, registration URL)
* take into account that the name may change (from ownerApp to app) as we use an object mapper to exclude field.

The chance that you get unwanted Json data in the database is high.
Therefore, the chance that you get error while decoding is high.

When using a collection (a map), this a no-brainer but when you
get into relational model, it should be banned.

#### Counter More difficult to update

If you have the analytics in Json format, it's a little bit more difficult
to increment the counter (for instance, when a user is added to a list)

#### The update of Json will not fail even with a bad Json schema

If you have the analytics in Json format, the update
may not fail but the Json format stored may not be the good one.
By putting the data in the table, we give it a structure.

#### Json as string non queryable
If you store it as string, Jackson will escape the quote
making it non queryable by Postgres

Example:

```json
"{\"name\":\"Eraldy\"}"
```
in place of
```json
"{"name":"Eraldy"}"
```

You need then:
```json
SELECT (trim( replace(realm_data::text,'\',''),'"'))::jsonb->'name' FROM realm;
```


## Flyway Refactoring (Baseline)

Use baseline() or set baselineOnMigrate to true to initialize the schema history table.
