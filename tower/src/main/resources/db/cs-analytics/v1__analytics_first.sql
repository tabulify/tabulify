CREATE TABLE realm_user_event
(
  user_event_id        UUID PRIMARY KEY NOT NULL,
  user_event_realm_id  bigint           NOT NULL,
  user_event_user_id   bigint           NOT NULL,
  user_event_name      varchar(255)     NOT NULL,
  user_event_timestamp timestamp        NOT NULL,
  user_event_app_id    bigint           NULL,
  user_event_data      jsonb            NOT NULL
);

comment on table realm_user_event is
  'A list of events for the realm user.
Child tables created by the application code inherit the structure.
See https://www.postgresql.org/docs/current/ddl-partitioning.html#DDL-PARTITIONING-USING-INHERITANCE';
comment on column realm_user_event.user_event_id is
  'An id to be able to identify an event uniquely. uuid v4 because we can generate it natively in the browser with Web/API/Crypto/randomUUID
See https://developer.mozilla.org/en-US/docs/Web/API/Crypto/randomUUID';
comment on column realm_user_event.user_event_name is 'The slug of the event name';
comment on column realm_user_event.user_event_realm_id is 'The realm id';
comment on column realm_user_event.user_event_user_id is 'The user id';
comment on column realm_user_event.user_event_timestamp is 'The UTC+00:00 timestamp (timezone is in the analytics data)';
comment on column realm_user_event.user_event_app_id is 'The app id (for filtering, equivalent to a property in GA). May be null if it''s a realm event such as login in';
