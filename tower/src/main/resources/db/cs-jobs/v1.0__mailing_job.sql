-- A mailing execution
create table IF NOT EXISTS cs_jobs.realm_mailing_job
(
  MAILING_JOB_REALM_ID             BIGINT                      NOT NULL references "cs_realms"."realm" (REALM_ID),
  MAILING_JOB_ID                   BIGINT                      NOT NULL,
  MAILING_JOB_MAILING_ID           BIGINT                      NOT NULL,
  MAILING_JOB_STATUS_CODE          INT                         NOT NULL,
  MAILING_JOB_STATUS_MESSAGE       TEXT                        NULL,
  MAILING_JOB_START_TIME           TIMESTAMP WITHOUT TIME ZONE NOT NULL,
  MAILING_JOB_END_TIME             TIMESTAMP WITHOUT TIME ZONE NULL,
  MAILING_JOB_ITEM_TO_EXECUTE_COUNT BIGINT                      NOT NULL,
  MAILING_JOB_ITEM_SUCCESS_COUNT    BIGINT                      NULL,
  MAILING_JOB_ITEM_EXECUTION_COUNT  BIGINT                      NULL
);
comment on table cs_jobs.realm_mailing_job is 'The mailing executions';
alter table cs_jobs.realm_mailing_job
  add primary key (MAILING_JOB_REALM_ID, MAILING_JOB_ID);
alter table cs_jobs.realm_mailing_job
  add foreign key (MAILING_JOB_REALM_ID, MAILING_JOB_MAILING_ID) REFERENCES "cs_realms"."realm_mailing" (mailing_realm_id, mailing_id);
comment on column cs_jobs.realm_mailing_job.MAILING_JOB_ITEM_TO_EXECUTE_COUNT is 'The number of item to execute in the job (ie email to send)';
comment on column cs_jobs.realm_mailing_job.MAILING_JOB_ITEM_SUCCESS_COUNT is 'The number of email send successfully (ie successful smtp transaction)';
comment on column cs_jobs.realm_mailing_job.MAILING_JOB_ITEM_EXECUTION_COUNT is 'The number of smtp transaction execution (successful or not)';


-- a mailing row
create table IF NOT EXISTS cs_jobs.realm_mailing_item
(
  MAILING_ITEM_REALM_ID              BIGINT                      NOT NULL references "cs_realms"."realm" (REALM_ID),
  MAILING_ITEM_MAILING_ID            BIGINT                      NOT NULL,
  MAILING_ITEM_USER_ID               BIGINT                      NOT NULL,
  MAILING_ITEM_STATUS_CODE           INT                         NULL,
  MAILING_ITEM_MAILING_JOB_ID        BIGINT                      NULL,
  MAILING_ITEM_PLANNED_DELIVERY_TIME TIMESTAMP WITHOUT TIME ZONE NULL,
  MAILING_ITEM_STATUS_MESSAGE        TEXT                        NULL,
  MAILING_ITEM_FAILURE_COUNT         INT                         NULL,
  MAILING_ITEM_EMAIL_SERVER_RECEIVER VARCHAR(255)                NULL,
  MAILING_ITEM_EMAIL_SERVER_SENDER   VARCHAR(255)                NULL,
  MAILING_ITEM_EMAIL_MESSAGE_ID      VARCHAR(255)                NULL,
  MAILING_ITEM_EMAIL_DATE            TIMESTAMP WITHOUT TIME ZONE NULL,
  MAILING_ITEM_EMAIL_MESSAGE         TEXT                        NULL,
  MAILING_ITEM_CREATION_TIME         TIMESTAMP WITHOUT TIME ZONE NOT NULL,
  MAILING_ITEM_MODIFICATION_TIME     TIMESTAMP WITHOUT TIME ZONE NULL
);
comment on table cs_jobs.realm_MAILING_ITEM is 'An execution unit for a SMTP transfer (ie one row, one email, one user)';
alter table cs_jobs.realm_mailing_item
  add primary key (MAILING_ITEM_REALM_ID, MAILING_ITEM_MAILING_ID, MAILING_ITEM_USER_ID);
alter table cs_jobs.realm_mailing_item
  add foreign key (MAILING_ITEM_REALM_ID, MAILING_ITEM_USER_ID) REFERENCES "cs_realms"."realm_user" (user_realm_id, user_id);
alter table cs_jobs.realm_mailing_item
  add foreign key (MAILING_ITEM_REALM_ID, MAILING_ITEM_MAILING_JOB_ID) REFERENCES "cs_jobs".realm_mailing_job(mailing_job_realm_id, mailing_job_id);
alter table cs_jobs.realm_mailing_item
  add foreign key (MAILING_ITEM_REALM_ID, MAILING_ITEM_MAILING_ID) REFERENCES "cs_realms"."realm_mailing" (mailing_realm_id, mailing_id);

comment on column cs_jobs.realm_mailing_item.MAILING_ITEM_MAILING_JOB_ID is 'The job id that has executed this row for the last time';
comment on column cs_jobs.realm_mailing_item.MAILING_ITEM_FAILURE_COUNT is 'The number of failed transaction';
comment on column cs_jobs.realm_mailing_item.MAILING_ITEM_EMAIL_SERVER_RECEIVER is 'The server (mx) where the email was send (ie the receiver)';
comment on column cs_jobs.realm_mailing_item.MAILING_ITEM_EMAIL_SERVER_SENDER is 'The server that has send this message';
comment on column cs_jobs.realm_mailing_item.MAILING_ITEM_EMAIL_MESSAGE_ID is 'The email message id';
comment on column cs_jobs.realm_mailing_item.MAILING_ITEM_EMAIL_DATE is 'The email send date';
comment on column cs_jobs.realm_mailing_item.MAILING_ITEM_PLANNED_DELIVERY_TIME is 'The planned delivery date (UTC local)';
