-- All create or replace script in one file
-- because:
--   * flyway can't manage easily the order of execution and the option to ignore missing R script is for paying customer only
--   * IDE code analysis IDE will see object even if they are not in the database
--   * with `ctrl-F12` on Idea, you get the list of statements
--   * we can navigate between functions in the file

CREATE OR REPLACE FUNCTION get_executing_schema_from_search_path()
  returns text
  LANGUAGE plpgsql
AS
$$
DECLARE
  search_path                 text;
  search_path_part            text;
  search_path_parts           text[];
  stack                       text;
  executing_function          text;
  executing_schema            text;
  search_path_expected_schema text = 'cs_realms';
BEGIN
  -- get the schema of the function
  GET DIAGNOSTICS stack = PG_CONTEXT;
  executing_function = substring(stack from 'function (.*?) line');

  -- search path when started in a do block execution
  -- with flyway : "cs_realms","$user", public
  -- with the IDE console: public, cs_realms
  select current_setting('search_path') into search_path;

  select regexp_split_to_array(search_path, E',\\s*') into search_path_parts;
  FOREACH search_path_part in array search_path_parts
    LOOP
      raise NOTICE '%: found path schema part (%)',executing_function, search_path_part;
      IF (search_path_part != 'public') THEN
        executing_schema = search_path_part;
        EXIT;
      END IF;
    END LOOP;
  if (executing_schema is null) THEN
    RAISE EXCEPTION '%: The executing schema was not found in the search path', search_path;
  end if;
  -- flyway, we resolve it without sql quote because
-- the value is used later in select
  executing_schema = trim(both '"' from search_path_expected_schema);
  if (executing_schema != search_path_expected_schema) THEN
    RAISE EXCEPTION '%: The schema from flyway should be cs_realms but was %. It was extracted from the search path (%)', executing_function, executing_schema, search_path;
  END IF;
  return executing_schema;
END;
$$;


create or replace view table_partition_definition as
select par.relnamespace::regnamespace::text as schema,
       par.relname                          as table_name,
       partnatts                            as num_columns,
       column_index,
       col.column_name,
       partition_strategy
from (select partrelid,
             partnatts,
             case partstrat
               when 'l' then 'list'
               when 'r' then 'range' end as partition_strategy,
             unnest(partattrs)              column_index
      from pg_partitioned_table) pt
       join
     pg_class par
     on
         par.oid = pt.partrelid
       join
     information_schema.columns col
     on
           col.table_schema = par.relnamespace::regnamespace::text
         and col.table_name = par.relname
         and ordinal_position = pt.column_index;

comment on view table_partition_definition is 'The partition data found in the DDL of the table';

-- list of table and the partition
create or replace view table_partition_list as
SELECT nmsp_parent.nspname AS table_schema,
       parent.relname      AS table_name,
       nmsp_child.nspname  AS partition_schema,
       child.relname       AS partition_name
FROM pg_inherits
       JOIN pg_class parent ON pg_inherits.inhparent = parent.oid
       JOIN pg_class child ON pg_inherits.inhrelid = child.oid
       JOIN pg_namespace nmsp_parent ON nmsp_parent.oid = parent.relnamespace
       JOIN pg_namespace nmsp_child ON nmsp_child.oid = child.relnamespace;

comment on view table_partition_list is 'The list of partitions for a table';


-- list of table and the partition
create or replace view table_no_partition as
select tableNamespace.nspname as schema_name,
       relations.relname      as name,
       relations.oid          as oid
from pg_catalog.pg_class relations,
     pg_namespace tableNamespace
where (relations.relkind = ANY (ARRAY ['r'::"char"
  , 'v'::"char"
  , 'f'::"char"
  , 'p'::"char"]))
  and relations.relnamespace = tableNamespace.oid
  and relations.relispartition = false; -- exclude table partitions

comment on view table_no_partition is 'A list of tables (without the table partition)';

-- list of table and the partition
create or replace view table_no_partition_column as
select tables.schema_name  as table_schema,
       tables.name         as table_name,
       tables.oid          as table_oid,
       tableColumn.attname as column_name
