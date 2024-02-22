UPDATE realm
SET realm_user_count = analytics.userCount,
    realm_app_count = analytics.appCount,
    realm_list_count = analytics.listCount
from (select realm.realm_id,
             coalesce(userCount.userCount, 0) as userCount,
             coalesce(appCount.appCount, 0)   as appCount,
             coalesce(listCount.listCount, 0) as listCount
      from realm
             LEFT OUTER JOIN (select user_realm_id, count(user_id) as userCount
                              from realm_user
                              group by user_realm_id) as userCount
                             on realm.realm_id = userCount.user_realm_id
             LEFT OUTER JOIN (select app_realm_id, count(app_id) as appCount
                              from realm_app
                              group by app_realm_id) as appCount
                             on realm.realm_id = appCount.app_realm_id
             LEFT OUTER JOIN (select list_realm_id, count(list_id) as listCount
                              from realm_list
                              group by list_realm_id) as listCount
                             on realm.realm_id = listCount.list_realm_id) as analytics
WHERE realm.realm_id = analytics.realm_id;
