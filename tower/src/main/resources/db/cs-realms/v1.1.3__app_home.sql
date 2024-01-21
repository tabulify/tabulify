-- add app_handle, app_uri is now a parameter
update cs_realms.realm_app SET app_uri = LEFT(app_uri, 30);
ALTER TABLE cs_realms.realm_app ALTER COLUMN app_uri TYPE VARCHAR(30);
alter table cs_realms.realm_app rename column app_uri to app_handle;
alter table cs_realms.realm_app ADD column app_uri VARCHAR(255) NULL UNIQUE;
