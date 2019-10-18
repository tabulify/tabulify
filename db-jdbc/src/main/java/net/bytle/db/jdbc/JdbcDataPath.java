package net.bytle.db.jdbc;

import net.bytle.db.model.TableDef;
import net.bytle.db.spi.DataPath;

import java.sql.DatabaseMetaData;
import java.util.ArrayList;
import java.util.List;

public class JdbcDataPath extends DataPath  {

    private final JdbcDataSystem jdbcDataSystem;
    private final String name;
    private final String schema;
    private final String catalog;


    public JdbcDataPath(JdbcDataSystem jdbcDataSystem, String catalog, String schema, String name) {
        this.jdbcDataSystem = jdbcDataSystem;
        this.catalog = catalog;
        this.schema = schema;
        this.name = name;
    }

    public static JdbcDataPath of(JdbcDataSystem jdbcDataSystem, String catalog, String schema, String name) {
        return new JdbcDataPath(jdbcDataSystem, catalog, schema, name);
    }

    @Override
    public JdbcDataSystem getDataSystem() {
        return jdbcDataSystem;
    }

    @Override
    public TableDef getDataDefOf() {


        return DbObjectBuilder.getTableDef(super.getDataDefOf());

    }

    /**
    * {@link DatabaseMetaData#getMaxSchemaNameLength()}
     */
    public JdbcDataPath getSchema() {

        if (schema==null){
            return null;
        } else {
            return JdbcDataPath.of(jdbcDataSystem, catalog, schema, null);
        }

    }

    /**
     * {@link DatabaseMetaData#getMaxTableNameLength()}
     */
    @Override
    public String getName() {
        if (name!=null){
            return name;
        }
        if (schema!=null){
            return schema;
        }
        if (catalog!=null){
            return catalog;
        }
        throw new RuntimeException("All JDBC data path name are null (catalog, schema and name)");
    }

    @Override
    public List<String> getPathSegments() {
        List<String> pathSegments = new ArrayList<>();
        if (catalog!=null){
            pathSegments.add(catalog);
        }
        if (schema!=null){
            pathSegments.add(schema);
        }
        if (name!=null){
            pathSegments.add(name);
        }
        return pathSegments;
    }


    public String getCatalog() {
        return catalog;
    }


}
