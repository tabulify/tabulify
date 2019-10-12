package net.bytle.db.database.Hana;

import net.bytle.db.database.DataTypeDatabase;
import net.bytle.db.database.SqlDatabase;
import net.bytle.db.engine.DbDdl;
import net.bytle.db.jdbc.JdbcDataSystem;
import net.bytle.db.model.ForeignKeyDef;
import net.bytle.db.model.PrimaryKeyDef;
import net.bytle.db.jdbc.TableDef;
import net.bytle.db.model.UniqueKeyDef;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by gerard on 11-01-2016.
 */
public class SqlDatabaseIHana extends SqlDatabase {


    private static Map<Integer, DataTypeDatabase> dataTypeDatabaseSet = new HashMap<>();

    static {
        dataTypeDatabaseSet.put(HanaDbVarcharType.TYPE_CODE, new HanaDbVarcharType());
    }

    public SqlDatabaseIHana(JdbcDataSystem jdbcDataSystem) {
            super(jdbcDataSystem);
    }


    @Override
    public DataTypeDatabase dataTypeOf(Integer typeCode) {
        return dataTypeDatabaseSet.get(typeCode);
    }

    /**
     * See http://help.sap.com/saphelp_hanaplatform/helpdata/en/20/d58a5f75191014b2fe92141b7df228/content.htm
     * for the complete statement
     * @param tableDef
     * @return a create table statement with primary key, foreign and unique
     */
    @Override
    public List<String> getCreateTableStatements(TableDef tableDef, String name) {

        List<String> statements = new ArrayList<>();

        String statement = "create ";

        // String tableType = tableDef.getCreateProperties().getProperty("after_create");
        //        if (tableType != null){
        //            statement += tableType;
        //        }
        statement += " table " + name + " (\n"
                + DbDdl.getCreateTableStatementColumnsDefinition(tableDef.getColumnDefs(), tableDef.getSchema())
                + "\n)";

        statements.add(statement);
        final PrimaryKeyDef primaryKey = tableDef.getPrimaryKey();
        if (primaryKey!=null) {
            statements.add(DbDdl.getAlterTablePrimaryKeyStatement(primaryKey, tableDef.getSchema()));
        }

        for (ForeignKeyDef foreignKeyDef:tableDef.getForeignKeys()){
            statements.add(DbDdl.getAlterTableForeignKeyStatement(foreignKeyDef, tableDef.getSchema()));
        }

        for (UniqueKeyDef uniqueKeyDef:tableDef.getUniqueKeys()){
            statements.add(DbDdl.getAlterTableUniqueKeyStatement(uniqueKeyDef, tableDef.getSchema()));
        }

        return statements;

    }


    @Override
    public Object getLoadObject(int targetColumnType, Object sourceObject) {
        return null;
    }

    @Override
    public String getNormativeSchemaObjectName(String objectName) {
        return null;
    }

    @Override
    public Integer getMaxWriterConnection() {
        return null;
    }

}
