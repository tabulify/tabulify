-- Create orga
create table organization
(
  ORGA_ID                BIGSERIAL                   NOT NULL PRIMARY KEY,
  ORGA_NAME              varchar(255)                NOT NULL,
  ORGA_HANDLE            varchar(32)                 NULL UNIQUE,
  ORGA_OWNER_USER_ID     BIGINT                      NOT NULL UNIQUE,
  ORGA_OWNER_REALM_ID    BIGINT                      NOT NULL CHECK (ORGA_OWNER_REALM_ID = 1),
  ORGA_CREATION_TIME     TIMESTAMP WITHOUT TIME ZONE NOT NULL,
  ORGA_MODIFICATION_TIME TIMESTAMP WITHOUT TIME ZONE NULL
);
comment on table organization is 'An organization is the owner of realms, app and list and has users. They are the customer of our interact app.';

create table organization_role
(
  ORGA_ROLE_ID                BIGSERIAL                   NOT NULL PRIMARY KEY,
  ORGA_ROLE_NAME              varchar(255)                NOT NULL,
  ORGA_ROLE_CREATION_TIME     TIMESTAMP WITHOUT TIME ZONE NOT NULL,
  ORGA_ROLE_MODIFICATION_TIME TIMESTAMP WITHOUT TIME ZONE NULL
);
comment on table organization_role is 'The role for the users (owner, billing, ..). It gives permission to user on the organization level.';

create table organization_user
(
  ORGA_USER_ORGA_ID           BIGINT                      NOT NULL REFERENCES organization (ORGA_ID),
  ORGA_USER_USER_ID           BIGINT                      NOT NULL,
  ORGA_USER_REALM_ID          BIGINT                      NOT NULL DEFAULT 1 CHECK (ORGA_USER_REALM_ID = 1),
  ORGA_USER_ROLE_ID           BIGINT                      NOT NULL REFERENCES organization_role (orga_role_id),
  ORGA_USER_CREATION_TIME     TIMESTAMP WITHOUT TIME ZONE NOT NULL,
  ORGA_USER_MODIFICATION_TIME TIMESTAMP WITHOUT TIME ZONE NULL,
  CONSTRAINT organization_user_pk PRIMARY KEY (ORGA_USER_ORGA_ID, ORGA_USER_USER_ID)
);
comment on table organization_user is 'The users of the organization';
comment on column organization_user.ORGA_USER_USER_ID is 'The user id of the realm id 1. It''s not a sequence';
comment on column organization_user.ORGA_USER_REALM_ID is 'The Eraldy realm (1). The column is here to a foreign key to the realm user table';

-- a list of all realm
create table IF NOT EXISTS realm
(
  REALM_ID                BIGSERIAL                   NOT NULL PRIMARY KEY,
  REALM_NAME              varchar(50)                 NOT NULL,
  REALM_HANDLE            varchar(32)                 NULL UNIQUE,
  REALM_ORGA_ID           BIGINT                      NOT NULL REFERENCES organization,
  REALM_OWNER_USER_ID     BIGINT                      NOT NULL,
  REALM_USER_COUNT        BIGINT                      NOT NULL DEFAULT 0,
  REALM_USER_IN_COUNT     BIGINT                      NOT NULL DEFAULT 0,
  REALM_APP_COUNT         INTEGER                     NOT NULL DEFAULT 0,
  REALM_LIST_COUNT        INTEGER                     NOT NULL DEFAULT 0,
  REALM_CREATION_TIME     TIMESTAMP WITHOUT TIME ZONE NOT NULL,
  REALM_MODIFICATION_TIME TIMESTAMP WITHOUT TIME ZONE NULL
);
comment on table realm is 'The list of realms';
comment on column realm.REALM_HANDLE is 'A short unique name identifier used as named identifier';
comment on column realm.REALM_ORGA_ID is 'The organization that owns this realm';
comment on column realm.REALM_OWNER_USER_ID is 'The owner, ie contact identifier (the user id of the eraldy realm, used in reporting for support when people have problem with a connection for instance)';
-- Defer for initial insertion of the first eraldy org, realm and app
ALTER TABLE realm
  ADD FOREIGN KEY (REALM_ORGA_ID, REALM_OWNER_USER_ID) REFERENCES organization_user DEFERRABLE INITIALLY IMMEDIATE;
