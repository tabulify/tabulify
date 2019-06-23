package net.bytle.db.engine;

import net.bytle.db.model.TableDef;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * All static function belows are using the
 *
 * @{link java.sql.Connection#getMetaData} function
 */
public class JdbcMetas {

    public void printPrimaryKey(TableDef tableDef) {

        try (
                ResultSet resultSet = tableDef.getDatabase().getCurrentConnection().getMetaData().getPrimaryKeys(null, null, tableDef.getName());
        ) {
            while (resultSet.next()) {
                System.out.println("Primary Key Column: " + resultSet.getString("COLUMN_NAME"));
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

    }

    public void printUniqueKey(TableDef tableDef) {

        try (
                ResultSet resultSet = tableDef.getDatabase().getCurrentConnection().getMetaData().getIndexInfo(null, null, tableDef.getName(), true, false)
        ) {
            while (resultSet.next()) {
                System.out.println("Unique Key Column: " + resultSet.getString("COLUMN_NAME"));
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

    }

}
