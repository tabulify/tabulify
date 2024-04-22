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
via the [JdbcSchemaManager class](../../vertx/src/main/java/net/bytle/vertx/db/JdbcSchemaManager.java)

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
* it permits to avoid alias conflict in a Select sql. Example: all column name are unique.
```sql
select organization_user.* ,
   realm_user.user_password as user_password,
   realm_user.user_creation_time as user_creation_time,
   from ....
```
* it permits to see quickly for who is the attribute (ie `realm_name` is for the realm)
* it's consistent with the java naming (`realName`)
* it's used in the [guid identifier](identifier.md#guid) as header and to not mix id between tables.

Except for the `REALM_ID` and `ID` because
we use a trigger to generate the `ID` and the column name should be known
as pgsql is a static language and that the `record` data type does
not allow an access by array name.
See [identifier](identifier.md) for more information

### Item: List object naming (row, line, item, object, node)

For the element of a list, we may use the following term:
  * row,
  * line,
  * item,
  * object
  * node
  * entity

In general, we go with the delivery term: ie `item`

An order have `line of items` in a sales order.

### Count column naming

Count column are natural column. We use therefore
`failure_count` and not `count_failure`

### Renaming

When renaming, you should:
* in the backend
  * create the ALTER statement in the flyway migration script
  * change the correspondent `JdbcTableCols` enum.
  * change the corresponding `sql file`
  * change the Pojo
  * change the GraphQL API
* on the frontend
  * change the typescript type
  * change the GraphQL query

This is not an easy task.

We use a mix of:
* typed SQL with the `JdbcQuery` and `JdbcTableCols` for simple/single query
* and of dynamic SQL with `Sql file` for more complicated stuff

Because of the dynamic side, a column rename is a big endeavor, and we may forget a step (ie renamed in `SQL file` for instance)

If you don't feel safe:
  * don't, just change the wrapper (ie database class POJO)
  * or create a new column with deprecation (GraphQL permits that)
  * or create a view with an alias for the `data analytics guys`.

### Don't use SQL Upsert

We don't use SQL `upsert` because they will increase the sequence
and create gap.
ie in the upsert, an insert is performed and rolled-back if the record already exists,
eating a number in the sequence.


### Identifier

See [identifier](identifier.md)

### Handle

Some table have a unique column known as handle.

A handle is a textual identifier (same as DNS label)

This column has 2 purposes:
* the first: inserting/getting data in test
* a human id in Analytics event or URL so that you can read them (generated guid are pretty useless)

From a user perspective, they are not so important.

### Json data type

We:
  * use JSON storage to store the whole object in map/collection structure (ie to persist between restart or for archivage)
  * don't use Json datatype as storage structure for an object in a relational model

Why not in a relation model?


#### Strict vs Dynamic Datatype

Because JSON is a dynamic data type while a relational data type is static.

Json is a string, you need to cast all other type with error tht may ensue.

Can you update an integer with a string?
ie
```sql
update userTable set user = userDdata::jsonb->'id' ;
```

Nope:
```
SQL State  : 42804
Error Code : 0
ERROR: column "list_user_in_source_id" is of type integer but expression is of type jsonb
Hint: You will need to rewrite or cast the expression.
```
or
```
SQL State  : 22P02
Error Code : 0
Message    : ERROR: invalid input syntax for type integer: "inSourceId"
```
or
```
SQL State  : 42846
Error Code : 0
Message    : ERROR: cannot cast type jsonb to timestamp without time zone
```


#### Name migration

What if you want to migrate the column name?
in SQL:
```sql
alter table rename
```
In Json, you need to update the deserializer.
Migration:
* out of sync: the migration happens on read and write of object
* sync: you need to create a migration script (with sql or with your language)

And, your external sql will not fail even if the name is not good.
Example: This select will not fail, it will return null.
```sql
select '{}'::jsonb->'id'
```

#### Decoder/Encoder problem

JSON needs to be decoded in your database:
* Want a time? You need a time decoder and encoder

#### JSON in a string text

You may store a JSON in a string, but it may be then escaped.


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
