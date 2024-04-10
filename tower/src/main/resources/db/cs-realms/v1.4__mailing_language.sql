-- not char otherwise 2 get spaces and not null
alter table realm_mailing add column mailing_email_language varchar(2);
comment on column realm_mailing.mailing_email_language is 'The language of the email (used in assistive technology)';
