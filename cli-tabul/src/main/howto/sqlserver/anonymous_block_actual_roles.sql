SELECT name
FROM sys.database_principals
WHERE name = 'webuser'
  AND type = 'R';
