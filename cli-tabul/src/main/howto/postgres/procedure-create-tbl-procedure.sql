-- creation of the table used by the procedure
create table if not exists tbl
(
  num int
);

-- creation of the procedure
CREATE OR REPLACE PROCEDURE insert_data(a integer, b integer)
  LANGUAGE SQL
AS
$$
INSERT INTO tbl
VALUES (a);
INSERT INTO tbl
VALUES (b);
$$;

