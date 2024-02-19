-- The list of jobs
create table IF NOT EXISTS queues
(
  QUEUE_NAME    VARCHAR(50)                 NOT NULL,
  OBJECT_ID     VARCHAR(250)                NOT NULL,
  DATA          jsonb                       NOT NULL,
  CREATION_TIME TIMESTAMP WITHOUT TIME ZONE NOT NULL
);
alter table queues
  add primary key (QUEUE_NAME, OBJECT_ID);
comment on table queues is 'A table to store queue data';
comment on column queues.QUEUE_NAME is 'The name of the queue';
comment on column queues.OBJECT_ID is 'The id of the element';
comment on column queues.DATA is 'The data representation of the element';
comment on column queues.CREATION_TIME is 'The sorting value. The oldest is at the head)';
