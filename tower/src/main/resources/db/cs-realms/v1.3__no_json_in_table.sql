
-- no json in the database (avoid pollution)
--realm
alter table realm add column realm_name VARCHAR(50);
update realm set realm_name = (trim( replace(realm_data::text,'\',''),'"'))::jsonb->'name';
alter table realm alter column realm_name set not null;
alter table realm drop column realm_data;
alter table realm add column realm_user_count BIGINT;
alter table realm add column realm_list_count BIGINT;
alter table realm add column realm_app_count BIGINT;
alter table realm drop column realm_analytics;
alter table realm drop column realm_analytics_time;

--app
alter table realm_app add column app_name VARCHAR(50) NULL;
alter table realm_app add column app_home VARCHAR(250) NULL;
update realm_app set app_name = (trim( replace(app_data::text,'\',''),'"'))::jsonb->'name';
update realm_app set app_home = (trim( replace(app_data::text,'\',''),'"'))::jsonb->'home';
alter table realm_app alter column app_name set not null;
alter table realm_app drop column app_data;

alter table realm_app add column app_list_count BIGINT;
alter table realm_app drop column app_analytics;
alter table realm_app drop column app_analytics_time;

-- list
alter table realm_list add column list_user_count BIGINT;
alter table realm_list add column list_user_in_count BIGINT;
alter table realm_list add column list_mailing_count BIGINT;
alter table realm_list add column list_name VARCHAR(50) NULL;
alter table realm_list add column list_title VARCHAR(250) NULL;
alter table realm_list add column list_desc_file_id BIGINT;
update realm_list set list_name = (trim( replace(list_data::text,'\',''),'"'))::jsonb->'name';
update realm_list set list_title = (trim( replace(list_data::text,'\',''),'"'))::jsonb->'title';
alter table realm_list drop column list_data;
alter table realm_list drop column list_analytics;
alter table realm_list drop column list_analytics_time;
alter table realm_list alter column list_name set not null;
alter table realm_list
  add foreign key (list_REALM_ID, list_desc_file_id) REFERENCES realm_file (FILE_REALM_ID, FILE_ID);
comment on column realm_list.list_user_count is 'An analytics - the count of users on the list';
comment on column realm_list.list_user_in_count is 'An analytics - the count of users that are still in on the list';
comment on column realm_list.list_mailing_count is 'An analytics - the count of mailing for the list';
comment on column realm_list.list_name is 'A short name for the list';
comment on column realm_list.list_title is 'A title for the list';
comment on column realm_list.list_desc_file_id is 'A description in a document file format';
comment on table realm_list is 'A list of users';
comment on column realm_list.list_realm_id is 'The realm id';
comment on column realm_list.list_id is 'The list id (unique in the realm)';
