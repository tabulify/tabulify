-- realm orga unique key
alter table realm
  add unique (realm_id, realm_orga_id);

--
-- owner user is not a realm user but on org user
-- ---------------------------------------------------
-- app user was a realm user, not an organization user
comment on column realm_app.APP_USER_ID is 'The organizational user used for brand communication by default (user in realm 1 that belongs to the organization of the realm)';
-- constraint is on the app level (for now)
alter table realm_app drop constraint realm_app_app_realm_id_app_user_id_fkey;
alter table realm_app add column app_orga_id BIGINT;
update realm_app set app_orga_id = 1;
alter table realm_app alter column app_orga_id set not null;
alter table realm_app
  add foreign key (app_orga_id, app_user_id) REFERENCES organization_user (orga_user_orga_id, orga_user_user_id);
alter table realm_app
  rename column app_user_id to app_owner_user_id;
alter table realm_app
  add foreign key (app_realm_id, app_orga_id) REFERENCES realm (realm_id, realm_orga_id);

-- Realm list refactoring
-- owner on app is not needed as you can have only one app for a list
alter table cs_realms.realm_list
  rename column list_owner_app_id to list_app_id;
-- orga user list is not a realm user but on org user
comment on column realm_list.LIST_OWNER_USER_ID is 'The owner of the list (The organizational user used for brand communication by default (user in realm 1 that belongs to the organization of the realm)';
alter table realm_list drop constraint realm_list_list_realm_id_list_owner_user_id_fkey;
alter table realm_list add column list_orga_id BIGINT;
comment on column realm_list.list_orga_id is 'The organization id (added for foreign constraint on the user, the value should never change)';
update realm_list set list_orga_id = 1 where list_owner_user_id is not null;
alter table realm_list
  add foreign key (list_orga_id, list_owner_user_id) REFERENCES organization_user (orga_user_orga_id, orga_user_user_id);
alter table realm_list
  add foreign key (list_realm_id, list_orga_id) REFERENCES realm (realm_id, realm_orga_id);

-- the file system drive
create table IF NOT EXISTS realm_drive
(
  DRIVE_REALM_ID          BIGINT                    NOT NULL references "realm" (REALM_ID),
  DRIVE_ID                BIGINT                    NOT NULL,
  DRIVE_HANDLE            varchar(50)               NOT NULL,
  DRIVE_APP_ID            BIGINT                    NOT NULL,
  DRIVE_CREATION_TIME     TIMESTAMP WITHOUT TIME ZONE NOT NULL,
  DRIVE_MODIFICATION_TIME TIMESTAMP WITHOUT TIME ZONE NULL
);
alter table realm_drive
  add primary key (DRIVE_REALM_ID, DRIVE_ID);
alter table realm_drive
  add foreign key (DRIVE_REALM_ID, DRIVE_APP_ID) REFERENCES realm_app (APP_REALM_ID, APP_ID);
alter table realm_drive
  add unique (DRIVE_REALM_ID, DRIVE_HANDLE);
comment on table realm_drive is 'A file system drive';
comment on column realm_drive.DRIVE_REALM_ID is 'The realm id';
comment on column realm_drive.DRIVE_ID is 'The unique id in the realm';
comment on column realm_drive.DRIVE_HANDLE is 'A Handle (A DNS label)';
comment on column realm_drive.DRIVE_APP_ID is 'The App, this drive belongs';

-- represents a file
create table IF NOT EXISTS realm_file
(
  FILE_REALM_ID           BIGINT                    NOT NULL references "realm" (REALM_ID),
  FILE_ID                 BIGINT                    NOT NULL,
  FILE_DRIVE_ID           BIGINT                    NOT NULL,
  FILE_GUID               varchar(50)               NOT NULL,
  FILE_NAME               varchar(50)               NOT NULL,
  FILE_PARENT_ID          BIGINT                    NULL,
  FILE_TYPE               INT                       NOT NULL,
  FILE_MEDIA_TYPE         varchar(50)               NOT NULL,
  FILE_LOGICAL_TYPE       varchar(50)               NULL,
  FILE_TEXT               TEXT                      NULL,
  FILE_METADATA           JSONB                     NULL,
  FILE_CREATION_TIME      TIMESTAMP WITHOUT TIME ZONE NOT NULL,
  FILE_MODIFICATION_TIME  TIMESTAMP WITHOUT TIME ZONE NULL
);
alter table realm_file
  add primary key (FILE_REALM_ID, FILE_ID);
