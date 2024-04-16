update cs_realms.realm_mailing mailing
set mailing_row_count = analytics.countTotal,
    mailing_status = $1
from (select mailingRow.mailing_row_realm_id as realm_id, mailingRow.mailing_row_mailing_id as mailing_id, count(*) as countTotal
      from cs_jobs.realm_mailing_row mailingRow
             inner join cs_realms.realm_mailing mailing
                        on mailingRow.mailing_row_realm_id = mailing.mailing_realm_id
                          and mailingRow.mailing_row_mailing_id = mailing.mailing_id
      group by mailingRow.mailing_row_realm_id, mailingRow.mailing_row_mailing_id) as analytics
where mailing.mailing_realm_id = analytics.realm_id
  and mailing.mailing_id = analytics.mailing_id
  and mailing.mailing_realm_id = $2
  and mailing.mailing_id = $3
RETURNING mailing_row_count
