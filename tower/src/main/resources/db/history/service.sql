-- a service module

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
