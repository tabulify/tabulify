select
  user_period.user_id,
  users.email,
  user_period.trial_started_date,
  user_period.subscription_started_date,
  user_period.subscription_cancelled_date,
  (strftime('%s',IFNULL(user_period.subscription_cancelled_date,'now'))-strftime('%s',user_period.trial_started_date))/60/60/24 as subscription_period,
  users.tracking_id
from
(
  select
      user_id,
      datetime(max(case when type = 'Trial Started' then created_at end)) as trial_started_date,
      datetime(max(case when type = 'Subscription Started' then created_at end)) as subscription_started_date,
      datetime(max(case when type = 'Subscription Cancelled' then created_at end)) as subscription_cancelled_date
  from events_2 events
  where user_id is not null
  group by user_id
) user_period left outer join users on users.user_id = user_period.user_id
-- It should be a full outer join (sqlite does not support them)

