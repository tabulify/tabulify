package net.bytle.db.jdbc;

import java.sql.Connection;
import java.sql.SQLException;

public class DbSql {

    static String getIdentifierQuote(JdbcDataStore jdbcDataStore) {
        String identifierQuoteString = "\"";
        try {
            final Connection currentConnection = jdbcDataStore.getCurrentConnection();
            if (currentConnection!=null) {
                identifierQuoteString = currentConnection.getMetaData().getIdentifierQuoteString();
            }
        } catch (SQLException e) {
            JdbcDataSystemLog.LOGGER_DB_JDBC.warning("The database "+jdbcDataStore+" throw an error when retrieving the quoted string identifier."+e.getMessage());
        }
        return identifierQuoteString;
    }

    protected String quoted(String name,JdbcDataStore dataStore)
    {
        String identifierQuote = getIdentifierQuote(dataStore);
        name = name.replace(identifierQuote, identifierQuote + identifierQuote);
        return identifierQuote + name + identifierQuote;
    }

}
