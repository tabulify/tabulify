-- add the view of ip as inet
create or replace view ip_inet as
select
  '0.0.0.0'::inet + ip_from AS ip_from_inet,
  '0.0.0.0'::inet + ip_to AS ip_to_inet,
  *
FROM
  cs_ip.ip;
