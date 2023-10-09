-- Update tracking id (no null)
select
  events.created_at,
  case when events.tracking_id is null then users.tracking_id else events.tracking_id end as tracking_id,
  events.type,
  events.user_id,
  events.utm_campaign,
  events.utm_medium
from
  f_events events left join users on events.user_id = users.user_id


