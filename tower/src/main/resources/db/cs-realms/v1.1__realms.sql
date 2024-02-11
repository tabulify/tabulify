create  table  organization
(
  ORGA_ID                BIGSERIAL                   NOT NULL PRIMARY KEY,
  ORGA_NAME              varchar(255)                NOT NULL,
  ORGA_DATA              jsonb                       NOT NULL,
  ORGA_CREATION_TIME     TIMESTAMP WITHOUT TIME ZONE NOT NULL,
  ORGA_MODIFICATION_TIME TIMESTAMP WITHOUT TIME ZONE NULL
);
comment on table organization is 'An organization is the owner of realms and has users.';

create table organization_role
(
  ORGA_ROLE_ID                BIGSERIAL                   NOT NULL PRIMARY KEY,
  ORGA_ROLE_NAME              varchar(255)                NOT NULL UNIQUE,
  ORGA_ROLE_DATA              jsonb                       NOT NULL,
  ORGA_ROLE_CREATION_TIME     TIMESTAMP WITHOUT TIME ZONE NOT NULL,
  ORGA_ROLE_MODIFICATION_TIME TIMESTAMP WITHOUT TIME ZONE NULL
);
comment on table organization_role is 'The role for the users (owner, billing, ..). It gives permission to user on the organization level.';


-- Add the organization id to the realm
alter table realm
  add column if not exists REALM_ORGA_ID BIGINT references organization (ORGA_ID);
comment on column realm.REALM_ORGA_ID is 'The organization that owns this realm';

-- Add the contact user id
alter table realm
  add column if not exists REALM_OWNER_USER_ID BIGINT;
comment on column realm.realm_owner_user_id is 'The owner, ie contact identifier (the user id of the eraldy realm, used in reporting for support when people have problem with a connection for instance)';

alter table realm
  add column if not exists REALM_DEFAULT_APP_ID BIGINT;
comment on column realm.REALM_DEFAULT_APP_ID is 'The default app (The app that is shown or used by default)';
ALTER TABLE realm
  ADD CONSTRAINT realm_default_app_fkey FOREIGN KEY (REALM_ORGA_ID, REALM_DEFAULT_APP_ID) REFERENCES realm_app;

create table organization_user
(
  ORGA_USER_USER_ID           BIGINT                      NOT NULL,
  ORGA_USER_ORGA_ID           BIGINT                      NOT NULL REFERENCES organization (ORGA_ID),
  ORGA_USER_ORGA_ROLE_ID      BIGINT                      NOT NULL REFERENCES organization_role (orga_role_id),
  ORGA_USER_CREATION_TIME     TIMESTAMP WITHOUT TIME ZONE NOT NULL,
  ORGA_USER_MODIFICATION_TIME TIMESTAMP WITHOUT TIME ZONE NULL,
  CONSTRAINT organization_user_pk PRIMARY KEY (ORGA_USER_ORGA_ID, ORGA_USER_USER_ID)
);
comment on table organization_user is 'The users of the organization';
comment on column organization_user.ORGA_USER_USER_ID is 'The user id of the realm id 1. It''s not a sequence';



-- update and not null contact user id
update realm
set REALM_ORGA_ID = 1
where REALM_ORGA_ID is null;
alter table realm
  ALTER
    column REALM_ORGA_ID
    set NOT NUll;
-- update and not null contact user id
update realm
set realm_owner_user_id=1
where realm_owner_user_id is null;
alter table realm
  ALTER
    column realm_owner_user_id
    set NOT NULL;
ALTER TABLE realm
  ADD CONSTRAINT realm_organization_owner_user_fkey FOREIGN KEY (REALM_ORGA_ID, realm_owner_user_id) REFERENCES organization_user;
-- update realm default app
update realm
set REALM_DEFAULT_APP_ID = 1
where realm.realm_id = 1;

-- Index
create index IF NOT EXISTS realm_realm_orga_id_idx on realm (REALM_ORGA_ID);

-- Add the first analytics columns
alter table realm
  add column if not exists REALM_ANALYTICS JSONB;
comment on column realm.REALM_ANALYTICS is 'The analytics data for the realm';
alter table realm
  add column if not exists REALM_ANALYTICS_TIME TIMESTAMP WITHOUT TIME ZONE NULL;

-- Add the second analytics columns
alter table realm_user
  add column if not exists USER_ANALYTICS JSONB;
comment on column realm_user.USER_ANALYTICS is 'The analytics data for the user';
alter table realm_user
  add column if not exists USER_ANALYTICS_TIME TIMESTAMP WITHOUT TIME ZONE NULL;

-- Add the third analytics columns
alter table realm_app
  add column if not exists APP_ANALYTICS JSONB;
comment on column realm_app.APP_ANALYTICS is 'The analytics data for the app';
alter table realm_app
  add column if not exists APP_ANALYTICS_TIME TIMESTAMP WITHOUT TIME ZONE NULL;

alter table realm_list
  add column if not exists LIST_ANALYTICS JSONB;
comment on column realm_list.LIST_ANALYTICS is 'The analytics data for the list';
alter table realm_list
  add column if not exists LIST_ANALYTICS_TIME TIMESTAMP WITHOUT TIME ZONE NULL;
