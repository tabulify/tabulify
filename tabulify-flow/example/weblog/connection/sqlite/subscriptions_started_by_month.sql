-- How many customers convert from trial to paid each month?
-- We are missing:
--    - a date dimension
--    - a mont aggregate
-- The case where a customer cancelled in the same month is not taken
select
  strftime('%Y%m',created_at)
, count(1) from events_2
where
  type = 'Subscription Started'
group by
  strftime('%Y%m',created_at);

