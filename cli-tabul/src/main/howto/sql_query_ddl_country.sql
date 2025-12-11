-- DDL CREATE
create table country
(
  country        varchar,
  code2          varchar,
  coden          varchar,
  region_code    varchar,
  subregion_code varchar
);

-- DDL ALTER
alter table country
  add code3 varchar;


-- DML without returning clause
-- The first 2 countries
INSERT INTO country (country, code2, code3, coden, region_code, subregion_code)
VALUES ('Afghanistan', 'AF', 'AFG', '4', '142', '34');
INSERT INTO country (country, code2, code3, coden, region_code, subregion_code)
VALUES ('Åland Islands', 'AX', 'ALA', '248', '150', '154');
