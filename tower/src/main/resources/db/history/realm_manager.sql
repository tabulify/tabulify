
-- Not finished
-- adding managers users to be able to manage a realm
-- add owners
create table IF NOT EXISTS realm_owner
(
  REALM_OWNER_REALM_ID          BIGSERIAL                   NOT NULL references "realm" (REALM_ID),
  REALM_OWNER_EMAIL             varchar(255)                NOT NULL,
  REALM_OWNER_CREATION_TIME     TIMESTAMP WITHOUT TIME ZONE NOT NULL,
  REALM_OWNER_MODIFICATION_TIME TIMESTAMP WITHOUT TIME ZONE NULL
);
alter table realm_owner
  add primary key (REALM_OWNER_REALM_ID, REALM_OWNER_EMAIL);
comment on table realm_owner is 'The list of realm owner';
comment on column realm_owner.REALM_OWNER_EMAIL is 'The user identifier (the user email and not the user sequence as we may split the realms into different database)';
