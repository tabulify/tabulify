drop table if exists foo;
drop table if exists bar;
create table foo
(
  update_time timestamp
);
create table bar as
select *
from foo;
PRAGMA
table_info('bar');
