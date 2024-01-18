UPDATE realm_list
SET list_analytics      =
        ('{' ||
        ' "userCount": ' || analytics.userCount::text || ',' ||
        ' "userInCount": ' || analytics.userInCount::text ||
        '}')::jsonb,
    list_analytics_time = now()
from (select realm_list.list_id,
             realm_list.list_realm_id,
             coalesce(userCount.userCount, 0)        as userCount,
             coalesce(userCount.userInListCount, 0)  as userInCount
      from realm_list
             LEFT OUTER JOIN (select list_user_realm_id
                                   , list_user_list_id
                                   , count(*)                                          as userCount
                                   , COUNT(CASE WHEN list_user_status = 0 THEN 1 END)  AS userInListCount
                              from realm_list_user
                              group by list_user_realm_id, list_user_list_id) as userCount
                             on realm_list.list_realm_id = userCount.list_user_realm_id
                               and realm_list.list_id = userCount.list_user_list_id) as analytics
WHERE realm_list.list_id = analytics.list_id
  and realm_list.list_realm_id = analytics.list_realm_id;