-- realm orga unique key to be able to create foreign key
alter table realm
  add unique (realm_id, realm_orga_id);

-- note that because user is a reserved word, we use quote it
create table IF NOT EXISTS realm_user
(
  USER_REALM_ID          BIGINT                      NOT NULL references "realm" (REALM_ID),
  USER_ID                BIGINT                      NOT NULL,
  USER_EMAIL_ADDRESS     varchar(255)                NOT NULL,
  USER_PASSWORD          varchar(255),
  USER_STATUS_CODE       integer                     NOT NULL,
  USER_STATUS_MESSAGE    VARCHAR(50),
  USER_GIVEN_NAME        VARCHAR(50)                 NOT NULL,
  USER_FAMILY_NAME       VARCHAR(50)                 NULL,
  USER_TITLE             VARCHAR(255)                NULL,
  USER_BIO               VARCHAR(255)                NULL,
  USER_LOCATION          VARCHAR(50),
  USER_AVATAR            VARCHAR(255),
  USER_WEBSITE           VARCHAR(255),
  USER_TIME_ZONE         VARCHAR(32),
  USER_LAST_ACTIVE_TIME  TIMESTAMP WITHOUT TIME ZONE NOT NULL,
  USER_CREATION_TIME     TIMESTAMP WITHOUT TIME ZONE NOT NULL,
  USER_MODIFICATION_TIME TIMESTAMP WITHOUT TIME ZONE NULL
) PARTITION BY LIST (USER_REALM_ID);
alter table realm_user
  add primary key (USER_REALM_ID, USER_ID);
alter table realm_user
  add unique (USER_REALM_ID, USER_EMAIL_ADDRESS);
comment on table realm_user is 'A list of users (a user is an identity in a realm)';
comment on column realm_user.USER_EMAIL_ADDRESS is 'the user email address';
comment on column realm_user.USER_STATUS_CODE is 'The status code of the user';
comment on column realm_user.USER_STATUS_MESSAGE is 'A message explicative for the status code';
comment on column realm_user.USER_PASSWORD is 'a non-reversible hash of the password';
comment on column realm_user.USER_TIME_ZONE is 'The IANA Time Zone';
comment on column realm_user.USER_TITLE is 'Job title';
comment on column realm_user.USER_BIO is 'A text explicatif';
comment on column realm_user.USER_WEBSITE is 'An URL';
comment on column realm_user.USER_AVATAR is 'An URL to a avatar';
comment on column realm_user.USER_LAST_ACTIVE_TIME is 'The last active time (Updated at login time at least)';

-- Constraint
-- Defer for initial insertion of the first eraldy org, realm and app
ALTER TABLE organization
  ADD FOREIGN KEY (ORGA_OWNER_REALM_ID, ORGA_OWNER_USER_ID) REFERENCES realm_user DEFERRABLE INITIALLY IMMEDIATE;
ALTER TABLE organization_user
  ADD FOREIGN KEY (ORGA_USER_REALM_ID, ORGA_USER_USER_ID) REFERENCES realm_user DEFERRABLE INITIALLY IMMEDIATE;