alter table realm_file
  add foreign key (FILE_REALM_ID, FILE_DRIVE_ID) REFERENCES realm_drive (DRIVE_REALM_ID, DRIVE_ID);
alter table realm_file
  add foreign key (FILE_REALM_ID, FILE_PARENT_ID) REFERENCES realm_file (FILE_REALM_ID, FILE_ID);
alter table realm_drive
  add unique (DRIVE_REALM_ID, DRIVE_HANDLE);
comment on table realm_file is 'A file (that keeps its identifier for life, not its path)';
comment on column realm_file.FILE_REALM_ID is 'The realm id of the file';
comment on column realm_file.FILE_ID is 'The unique sequential id on the realm';
comment on column realm_file.FILE_GUID is 'An textual guid unique on the realm without any prefix (Used to sync with an external app that may be our mobile app or any third party app)';
comment on column realm_file.FILE_NAME is 'The name of the file with or without extension (ie employee.csv)';
comment on column realm_file.FILE_TYPE is 'The type of file (0: directory, 1 regular file)';
comment on column realm_file.FILE_MEDIA_TYPE is 'The media type (the structure of the file so that we can create an AST). ie text/csv, text/json';
comment on column realm_file.FILE_LOGICAL_TYPE is 'A logical type to define the logical use and type of metadata (Example: email as json, eml, ... or xml may be a full document or a fragment, may contain raw data or ui description)';
comment on column realm_file.FILE_TEXT is 'The content of text file (binary file such as images are not stored in the database)';
comment on column realm_file.FILE_METADATA is 'A json that holds extra metadata information. The type of metadata is given by the logical type';
comment on column realm_file.FILE_PARENT_ID is 'The parent file (if null, the root)';

-- represents a mailing (sending an email to a list of users)
create table IF NOT EXISTS realm_mailing
(
  MAILING_REALM_ID           BIGINT                    NOT NULL references "realm" (REALM_ID),
  MAILING_ID                 BIGINT                    NOT NULL,
  MAILING_NAME               VARCHAR(50)               NOT NULL,
  MAILING_RCPT_LIST_ID       BIGINT                    NOT NULL,
  MAILING_EMAIL_FILE_ID      BIGINT                    NULL,
  MAILING_ORGA_ID            BIGINT                    NOT NULL,
  MAILING_AUTHOR_USER_ID     BIGINT                    NOT NULL,
  MAILING_STATUS             INT                       NOT NULL,
  MAILING_CREATION_TIME      TIMESTAMP WITHOUT TIME ZONE NOT NULL,
  MAILING_MODIFICATION_TIME  TIMESTAMP WITHOUT TIME ZONE NULL
);

alter table realm_mailing
  add primary key (MAILING_REALM_ID, MAILING_ID);
alter table realm_mailing
  add foreign key (MAILING_REALM_ID, MAILING_RCPT_LIST_ID) REFERENCES realm_list (LIST_REALM_ID, LIST_ID);
alter table realm_mailing
  add foreign key (MAILING_REALM_ID, MAILING_EMAIL_FILE_ID) REFERENCES realm_file (FILE_REALM_ID, FILE_ID);
alter table realm_mailing
  add foreign key (MAILING_ORGA_ID, MAILING_AUTHOR_USER_ID) REFERENCES organization_user (orga_user_orga_id, orga_user_user_id);
alter table realm_mailing
  add foreign key (MAILING_REALM_ID, MAILING_ORGA_ID) REFERENCES realm (realm_id, realm_orga_id);
comment on table realm_mailing is 'A mailing (the sending of an email to users)';
comment on column realm_mailing.MAILING_REALM_ID is 'The realm id of the mailing';
comment on column realm_mailing.MAILING_ID is 'The unique sequential id on the realm';
comment on column realm_mailing.MAILING_NAME is 'The name of the mailing';
comment on column realm_mailing.MAILING_RCPT_LIST_ID is 'The list of recipients';
comment on column realm_mailing.MAILING_EMAIL_FILE_ID is 'The email file id';
comment on column realm_mailing.MAILING_AUTHOR_USER_ID is 'The author of the email (An organizational user, the id of the user in the realm 1)';
comment on column realm_mailing.MAILING_STATUS is 'The status (draft, send, scheduled, ...)';


-- email as email address
alter table realm_user rename user_email to user_email_address;
-- drop disabled for status
alter table realm_user DROP user_disabled;
alter table realm_user add column user_status int not null default 0;
comment on column realm_user.user_status is 'The status (active: 0, deleted, ...)';
