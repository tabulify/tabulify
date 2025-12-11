DECLARE
  role_count NUMBER;
BEGIN
  -- Check if role already exists
  SELECT COUNT(*)
  INTO role_count
  FROM dba_roles
  WHERE role = 'WEBUSER';

  IF role_count = 0 THEN
    EXECUTE IMMEDIATE 'CREATE ROLE webuser';
    DBMS_OUTPUT.PUT_LINE('Role webuser created successfully.');
  ELSE
    DBMS_OUTPUT.PUT_LINE('Role webuser already exists.');
  END IF;

END;
/