from pg_attribute tableColumn,
     table_no_partition tables
where tableColumn.attrelid = tables.oid
  and tableColumn.attnum > 0 -- only columns
;

comment on view table_no_partition_column is 'The list of columns for all tables that are not a partition';

-- list of table and the partition
create or replace view table_no_partition_column_pk as
select constraintTable.relname                                   as table_name,
       tableNamespace.nspname::information_schema.sql_identifier as table_schema,
       indexColumn.attname as column_name
from pg_constraint constrain,
     pg_class constraintTable,
     pg_class constraintIndex,
     pg_namespace tableNamespace,
     pg_attribute indexColumn
where constrain.contype = 'p'                                          -- primary key constraint
  and constrain.conrelid = constraintTable.oid
  and constraintTable.relkind = ANY (ARRAY ['r'::"char", 'p'::"char"]) -- normal table or partitioned table
  and constrain.conindid = constraintIndex.oid
  and constraintTable.relnamespace = tableNamespace.oid
  and constraintIndex.oid = indexColumn.attrelid
  AND NOT constraintTable.relispartition                               -- exclude child partitions
  and tableNamespace.nspname = 'cs_realms'                             -- postgres have also table with realm_id and id
;
comment on view table_no_partition_column_pk is 'The list of primary columns for all tables that are not a partition';


create or replace view realm_primary_tables as
with realmPrimaryKeyColumnsCount as
       (select columns.table_name   as table_name,
               columns.table_schema as table_schema,
               count(1)             as column_count
        from table_no_partition_column_pk columns
        where (columns.column_name = 'realm_id' or columns.column_name like '%_id')
        group by columns.table_name, columns.table_schema)
select table_name, table_schema
from realmPrimaryKeyColumnsCount
where column_count = 2;

comment on view realm_primary_tables is 'The list of primary tables for the realm data model.

A realm primary table is a table with the columns realm_id and id as primary key.

ie the table is split by realm, meaning the primary object is scoped to a realm.

The view check the column index that supports the primary key.';


-- the partition by ranges for realm_list_registration is created when a list is created by the list provider
CREATE OR REPLACE FUNCTION create_partitions_for_realm_list_type(realmId bigint, targetSchema text)
  RETURNS void
  LANGUAGE plpgsql
AS
$$
DECLARE
  cursor_table_name            text;
  partition_name        TEXT;
  old_search_path       TEXT;
  actual_partition_name TEXT;
BEGIN

  select current_setting('search_path') into old_search_path;
  PERFORM set_config('search_path', targetSchema, true);

  FOR cursor_table_name IN
    select table_name
    from table_partition_definition
    where schema = schema
      and num_columns = 1
      and column_name like '%_realm_id'
      and partition_strategy = 'list'
    LOOP

      partition_name := cursor_table_name || '_realm_' || realmId::text;
      SELECT relname into actual_partition_name FROM pg_class WHERE relname = partition_name;
      IF actual_partition_name IS NULL THEN
        RAISE NOTICE 'A partition has been created %',partition_name;
        EXECUTE 'CREATE TABLE '
                  || partition_name || ' partition of ' || cursor_table_name
                  || ' for values in (' || realmId || ')';
      END IF;

    END LOOP;
  PERFORM set_config('search_path', old_search_path, true);

END;
$$;

comment on function create_partitions_for_realm_list_type is '
Create the realm partitions on other partitioned table when a realm is inserted.

https://www.postgresql.org/docs/current/ddl-partitioning.html#DDL-PARTITIONING-CONSTRAINT-EXCLUSION
BEFORE ROW triggers on INSERT cannot change which partition is the final destination for a new row.
therefore we need to create the partition before';

-- https://www.postgresql.org/docs/current/ddl-partitioning.html#DDL-PARTITIONING-CONSTRAINT-EXCLUSION
-- BEFORE ROW triggers on INSERT cannot change which partition is the final destination for a new row.
-- therefore we need to create the partition before
CREATE OR REPLACE FUNCTION create_partitions_for_realm(realmId bigint, targetSchema text)
  RETURNS Void
  LANGUAGE plpgsql
