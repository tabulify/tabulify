# Identifier

## Object Database id

The database id is a long (The hash algo `HashId` that creates [GUID](#guid) works only with `long`)

We generate our own sequence because:
* Sequence / serial create sequence with gap (see the [functions-sequence note](https://www.postgresql.org/docs/current/functions-sequence.html))
  * if the transaction is rolled back or does not succeed
  * with an upsert statement as the insert is sort of rolled back if there is a conflict
* We want to preserve the primary key constraints on partition. As the primary key should contain all partition columns. See [5.11.2.3. Limitations](https://www.postgresql.org/docs/current/ddl-partitioning.html#DDL-PARTITIONING-DECLARATIVE)

Therefore, we don't use a `sequence` or any other derived datatype  type such as  `GENERATED ALWAYS AS IDENTITY` or `BIGSERIAL` because they will create gap in the sequence.

The sequence is generated via the database procedure [set_id_on_insert](../src/main/resources/db/cs-realms/R__000.sql).
That the code is in a function database to be able to have it wrapped in
a single database transaction to get a rollback if any problems occurs.


## Guid

The guid is the:
* the public id
* the request id. When receiving a guid, a query is made.

The format of a Guid is:
- a prefix
- a hashed value

### Prefix
The prefix functionality are:
- seeing the object
- acts as a metadata for the hashed value
- used as `prefix` in column name for easy IDE completion.
- permits to not mix the guid (use a guid in place of another one)

If we lost the salt, secret, we may change the prefix

Example for list, the identifier is:
```
pub_hash
```
if we need to change the hash structure, the new identifier will be
```
pub1_hash
```
where `1` defines a new salt, secret and cipher

### Hashed

The hashed valued. For now, we used `HashId`
* secrecy: even if this is not completely a secret, that's good enough
* multiple id: it permits storing multiple id. As the realmId is always mandatory, the guid needs always two ids)
* easy to read: the generated hash are really nice url and human hash

Note that we may format it for easy reading. Example
```
SE4R-7LNH-X3TMJ-OJYN-ETVQ
```



## Handle (Named identifier)

Every `id` has a unique `Named identifier` for each [realm](data_model.md), called by default a `handle`.

The features are:
* allow upsert (in test)
* permits a human to see if the record is the good one
* permits to a human to fill them in a form (login handle for a user for instance)

Normally, a `handle` value can be changed as they are backed by the `id`.
