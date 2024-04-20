-- first draft
-- the file system drive
create table IF NOT EXISTS realm_file_drive
(
  DRIVE_REALM_ID          BIGINT                      NOT NULL references "realm" (REALM_ID),
  DRIVE_ID                BIGINT                      NOT NULL,
  DRIVE_HANDLE            varchar(50)                 NOT NULL,
  DRIVE_APP_ID            BIGINT                      NOT NULL,
  DRIVE_CREATION_TIME     TIMESTAMP WITHOUT TIME ZONE NOT NULL,
  DRIVE_MODIFICATION_TIME TIMESTAMP WITHOUT TIME ZONE NULL
);
alter table realm_file_drive
  add primary key (DRIVE_REALM_ID, DRIVE_ID);
alter table realm_file_drive
  add foreign key (DRIVE_REALM_ID, DRIVE_APP_ID) REFERENCES realm_app (APP_REALM_ID, APP_ID);
alter table realm_file_drive
  add unique (DRIVE_REALM_ID, DRIVE_HANDLE);
comment on table realm_file_drive is 'A file system drive';
comment on column realm_file_drive.DRIVE_REALM_ID is 'The realm id';
comment on column realm_file_drive.DRIVE_ID is 'The unique id in the realm';
comment on column realm_file_drive.DRIVE_HANDLE is 'A Handle (A DNS label)';
comment on column realm_file_drive.DRIVE_APP_ID is 'The App, this drive belongs';

-- represents a file
create table IF NOT EXISTS realm_file
(
  FILE_REALM_ID          BIGINT                      NOT NULL references "realm" (REALM_ID),
  FILE_ID                BIGINT                      NOT NULL,
  FILE_DRIVE_ID          BIGINT                      NOT NULL,
  FILE_GUID              varchar(50)                 NOT NULL,
  FILE_NAME              varchar(50)                 NOT NULL,
  FILE_PARENT_ID         BIGINT                      NULL,
  FILE_TYPE              INT                         NOT NULL,
  FILE_MEDIA_TYPE        varchar(50)                 NOT NULL,
  FILE_LOGICAL_TYPE      varchar(50)                 NULL,
  FILE_TEXT              TEXT                        NULL,
  FILE_METADATA          JSONB                       NULL,
  FILE_CREATION_TIME     TIMESTAMP WITHOUT TIME ZONE NOT NULL,
  FILE_MODIFICATION_TIME TIMESTAMP WITHOUT TIME ZONE NULL
);
alter table realm_file
  add primary key (FILE_REALM_ID, FILE_ID);
alter table realm_file
  add foreign key (FILE_REALM_ID, FILE_DRIVE_ID) REFERENCES realm_file_drive (DRIVE_REALM_ID, DRIVE_ID);
alter table realm_file
  add foreign key (FILE_REALM_ID, FILE_PARENT_ID) REFERENCES realm_file (FILE_REALM_ID, FILE_ID);
-- A file name in a parent is unique
alter table realm_file
  add unique (FILE_REALM_ID, FILE_PARENT_ID, FILE_NAME);
-- You can't have 2 roots (ie null value) for a drive
alter table realm_file
  add unique (FILE_REALM_ID, FILE_DRIVE_ID, FILE_PARENT_ID);
comment on table realm_file is 'A file (that keeps its identifier for life, not its path)';
comment on column realm_file.FILE_REALM_ID is 'The realm id of the file';
comment on column realm_file.FILE_ID is 'The unique sequential id on the realm';
comment on column realm_file.FILE_PARENT_ID is 'The parent directory or null for the root of the drive';
comment on column realm_file.FILE_GUID is 'An textual guid unique on the realm without any prefix (Used to sync with an external app that may be our mobile app or any third party app)';
comment on column realm_file.FILE_NAME is 'The name of the file with or without extension (ie employee.csv)';
comment on column realm_file.FILE_TYPE is 'The type of file (0: directory, 1 regular file)';
comment on column realm_file.FILE_MEDIA_TYPE is 'The media type (the structure of the file so that we can create an AST). ie text/csv, text/json';
comment on column realm_file.FILE_LOGICAL_TYPE is 'A logical type to define the logical use and type of metadata (Example: email as json, eml, ... or xml may be a full document or a fragment, may contain raw data or ui description)';
comment on column realm_file.FILE_TEXT is 'The content of text file (binary file such as images are not stored in the database)';
comment on column realm_file.FILE_METADATA is 'A json that holds extra metadata information. The type of metadata is given by the logical type';
comment on column realm_file.FILE_PARENT_ID is 'The parent file (if null, the root)';
