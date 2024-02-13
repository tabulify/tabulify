UPDATE realm_app
SET app_analytics      =
        ('{' ||
        ' "listCount": ' || analytics.listCount::text ||
        '}')::jsonb,
    app_analytics_time = now()
from (select realm_app.app_id,
             realm_app.app_realm_id,
             coalesce(listCount.listCount, 0) as listCount
      from realm_app
             LEFT OUTER JOIN (select list_realm_id
                                   , list_owner_app_id
                                   , count(*)                                          as listCount
                              from realm_list
                              group by list_realm_id, list_owner_app_id) as listCount
                             on realm_app.app_realm_id = listCount.list_realm_id
                               and realm_app.app_id = listCount.list_owner_app_id) as analytics
WHERE realm_app.app_id = analytics.app_id
  and realm_app.app_realm_id = analytics.app_realm_id;
