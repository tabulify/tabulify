
create table IP (
  IP_FROM INTEGER,
  IP_TO INTEGER,
  REGISTRY varchar(255),
  ASSIGNED INTEGER,
  CTRY varchar(2),
  CNTRY varchar(3),
  COUNTRY varchar(255)
);

create unique index IP_FROM_TO on IP (IP_FROM, IP_TO);
