-- a list of all realm
create table IF NOT EXISTS realm
(
  REALM_ID                BIGSERIAL                   NOT NULL PRIMARY KEY,
  REALM_HANDLE            varchar(20)                 NOT NULL UNIQUE,
  REALM_DATA              jsonb                       NOT NULL,
  REALM_CREATION_TIME     TIMESTAMP WITHOUT TIME ZONE NOT NULL,
  REALM_MODIFICATION_TIME TIMESTAMP WITHOUT TIME ZONE NULL
);
comment on table realm is 'The list of realms';
comment on column realm.REALM_HANDLE is 'A short unique name identifier used as named identifier';

CREATE TABLE realm_sequence
(
  SEQUENCE_REALM_ID          bigint                      NOT NULL references realm (REALM_ID),
  SEQUENCE_TABLE_NAME        varchar(50)                 NOT NULL,
  SEQUENCE_LAST_ID           bigint                      NOT NULL default 0,
  SEQUENCE_CREATION_TIME     TIMESTAMP WITHOUT TIME ZONE NOT NULL,
  SEQUENCE_MODIFICATION_TIME TIMESTAMP WITHOUT TIME ZONE NULL
);
alter table realm_sequence
  add primary key (SEQUENCE_REALM_ID, SEQUENCE_TABLE_NAME);
comment on table realm_sequence is 'The last value of each id for the realm tables';


-- note that because user is a reserved word, we use quote it
create table IF NOT EXISTS realm_user
(
  USER_REALM_ID          BIGINT                      NOT NULL references "realm" (REALM_ID),
  USER_ID                BIGINT                      NOT NULL,
  USER_EMAIL             varchar(255)                NOT NULL,
  USER_PASSWORD          varchar(255),
  USER_DISABLED          bool default false,
  USER_DATA              jsonb                       NOT NULL,
  USER_CREATION_TIME     TIMESTAMP WITHOUT TIME ZONE NOT NULL,
  USER_MODIFICATION_TIME TIMESTAMP WITHOUT TIME ZONE NULL
) PARTITION BY LIST (USER_REALM_ID);
alter table realm_user
  add primary key (USER_REALM_ID, USER_ID);
alter table realm_user
  add unique (USER_REALM_ID, USER_EMAIL);
comment on table realm_user is 'A list of users (a user is an identity in a realm)';
comment on column realm_user.USER_EMAIL is 'the user email address';
comment on column realm_user.USER_DISABLED is 'if the email bounce, the user should be disabled';
comment on column realm_user.USER_PASSWORD is 'a non-reversible hash of the password';
comment on column realm_user.USER_DATA is 'the user object';


-- the app holds app information such as branding data such as name, email, logo
create table IF NOT EXISTS realm_app
(
  APP_REALM_ID          BIGINT                      NOT NULL references "realm" (REALM_ID),
  APP_ID                BIGINT                      NOT NULL,
  APP_URI               varchar(255)                NOT NULL,
  APP_USER_ID           BIGINT                      NOT NULL,
  APP_DATA              jsonb                       NOT NULL,
  APP_CREATION_TIME     TIMESTAMP WITHOUT TIME ZONE NOT NULL,
  APP_MODIFICATION_TIME TIMESTAMP WITHOUT TIME ZONE NULL
);
alter table realm_app
  add primary key (APP_REALM_ID, APP_ID);
alter table realm_app
  add unique (APP_REALM_ID, APP_URI);
alter table realm_app
  add foreign key (APP_REALM_ID, APP_USER_ID) REFERENCES realm_user (USER_REALM_ID, USER_ID);
comment on table realm_app is 'A list of apps for the realm';
comment on column realm_app.APP_ID is 'The app id';
comment on column realm_app.APP_URI is 'The uri of the app (unique on the realm). It holds the authentication scope: https://www.rfc-editor.org/rfc/rfc7617#section-2.2)';
comment on column realm_app.APP_USER_ID is 'The user used for brand communication by default';
comment on column realm_app.APP_DATA is 'The non-relational data as json';


-- A list of list
create table IF NOT EXISTS realm_list
(
  LIST_REALM_ID          BIGINT                      NOT NULL references "realm" (REALM_ID),
  LIST_ID                BIGINT                      NOT NULL,
  LIST_HANDLE            varchar(30)                 NOT NULL,
  LIST_DATA              jsonb                       NOT NULL,
  LIST_OWNER_APP_ID      BIGINT                      NOT NULL,
  LIST_OWNER_USER_ID     BIGINT                      NULL,
  LIST_CREATION_TIME     TIMESTAMP WITHOUT TIME ZONE NOT NULL,
  LIST_MODIFICATION_TIME TIMESTAMP WITHOUT TIME ZONE NULL
);
alter table realm_list
  add primary key (LIST_REALM_ID, LIST_ID);
