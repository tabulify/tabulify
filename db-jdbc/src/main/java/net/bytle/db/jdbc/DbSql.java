package net.bytle.db.jdbc;

import java.sql.Connection;
import java.sql.SQLException;

public class DbSql {

    static String getIdentifierQuote(JdbcDataSystem dataSystem) {
        String identifierQuoteString = "\"";
        try {
            final Connection currentConnection = dataSystem.getCurrentConnection();
            if (currentConnection!=null) {
                identifierQuoteString = currentConnection.getMetaData().getIdentifierQuoteString();
            }
        } catch (SQLException e) {
            JdbcDataSystemLog.LOGGER_DB_JDBC.warning("The database "+dataSystem.getDatabase()+" throw an error when retrieving the quoted string identifier."+e.getMessage());
        }
        return identifierQuoteString;
    }

    protected String quoted(String name,JdbcDataSystem dataSystem)
    {
        String identifierQuote = getIdentifierQuote(dataSystem);
        name = name.replace(identifierQuote, identifierQuote + identifierQuote);
        return identifierQuote + name + identifierQuote;
    }

}
