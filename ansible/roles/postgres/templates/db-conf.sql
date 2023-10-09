-- https://www.postgresql.org/docs/current/sql-createextension.html
create schema if not exists addons_hstore;

-- Install the hstore extension into the current database, placing its objects in schema addons:
CREATE EXTENSION if not exists hstore SCHEMA addons_hstore;



