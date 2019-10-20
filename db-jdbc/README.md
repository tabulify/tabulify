# Db Jdbc 

## About

A data system implementation based on Jdbc.

If a database driver implements ANSI, it should work without a SQL database implementation
but as this is never the case, jdbc database should implement the SQL Database interface.

The reference implementation is the Sqlite one. The tests are there.


## Note

See: DatabaseMetadata function:

  * supportsANSI92EntryLevelSQL() - Retrieves whether this database supports the ANSI92 entry level SQL grammar.
  * boolean	supportsANSI92FullSQL() - Retrieves whether this database supports the ANSI92 full SQL grammar supported.
  * boolean	supportsANSI92IntermediateSQL() - Retrieves whether this database supports the ANSI92 intermediate SQL grammar supported.
  * supportsCoreSQLGrammar() - Retrieves whether this database supports the ODBC Core SQL grammar.
  * supportsExtendedSQLGrammar() - Retrieves whether this database supports the ODBC Extended SQL grammar.
  *	supportsMinimumSQLGrammar() - Retrieves whether this database supports the ODBC Minimum SQL grammar.