UPDATE realm_list
SET list_user_count = analytics.userCount
  , list_user_in_count = analytics.userInCount
  , list_mailing_count = analytics.mailingCount
from (select realm_list.list_id,
             realm_list.list_realm_id,
             coalesce(userCount.userCount, 0)        as userCount,
             coalesce(userCount.userInListCount, 0)  as userInCount,
             coalesce(mailingCount.mailingCount, 0)  as mailingCount
      from realm_list
             LEFT OUTER JOIN (select list_user_realm_id
                                   , list_user_list_id
                                   , count(*)                                          as userCount
                                   , COUNT(CASE WHEN list_user_status_code = 0 THEN 1 END)  AS userInListCount
                              from realm_list_user
                              group by list_user_realm_id, list_user_list_id) as userCount
                             on realm_list.list_realm_id = userCount.list_user_realm_id
                               and realm_list.list_id = userCount.list_user_list_id
             LEFT OUTER JOIN (select mailing_realm_id
                                   , mailing_email_rcpt_list_id
                                   , count(*)                                        as mailingCount
                              from realm_mailing
                              group by mailing_realm_id, mailing_email_rcpt_list_id) as mailingCount
                             on realm_list.list_realm_id = userCount.list_user_realm_id
                               and realm_list.list_id = userCount.list_user_list_id) as analytics
WHERE realm_list.list_id = analytics.list_id
  and realm_list.list_realm_id = analytics.list_realm_id;
