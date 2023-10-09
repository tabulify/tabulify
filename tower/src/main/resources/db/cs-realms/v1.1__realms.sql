create  table  organization
(
  ORGA_ID                BIGSERIAL                   NOT NULL PRIMARY KEY,
  ORGA_NAME              varchar(255)                NOT NULL,
  ORGA_DATA              jsonb                       NOT NULL,
  ORGA_CREATION_TIME     TIMESTAMP WITHOUT TIME ZONE NOT NULL,
  ORGA_MODIFICATION_TIME TIMESTAMP WITHOUT TIME ZONE NULL
);
comment on table organization is 'An organization is the owner of realms and has users.';

-- Create the Eraldy Organization
INSERT INTO organization(ORGA_ID,
                         ORGA_NAME,
                         ORGA_DATA,
                         ORGA_CREATION_TIME)
values (1,
        'eraldy',
        '{}'::jsonb,
        now())
ON conflict (ORGA_ID)
  DO NOTHING;

create table organization_role
(
  ORGA_ROLE_ID                BIGSERIAL                   NOT NULL PRIMARY KEY,
  ORGA_ROLE_NAME              varchar(255)                NOT NULL UNIQUE,
  ORGA_ROLE_DATA              jsonb                       NOT NULL,
  ORGA_ROLE_CREATION_TIME     TIMESTAMP WITHOUT TIME ZONE NOT NULL,
  ORGA_ROLE_MODIFICATION_TIME TIMESTAMP WITHOUT TIME ZONE NULL
);
comment on table organization_role is 'The role for the users (owner, billing, ..). It gives permission to user on the organization level.';

-- Create the first role, the Owner Role
INSERT INTO organization_role(ORGA_ROLE_ID,
                              ORGA_ROLE_NAME,
                              ORGA_ROLE_DATA,
                              ORGA_ROLE_CREATION_TIME)
values (1,
        'Owner',
        '{}'::jsonb,
        now())
ON conflict (ORGA_ROLE_ID)
  DO NOTHING;


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

-- Create the Eraldy realm
INSERT INTO realm(realm_id,
                  realm_handle,
                  realm_data,
                  realm_creation_time)
values (1,
        'eraldy',
        '{}'::jsonb,
        now())
ON conflict (realm_id)
  DO NOTHING;


-- Create the Eraldy owner user
INSERT INTO realm_user(user_realm_id,
                       user_id,
                       user_email,
                       user_data,
                       user_creation_time)
values (1, 1, 'nico@eraldy.com', '{}'::jsonb, now())
ON conflict (user_realm_id, user_id)
  DO NOTHING;



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

-- Insert the Eraldy owner
INSERT INTO organization_user(ORGA_USER_ORGA_ID,
                              ORGA_USER_USER_ID,
                              ORGA_USER_ORGA_ROLE_ID,
                              ORGA_USER_CREATION_TIME)
values (1,
        1,
        1,
        now())
ON conflict (ORGA_USER_ORGA_ID,ORGA_USER_USER_ID)
  DO NOTHING;

-- creating the default app
INSERT INTO realm_app(app_realm_id,
                      app_id,
                      app_uri,
                      app_user_id,
                      app_data,
                      app_creation_time)
values (1,
        1,
        'eraldy.com',
        1,
        '{}'::jsonb,
        now())
ON conflict (app_realm_id, app_id)
  DO NOTHING;



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