CREATE TABLE realm_sequence
(
  SEQUENCE_REALM_ID          bigint                      NOT NULL references realm (REALM_ID),
  SEQUENCE_TABLE_NAME        varchar(50)                 NOT NULL,
  SEQUENCE_LAST_ID           bigint                      NOT NULL default 0,
  SEQUENCE_CREATION_TIME     TIMESTAMP WITHOUT TIME ZONE NOT NULL,
  SEQUENCE_MODIFICATION_TIME TIMESTAMP WITHOUT TIME ZONE NULL
);
-- Defer for initial insertion of the first eraldy org, realm and app
alter table realm_sequence
  add primary key (SEQUENCE_REALM_ID, SEQUENCE_TABLE_NAME) DEFERRABLE INITIALLY IMMEDIATE;
comment on table realm_sequence is 'The last value of each id for the realm tables';


-- the app holds app information such as branding data such as name, email, logo
create table IF NOT EXISTS realm_app
(
  APP_REALM_ID          BIGINT                      NOT NULL references "realm" (REALM_ID),
  APP_ID                BIGINT                      NOT NULL,
  APP_ORGA_ID           BIGINT                      NOT NULL,
  APP_OWNER_USER_ID     BIGINT                      NOT NULL,
  APP_NAME              varchar(50)                 NOT NULL,
  APP_HANDLE            varchar(32)                 NULL,
  APP_HOME              varchar(255)                NULL,
  APP_URI               varchar(255)                NULL,
  APP_LIST_COUNT        INTEGER                     NULL,
  APP_CREATION_TIME     TIMESTAMP WITHOUT TIME ZONE NOT NULL,
  APP_MODIFICATION_TIME TIMESTAMP WITHOUT TIME ZONE NULL
);
alter table realm_app
  add primary key (APP_REALM_ID, APP_ID);
alter table realm_app
  add unique (APP_REALM_ID, APP_HANDLE) DEFERRABLE INITIALLY IMMEDIATE;
-- Defer for initial insertion of the first eraldy org, realm and app
alter table realm_app
  add foreign key (APP_ORGA_ID, APP_OWNER_USER_ID) REFERENCES organization_user (ORGA_USER_ORGA_ID, ORGA_USER_USER_ID) DEFERRABLE INITIALLY IMMEDIATE;
alter table realm_app
  add foreign key (app_realm_id, app_orga_id) REFERENCES realm (realm_id, realm_orga_id) DEFERRABLE INITIALLY IMMEDIATE;
comment on table realm_app is 'A list of apps for the realm';
comment on column realm_app.APP_ID is 'The app id';
comment on column realm_app.APP_URI is 'The uri of the app (unique on the realm). It holds the authentication scope: https://www.rfc-editor.org/rfc/rfc7617#section-2.2)';
comment on column realm_app.APP_OWNER_USER_ID is 'The organizational user used for brand communication by default (user in realm 1 that belongs to the organization of the realm)';


-- A list of list
create table IF NOT EXISTS realm_list
(
  LIST_REALM_ID          BIGINT                      NOT NULL references "realm" (REALM_ID),
  LIST_ID                BIGINT                      NOT NULL,
  LIST_ORGA_ID           BIGINT                      NOT NULL,
  LIST_OWNER_USER_ID     BIGINT                      NOT NULL,
  LIST_APP_ID            BIGINT                      NOT NULL,
  LIST_NAME              varchar(50)                 NOT NULL,
  LIST_HANDLE            varchar(32)                 NULL,
  LIST_TITLE             varchar(255)                NULL,
  LIST_USER_COUNT        BIGINT                      NULL,
  LIST_USER_IN_COUNT     BIGINT                      NULL,
  LIST_MAILING_COUNT     BIGINT                      NULL,
  LIST_CREATION_TIME     TIMESTAMP WITHOUT TIME ZONE NOT NULL,
  LIST_MODIFICATION_TIME TIMESTAMP WITHOUT TIME ZONE NULL
);
alter table realm_list
  add primary key (LIST_REALM_ID, LIST_ID);
alter table realm_list
  add foreign key (LIST_ORGA_ID, LIST_OWNER_USER_ID) REFERENCES organization_user (ORGA_USER_ORGA_ID, ORGA_USER_USER_ID);
