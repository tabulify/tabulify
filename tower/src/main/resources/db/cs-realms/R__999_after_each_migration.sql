-- ${flyway:timestamp}
-- Run each time due to the timestamp
-- https://flywaydb.org/blog/flyway-timestampsAndRepeatables


-- create table list partition on realm insertion
CREATE OR REPLACE TRIGGER create_realm_partitions_on_insert
  AFTER INSERT
  ON realm
  FOR EACH ROW
EXECUTE PROCEDURE create_partitions_for_realm_on_insert();
