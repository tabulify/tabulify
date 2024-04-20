-- example of querying the old data text json column

-- no json in the database (avoid pollution)
update realm_app set app_name = (trim( replace(app_data::text,'\',''),'"'))::jsonb->'name';
update realm_app set app_home = (trim( replace(app_data::text,'\',''),'"'))::jsonb->'home';

update realm_list set list_name = (trim( replace(list_data::text,'\',''),'"'))::jsonb->'name';
update realm_list set list_title = (trim( replace(list_data::text,'\',''),'"'))::jsonb->'title';



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
