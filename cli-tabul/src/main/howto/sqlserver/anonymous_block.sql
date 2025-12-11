BEGIN
  -- Check if the role already exists before creating it
  IF NOT EXISTS (SELECT * FROM sys.database_principals WHERE name = 'webuser' AND type = 'R')
    BEGIN
      CREATE ROLE webuser;
      PRINT 'Role webuser created successfully.';
    END
  ELSE
    BEGIN
      PRINT 'Role webuser already exists.';
    END
END
