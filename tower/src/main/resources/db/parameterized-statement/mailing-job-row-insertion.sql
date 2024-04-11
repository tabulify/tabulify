-- row insertion from a job
insert into cs_jobs.realm_mailing_row
(mailing_row_realm_id,
 mailing_row_mailing_id,
 mailing_row_mailing_job_id,
 mailing_row_user_id,
 mailing_row_status_code,
 mailing_row_creation_time)
select listUser.list_user_realm_id,
       mailing.mailing_id,
       mailingJob.mailing_job_id,
       listUser.list_user_user_id,
       0,
       now()
from cs_realms.realm_list_user listUser
       inner join cs_realms.realm_mailing mailing on listUser.list_user_realm_id = mailing.mailing_realm_id and
                                                     listUser.list_user_list_id = mailing.mailing_email_rcpt_list_id
       inner join cs_jobs.realm_mailing_job mailingJob
                  on mailing.mailing_realm_id = mailingJob.mailing_job_realm_id and
                     mailing.mailing_id = mailingJob.mailing_job_mailing_id
where listUser.list_user_status = 0
  and mailingJob.mailing_job_realm_id = $1
  and mailingJob.mailing_job_id = $2
