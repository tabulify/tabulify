
create table IP (
  IP_FROM BIGINT,
  IP_TO BIGINT,
  REGISTRY varchar(255),
  ASSIGNED BIGINT,
  CTRY varchar(2),
  CNTRY varchar(3),
  COUNTRY varchar(255)
);

create unique index IP_FROM_TO on IP (IP_FROM, IP_TO);
