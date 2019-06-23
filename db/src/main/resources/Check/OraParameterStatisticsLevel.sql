-- When this parameter is on ALL
-- We can get loose more than 30% performance gain
select * from v$parameter where name = 'statistics_level' and value = 'ALL';