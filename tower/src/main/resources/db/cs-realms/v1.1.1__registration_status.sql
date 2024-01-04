-- modify the type of the status from string to integer
alter table cs_realms.realm_list_registration add column registration_status_new int not null default 0;
alter table cs_realms.realm_list_registration drop column registration_status;
alter table cs_realms.realm_list_registration rename column registration_status_new to registration_status;