AS
$$
DECLARE
  old_search_path TEXT;
BEGIN

  select current_setting('search_path') into old_search_path;
  PERFORM set_config('search_path', targetSchema, true);

  perform create_partitions_for_realm_list_type(realmId, targetSchema);

  PERFORM set_config('search_path', old_search_path, true);
END;
$$;


-- https://www.postgresql.org/docs/current/ddl-partitioning.html#DDL-PARTITIONING-CONSTRAINT-EXCLUSION
-- BEFORE ROW triggers on INSERT cannot change which partition is the final destination for a new row.
-- therefore we need to create the partition before
CREATE
  OR REPLACE FUNCTION create_partitions_for_realm_on_insert()
  RETURNS trigger
  LANGUAGE plpgsql
AS
$$
DECLARE
  old_search_path TEXT;
BEGIN

  select current_setting('search_path') into old_search_path;
  PERFORM set_config('search_path', TG_TABLE_SCHEMA, true);

  -- perform: execute the function and discard the results (not a SELECT)
  perform create_partitions_for_realm(NEW.realm_id, TG_TABLE_SCHEMA);

  PERFORM set_config('search_path', old_search_path, true);

  RETURN NEW;

END;
$$;


-- set the creation and modification date
CREATE OR REPLACE FUNCTION set_sysdate_on_insert_update() RETURNS trigger
  LANGUAGE plpgsql
AS
$$
DECLARE
  new_jsonb          jsonb;
  stack              text;
  executing_function text;
BEGIN
  -- get the function name
  GET DIAGNOSTICS stack = PG_CONTEXT;
  executing_function = substring(stack from 'function (.*?) line');

  new_jsonb := to_jsonb(NEW);
  if (TG_OP = 'INSERT') THEN
    -- ? operator: Does the text string exist as a top-level key
    -- https://www.postgresql.org/docs/current/functions-json.html#FUNCTIONS-JSONB-OP-TABLE
    IF new_jsonb ? 'creation_time' THEN
      NEW.creation_time = now();
    ELSE
      RAISE WARNING '%: the creation time column does not exist on the table %',executing_function, TG_TABLE_SCHEMA || '.' || TG_TABLE_NAME;
    end if;
  ELSE
    IF (TG_OP = 'UPDATE') THEN
      IF new_jsonb ? 'modification_time' THEN
        NEW.modification_time = now();
      ELSE
        RAISE WARNING '%: the modification time column does not exist on the table %',executing_function, TG_TABLE_SCHEMA || '.' || TG_TABLE_NAME;
      end if;
    end if;
  end if;
  RETURN NEW;
END;
$$;

-- Check that the user is in the realm 1 (ie our realm)
CREATE OR REPLACE FUNCTION realm_check_orga_user_id()
  RETURNS TRIGGER AS $$
DECLARE
  old_search_path TEXT;
BEGIN
  select current_setting('search_path') into old_search_path;
  PERFORM set_config('search_path', TG_TABLE_SCHEMA, true);

  IF EXISTS (
    SELECT 1
    FROM realm_user
    WHERE realm_user.user_id = NEW.ORGA_USER_REALM_USER_ID
      and realm_user.user_realm_id = 1
  ) THEN
    PERFORM set_config('search_path', old_search_path, true);
    RETURN NEW;
  ELSE
    PERFORM set_config('search_path', old_search_path, true);
    RAISE EXCEPTION 'The user id (%) is not a realm user', NEW.realm_orga_user_id;
  END IF;
END;
$$ LANGUAGE plpgsql;


CREATE OR REPLACE TRIGGER realm_check_orga_user_id
  BEFORE INSERT OR UPDATE ON organization_user
  FOR EACH ROW
EXECUTE FUNCTION realm_check_orga_user_id();

-- create table list partition on realm insertion
CREATE OR REPLACE TRIGGER create_realm_partitions_on_insert
  AFTER INSERT
  ON realm
  FOR EACH ROW
EXECUTE PROCEDURE create_partitions_for_realm_on_insert();
