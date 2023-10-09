create table IF NOT EXISTS campaign
(
  REALM_ID        BIGINT       NOT NULL references cs_realms.realm (REALM_ID),
  ID              BIGINT,
  CAMPAIGN_HANDLE varchar(30)  NULL,
  CAMPAIGN_NAME   varchar(250) NOT NULL,
  CAMPAIGN_TYPE   varchar(50)  NOT NULL,
  CAMPAIGN_DATA   jsonb        NOT NULL,
  CONSTRAINT campaign_pk PRIMARY KEY (REALM_ID, ID)
);

comment on table campaign IS 'the campaigns';
comment on column campaign.CAMPAIGN_TYPE IS 'the type of campaign (email, ...)';
alter table campaign
  add unique (REALM_ID, CAMPAIGN_HANDLE);