alter table realm_list
  add foreign key (LIST_REALM_ID, LIST_APP_ID) REFERENCES realm_app (APP_REALM_ID, APP_ID);
alter table realm_list
  add unique (LIST_REALM_ID, LIST_HANDLE);
alter table realm_list
  add foreign key (list_realm_id, list_orga_id) REFERENCES realm (realm_id, realm_orga_id);

comment on table realm_list is 'A list of users';
comment on column realm_list.LIST_HANDLE is 'An unique handle that permits to update the list without knowing the id';
comment on column realm_list.LIST_APP_ID is 'The app id (to get the default owner and the design info such as logo, color, ...)';
comment on column realm_list.LIST_ORGA_ID is 'The organization id (added for foreign constraint on the user, the value should never change)';
comment on column realm_list.LIST_OWNER_USER_ID is 'The owner of the list (The organizational user used for brand communication by default (user in realm 1 that belongs to the organization of the realm)';
comment on column realm_list.list_user_count is 'An analytics - the count of users on the list';
comment on column realm_list.list_user_in_count is 'An analytics - the count of users that are still in on the list';
comment on column realm_list.list_mailing_count is 'An analytics - the count of mailing for the list';
comment on column realm_list.list_name is 'A short name for the list';
comment on column realm_list.list_title is 'A title for the list';
comment on column realm_list.list_realm_id is 'The realm id';
comment on column realm_list.list_id is 'The list id (unique in the realm)';

-- a list of users subscribed to this registration
create table IF NOT EXISTS realm_list_user
(
  LIST_USER_REALM_ID                    BIGINT                      NOT NULL,
  LIST_USER_LIST_ID                     BIGINT                      NOT NULL,
  LIST_USER_USER_ID                     BIGINT                      NOT NULL,
  LIST_USER_STATUS_CODE                 INTEGER                     NOT NULL,
  LIST_USER_STATUS_MESSAGE              VARCHAR(255)                NOT NULL,
  LIST_USER_IN_SOURCE_ID                INTEGER                     NOT NULL,
  LIST_USER_IN_OPT_IN_ORIGIN            VARCHAR(255)                NULL,
  LIST_USER_IN_OPT_IN_IP                inet,
  LIST_USER_IN_OPT_IN_TIME              TIMESTAMP WITHOUT TIME ZONE,
  LIST_USER_IN_OPT_IN_CONFIRMATION_IP   inet,
  LIST_USER_IN_OPT_IN_CONFIRMATION_TIME TIMESTAMP WITHOUT TIME ZONE,
  LIST_USER_OUT_OPT_OUT_TIME            TIMESTAMP WITHOUT TIME ZONE,
  LIST_USER_CREATION_TIME               TIMESTAMP WITHOUT TIME ZONE NOT NULL,
  LIST_USER_MODIFICATION_TIME           TIMESTAMP WITHOUT TIME ZONE NULL,
  PRIMARY KEY (LIST_USER_REALM_ID, LIST_USER_LIST_ID, LIST_USER_USER_ID)
) PARTITION BY RANGE (LIST_USER_REALM_ID, LIST_USER_LIST_ID);
alter table realm_list_user
  add foreign key (LIST_USER_REALM_ID, LIST_USER_LIST_ID) REFERENCES realm_list (LIST_REALM_ID, LIST_ID);
alter table realm_list_user
  add foreign key (LIST_USER_REALM_ID, LIST_USER_USER_ID) REFERENCES realm_user (USER_REALM_ID, USER_ID);
comment on table realm_list_user is 'A list of users registered to the list. The table does not have any id as it can be become huge.';
comment on column realm_list_user.LIST_USER_USER_ID is 'The subscriber';
comment on column realm_list_user.LIST_USER_STATUS_CODE is 'Registered, unregistered, cleaned (bounced)';


