
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

-- list user
alter table realm_list_user add column list_user_in_source_id int;
alter table realm_list_user add column list_user_in_opt_in_origin VARCHAR(255) NULL;
alter table realm_list_user add column list_user_in_opt_in_ip inet;
alter table realm_list_user add column list_user_in_opt_in_time TIMESTAMP WITHOUT TIME ZONE;
alter table realm_list_user add column list_user_in_opt_in_confirmation_ip inet;
alter table realm_list_user add column list_user_in_opt_in_confirmation_time TIMESTAMP WITHOUT TIME ZONE;
alter table realm_list_user add column list_user_out_opt_out_time TIMESTAMP WITHOUT TIME ZONE;

-- row in db
-- prod: {"optInIp": "194.254.109.163", "optInTime": "2023-05-15T12:59:07.140", "confirmationIp": "162.221.128.164", "confirmationTime": "2023-05-15T13:30:02.151"}
-- dev: {"inOptInIp": "190.94.253.166", "inSourceId": 2, "inOptInTime": "2022-10-04T19:26:53", "inOptInOrigin": "2", "inOptInConfirmationIp": "190.94.253.166", "inOptInConfirmationTime": "2022-10-04T19:27:55"}
update realm_list_user set list_user_in_source_id = cast((trim( replace(list_user_data::text,'\',''),'"'))::jsonb->>'inSourceId' as int);
update realm_list_user set list_user_in_source_id = 0 where list_user_in_source_id is null ;
update realm_list_user set list_user_in_opt_in_origin = (trim(replace(list_user_data::text,'\',''),'"'))::jsonb->>'inOptInOrigin';
update realm_list_user set list_user_in_opt_in_origin = 'combostrap' where list_user_in_opt_in_origin is null || trim(list_user_in_opt_in_origin) = '';
update realm_list_user set list_user_in_opt_in_ip = cast(trim(replace(list_user_data::text,'\',''),'"')::jsonb->>'optInIp' as inet);
update realm_list_user set list_user_in_opt_in_ip = cast((trim( replace(list_user_data::text,'\',''),'"'))::jsonb->>'inOptInIp' as inet) where list_user_in_opt_in_ip is null;
update realm_list_user set list_user_in_opt_in_time = ((trim( replace(list_user_data::text,'\',''),'"'))::jsonb->>'optInTime')::timestamp ;
update realm_list_user set list_user_in_opt_in_time = cast((trim( replace(list_user_data::text,'\',''),'"'))::jsonb->>'inOptInTime' as timestamp)  where list_user_in_opt_in_time is null ;
update realm_list_user set list_user_in_opt_in_confirmation_ip = cast((trim( replace(list_user_data::text,'\',''),'"'))::jsonb->>'inOptInConfirmationIp' as inet);
update realm_list_user set list_user_in_opt_in_confirmation_ip = cast((trim( replace(list_user_data::text,'\',''),'"'))::jsonb->>'confirmationIp' as inet) where list_user_in_opt_in_ip is null;
update realm_list_user set list_user_in_opt_in_confirmation_time = cast((trim( replace(list_user_data::text,'\',''),'"'))::jsonb->>'confirmationTime' as timestamp);
update realm_list_user set list_user_in_opt_in_confirmation_time = cast((trim( replace(list_user_data::text,'\',''),'"'))::jsonb->>'inOptInConfirmationTime' as timestamp) where list_user_in_opt_in_confirmation_time is null;

alter table realm_list_user alter column list_user_in_source_id set not null;
-- drop
alter table realm_list_user drop column list_user_data;