alter table realm_list
  add foreign key (LIST_REALM_ID, LIST_OWNER_USER_ID) REFERENCES realm_user (USER_REALM_ID, USER_ID);
alter table realm_list
  add foreign key (LIST_REALM_ID, LIST_OWNER_APP_ID) REFERENCES realm_app (APP_REALM_ID, APP_ID);
alter table realm_list
  add unique (LIST_REALM_ID, LIST_HANDLE);
comment on table realm_list is 'A list of list (mailing list, mail sequence, mail responder...)';
comment on column realm_list.LIST_HANDLE is 'An unique handle that permits to update the list without knowing the id';
comment on column realm_list.LIST_OWNER_APP_ID is 'The app id (to get the default owner and the design info such as logo, color, ...)';
comment on column realm_list.LIST_OWNER_USER_ID is 'If this a personal communication (cold email), the user id is set. The communication is send as if it came from the personal inbox of the user (see scope_user_id in the service table)';


-- a list of users subscribed to this registration
create table IF NOT EXISTS realm_list_registration
(
  REGISTRATION_REALM_ID          BIGINT                      NOT NULL,
  REGISTRATION_LIST_ID           BIGINT                      NOT NULL,
  REGISTRATION_USER_ID           BIGINT                      NOT NULL,
  REGISTRATION_STATUS            varchar(12)                 NOT NULL,
  REGISTRATION_DATA              jsonb                       NOT NULL,
  REGISTRATION_CREATION_TIME     TIMESTAMP WITHOUT TIME ZONE NOT NULL,
  REGISTRATION_MODIFICATION_TIME TIMESTAMP WITHOUT TIME ZONE NULL,
  CONSTRAINT list_registration_pk PRIMARY KEY (REGISTRATION_REALM_ID, REGISTRATION_LIST_ID, REGISTRATION_USER_ID)
) PARTITION BY RANGE (REGISTRATION_REALM_ID, REGISTRATION_LIST_ID);
alter table realm_list_registration
  add foreign key (REGISTRATION_REALM_ID, REGISTRATION_LIST_ID) REFERENCES realm_list (LIST_REALM_ID, LIST_ID);
alter table realm_list_registration
  add foreign key (REGISTRATION_REALM_ID, REGISTRATION_USER_ID) REFERENCES realm_user (USER_REALM_ID, USER_ID);
comment on table realm_list_registration is 'A list of users registered to the list. The table does not have any id as it can be become huge.';
comment on column realm_list_registration.REGISTRATION_USER_ID is 'The subscriber';
comment on column realm_list_registration.REGISTRATION_DATA is 'Extra data to additional contextual data required by law such as ip, origin of registration';
comment on column realm_list_registration.REGISTRATION_STATUS is 'Registered, unregistered, cleaned (bounced)';

create table IF NOT EXISTS realm_service
(
  SERVICE_REALM_ID             BIGINT                      NOT NULL references "realm" (REALM_ID),
  SERVICE_ID                   BIGINT                      NOT NULL,
  SERVICE_URI                  varchar(250)                NOT NULL,
  SERVICE_TYPE                 varchar(50)                 NOT NULL,
  SERVICE_DATA                 jsonb                       NOT NULL,
  SERVICE_IMPERSONATED_USER_ID BIGINT                      NULL,
  SERVICE_CREATION_TIME        TIMESTAMP WITHOUT TIME ZONE NOT NULL,
  SERVICE_MODIFICATION_TIME    TIMESTAMP WITHOUT TIME ZONE NULL
);
alter table realm_service
  add primary key (SERVICE_REALM_ID, SERVICE_ID);
alter table realm_service
  add unique (SERVICE_REALM_ID, SERVICE_URI);
alter table realm_service
  add foreign key (SERVICE_REALM_ID, SERVICE_IMPERSONATED_USER_ID) REFERENCES realm_user (USER_REALM_ID, USER_ID);
comment on table realm_service is 'A list of service configuration/connection. The scope is when the service can be used. If the app and user scope are null, the scope is the realm.';
comment on column realm_service.SERVICE_URI is 'The service identifier, the service uri. Example: one@twitter, second@twitter, smtp://one@host:port)';
comment on column realm_service.SERVICE_TYPE is 'The type of service (smtp, whatsapp, twitter, ...) that defines the service and ultimately the configuration data stored';
comment on column realm_service.SERVICE_DATA is 'The configuration data';
comment on column realm_service.SERVICE_IMPERSONATED_USER_ID is 'If the service is only for personal use (ie for a user), the user id is attached, this service cannot be an app service, it cannot be shared (ie used for transactional communication or system communication but only for personal communication, ie cold email). The login information are in the data.';

