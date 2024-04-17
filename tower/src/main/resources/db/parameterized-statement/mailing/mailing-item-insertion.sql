-- row insertion from a job
insert into cs_jobs.realm_mailing_item
(mailing_item_realm_id,
 mailing_item_mailing_id,
 mailing_item_user_id,
 mailing_item_status_code,
 mailing_item_creation_time)
select listUser.list_user_realm_id,
       mailing.mailing_id,
       listUser.list_user_user_id,
       -2,
       now()
from cs_realms.realm_list_user listUser
       inner join cs_realms.realm_mailing mailing on listUser.list_user_realm_id = mailing.mailing_realm_id and
                                                     listUser.list_user_list_id = mailing.mailing_email_rcpt_list_id
where listUser.list_user_status = $1
  and mailing.mailing_realm_id = $2
  and mailing.mailing_id = $3
-- in case of mailing reset in dev
on conflict (mailing_item_realm_id,mailing_item_mailing_id,mailing_item_user_id) do nothing
