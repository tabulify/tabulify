update cs_realms.realm_mailing mailing
set mailing_item_count = analytics.countTotal,
    mailing_status = $1
from (select mailingItem.mailing_item_realm_id as realm_id, mailingItem.mailing_item_mailing_id as mailing_id, count(*) as countTotal
      from cs_jobs.realm_mailing_item mailingItem
             inner join cs_realms.realm_mailing mailing
                        on mailingItem.mailing_item_realm_id = mailing.mailing_realm_id
                          and mailingItem.mailing_item_mailing_id = mailing.mailing_id
      group by mailingItem.mailing_item_realm_id, mailingItem.mailing_item_mailing_id) as analytics
where mailing.mailing_realm_id = analytics.realm_id
  and mailing.mailing_id = analytics.mailing_id
  and mailing.mailing_realm_id = $2
  and mailing.mailing_id = $3
RETURNING mailing_item_count
