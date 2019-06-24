package net.bytle.db.sqlite;

import net.bytle.db.database.SqlDatabase;

public class SqliteSqlDatabase extends SqlDatabase {

    private final SqliteProvider sqliteProvider;

    /**
     * Returns the provider that created this work system.
     *
     * @return The provider that created this work
     */
    public SqliteSqlDatabase(SqliteProvider sqliteProvider) {
        super(sqliteProvider);
        this.sqliteProvider = sqliteProvider;
    }




}
