-- The list of jobs
create table IF NOT EXISTS queues
(
  QUEUE_NAME              VARCHAR(50)                 NOT NULL,
  OBJECT_ID               VARCHAR(250)                NOT NULL,
  DATA                    jsonb                       NOT NULL,
  CREATION_TIME           TIMESTAMP WITHOUT TIME ZONE NOT NULL
);
