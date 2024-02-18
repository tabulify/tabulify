-- The list of jobs
create table IF NOT EXISTS queues
(
  QUEUE_NAME              VARCHAR(50)                 NOT NULL,
  HASH_CODE               VARCHAR(250)                NOT NULL,
  TO_STRING               VARCHAR(250)                NULL,
  DATA                    jsonb                       NOT NULL,
  CREATION_TIME           TIMESTAMP WITHOUT TIME ZONE NOT NULL
);
