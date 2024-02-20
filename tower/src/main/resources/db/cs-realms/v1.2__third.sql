--
-- owner user is not a realm user but on org user
-- ---------------------------------------------------
-- app user was a realm user, not an organization user
comment on column realm_app.APP_USER_ID is 'The organizational user used for brand communication by default (user in realm 1 that belongs to the organization of the realm)';
-- constraint is on the app level (for now)
alter table realm_app drop constraint realm_app_app_realm_id_app_user_id_fkey;

comment on column realm_list.LIST_OWNER_USER_ID is 'The owner of the list (The organizational user used for brand communication by default (user in realm 1 that belongs to the organization of the realm)';
alter table realm_list drop constraint realm_list_list_realm_id_list_owner_user_id_fkey;


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
  FILE_THIRD_TYPE         varchar(50)               NULL,
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
comment on column realm_file.FILE_THIRD_TYPE is 'A third type to define the logical use (for instance, xml may be a full document or a fragment, may contain raw data or ui description)';

-- represents a mailing (sending an email to a list of users)
create table IF NOT EXISTS realm_mailing
(
  MAILING_REALM_ID           BIGINT                    NOT NULL references "realm" (REALM_ID),
  MAILING_ID                 BIGINT                    NOT NULL,
  MAILING_RCPT_LIST_ID       BIGINT                    NOT NULL,
  MAILING_BODY_FILE_ID       BIGINT                    NOT NULL,
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
  add foreign key (MAILING_REALM_ID, MAILING_BODY_FILE_ID) REFERENCES realm_file (FILE_REALM_ID, FILE_ID);
comment on table realm_mailing is 'A mailing (sending email to users)';
comment on column realm_mailing.MAILING_REALM_ID is 'The realm id of the mailing';
comment on column realm_mailing.MAILING_ID is 'The unique sequential id on the realm';
comment on column realm_mailing.MAILING_RCPT_LIST_ID is 'The list of recipients';
comment on column realm_mailing.MAILING_BODY_FILE_ID is 'The email body template';
comment on column realm_mailing.MAILING_AUTHOR_USER_ID is 'The author of the email (An organizational user, the id of the user in the realm 1)';
comment on column realm_mailing.MAILING_STATUS is 'The status (send, scheduled, ...)';
