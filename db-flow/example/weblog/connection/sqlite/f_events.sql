select
  events.created_at,
  events.tracking_id,
  events.type,
  case when events.user_id is null then users.user_id else events.user_id end as user_id,
  case when events.utm_campaign is null then 'unknown' else events.utm_campaign end as utm_campaign,
  case when events.utm_medium is null then 'unknown' else events.utm_medium end as utm_medium
from
  events left join users on events.tracking_id = users.tracking_id