-- represents a mailing (sending an email to a list of users)
create table IF NOT EXISTS realm_mailing
(
  MAILING_REALM_ID                BIGINT                      NOT NULL references "realm" (REALM_ID),
  MAILING_ID                      BIGINT                      NOT NULL,
  MAILING_NAME                    VARCHAR(50)                 NOT NULL,
  MAILING_EMAIL_RCPT_LIST_ID      BIGINT                      NOT NULL,
  MAILING_EMAIL_SUBJECT           VARCHAR(150)                NULL,
  MAILING_EMAIL_PREVIEW           TEXT                        NULL,
  MAILING_EMAIL_BODY              TEXT                        NULL,
  MAILING_EMAIL_AUTHOR_USER_ID    BIGINT                      NOT NULL,
  mailing_email_language          varchar(2), -- not char otherwise we get 2 empty spaces and not null
  MAILING_ORGA_ID                 BIGINT                      NOT NULL,
  MAILING_STATUS                  INT                         NOT NULL,
  MAILING_JOB_LAST_EXECUTION_TIME TIMESTAMP WITHOUT TIME ZONE NULL,
  MAILING_JOB_NEXT_EXECUTION_TIME TIMESTAMP WITHOUT TIME ZONE NULL,
  MAILING_ITEM_COUNT              BIGINT                      NULL,
  MAILING_ITEM_SUCCESS_COUNT      BIGINT                      NULL,
  MAILING_ITEM_EXECUTION_COUNT    BIGINT                      NULL,
  MAILING_CREATION_TIME           TIMESTAMP WITHOUT TIME ZONE NOT NULL,
  MAILING_MODIFICATION_TIME       TIMESTAMP WITHOUT TIME ZONE NULL
);

alter table realm_mailing
  add primary key (MAILING_REALM_ID, MAILING_ID);
alter table realm_mailing
  add foreign key (MAILING_REALM_ID, MAILING_EMAIL_RCPT_LIST_ID) REFERENCES realm_list (LIST_REALM_ID, LIST_ID);
alter table realm_mailing
  add foreign key (MAILING_ORGA_ID, MAILING_EMAIL_AUTHOR_USER_ID) REFERENCES organization_user (orga_user_orga_id, orga_user_user_id);
alter table realm_mailing
  add foreign key (MAILING_REALM_ID, MAILING_ORGA_ID) REFERENCES realm (realm_id, realm_orga_id);
comment on table realm_mailing is 'A mailing (the sending of an email to users)';
comment on column realm_mailing.MAILING_REALM_ID is 'The realm id of the mailing';
comment on column realm_mailing.MAILING_ID is 'The unique sequential id on the realm';
comment on column realm_mailing.MAILING_NAME is 'The name of the mailing';
comment on column realm_mailing.MAILING_EMAIL_RCPT_LIST_ID is 'The list of recipients';
comment on column realm_mailing.MAILING_EMAIL_SUBJECT is 'The email subject';
comment on column realm_mailing.MAILING_EMAIL_PREVIEW is 'The email preview';
comment on column realm_mailing.MAILING_EMAIL_BODY is 'The email body';
comment on column realm_mailing.MAILING_EMAIL_AUTHOR_USER_ID is 'The author of the email (An organizational user, the id of the user in the realm 1)';
comment on column realm_mailing.MAILING_STATUS is 'The status (draft, send, scheduled, ...)';
comment on column realm_mailing.mailing_job_last_execution_time is 'The last time a job was executed for this mailing';
comment on column realm_mailing.mailing_job_next_execution_time is 'The next time that a job will execute for this mailing';
comment on column realm_mailing.mailing_item_count is 'The number of emails (ie users/email/row) for this mailing to send';
comment on column realm_mailing.mailing_item_success_count is 'The number of email send successfully (ie successful smtp transaction)';
comment on column realm_mailing.mailing_item_execution_count is 'The number of smtp transfer execution (successful or not)';
comment on column realm_mailing.mailing_email_language is 'The language of the email (used in assistive technology)';
