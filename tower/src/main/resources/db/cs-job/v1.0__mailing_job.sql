-- A mailing execution
create table IF NOT EXISTS cs_job.realm_mailing_job
(
  MAILING_JOB_REALM_ID             BIGINT                      NOT NULL references "cs_realms"."realm" (REALM_ID),
  MAILING_JOB_ID                   INT                         NOT NULL,
  MAILING_JOB_STATUS_CODE          INT                         NULL,
  MAILING_JOB_STATUS_MESSAGE       TEXT                        NULL,
  MAILING_JOB_START_TIME           TIMESTAMP WITHOUT TIME ZONE NOT NULL,
  MAILING_JOB_END_TIME             TIMESTAMP WITHOUT TIME ZONE NULL,
  MAILING_JOB_COUNT_EMAIL_TO_SEND  BIGINT                      NULL,
  MAILING_JOB_COUNT_SMTP_SUCCESS   BIGINT                      NULL,
  MAILING_JOB_COUNT_SMTP_EXECUTION BIGINT                      NULL
);
comment on table cs_job.realm_mailing_job is 'The mailing executions';
comment on column cs_job.realm_mailing_job.MAILING_JOB_COUNT_SMTP_SUCCESS is 'The number of email send successfully (ie successful smtp transaction)';
comment on column cs_job.realm_mailing_job.MAILING_JOB_COUNT_SMTP_EXECUTION is 'The number of smtp transaction execution (successful or not)';
comment on column cs_job.realm_mailing_job.MAILING_JOB_COUNT_EMAIL_TO_SEND is 'The number of email to send';

-- a mailing row
create table IF NOT EXISTS cs_job.realm_mailing_row
(
  MAILING_ROW_REALM_ID              BIGINT                      NOT NULL references "cs_realms"."realm" (REALM_ID),
  MAILING_ROW_MAILING_ID            BIGINT                      NOT NULL,
  MAILING_ROW_USER_ID               BIGINT                      NOT NULL,
  MAILING_ROW_STATUS_CODE           INT                         NULL,
  MAILING_ROW_STATUS_MESSAGE        TEXT                        NULL,
  MAILING_ROW_COUNT_FAILURE         INT                         NULL,
  MAILING_ROW_EMAIL_SERVER_RECEIVER VARCHAR(255)                NULL,
  MAILING_ROW_EMAIL_SERVER_SENDER   VARCHAR(255)                NULL,
  MAILING_ROW_EMAIL_MESSAGE_ID      VARCHAR(255)                NULL,
  MAILING_ROW_EMAIL_DATE            TIMESTAMP WITHOUT TIME ZONE NULL,
  MAILING_ROW_EMAIL_MESSAGE         TEXT                        NULL,
  MAILING_ROW_CREATION_TIME         TIMESTAMP WITHOUT TIME ZONE NOT NULL,
  MAILING_ROW_MODIFICATION_TIME     TIMESTAMP WITHOUT TIME ZONE NULL
);
comment on table cs_job.realm_mailing_row is 'An execution unit for a SMTP transfer (ie one row, one email, one user)';
alter table cs_job.realm_mailing_row
  add primary key (MAILING_ROW_REALM_ID, MAILING_ROW_MAILING_ID, MAILING_ROW_USER_ID);
alter table cs_job.realm_mailing_row
  add foreign key (MAILING_ROW_REALM_ID, MAILING_ROW_USER_ID) REFERENCES "cs_realms"."realm_user" (user_realm_id, user_id);
alter table cs_job.realm_mailing_row
  add foreign key (MAILING_ROW_REALM_ID, MAILING_ROW_MAILING_ID) REFERENCES "cs_realms"."realm_mailing" (mailing_realm_id, mailing_id);

comment on column cs_job.realm_mailing_row.MAILING_ROW_COUNT_FAILURE is 'The number of failed transaction';
comment on column cs_job.realm_mailing_row.MAILING_ROW_EMAIL_SERVER_RECEIVER is 'The server (mx) where the email was send (ie the receiver)';
comment on column cs_job.realm_mailing_row.MAILING_ROW_EMAIL_SERVER_SENDER is 'The server that has send this message';
comment on column cs_job.realm_mailing_row.MAILING_ROW_EMAIL_MESSAGE_ID is 'The email message id';
comment on column cs_job.realm_mailing_row.MAILING_ROW_EMAIL_DATE is 'The send date';
