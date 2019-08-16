package net.bytle.db.model;

import net.bytle.db.database.Database;

import java.util.*;

/**
 * This class is used to store the data definition of a tabular structure
 * <p>
 * There is only metadata in their. This is the counterpart of {@link java.sql.ResultSetMetaData}
 * <p>
 * The location of the table is a runtime information and is then not part of it.
 * <p>
 * Plugin may used it to add information its yaml file representation
 * <p>
 * For instance, the data loader plugin will add:
 * * the number of rows to be generated
 * * a generator for each column that gives the data generation rules
 */
public class DataDef implements RelationDef {

    private final String name;

    private final RelationMeta meta;

    private HashMap<String,Object> properties = new HashMap<>();

    public DataDef(String name) {
        this.name = name;
        meta = new RelationMeta(this);
    }


    @Override
    public Database getDatabase() {
        return null;
    }

    @Override
    public SchemaDef getSchema() {
        return null;
    }

    /**
     * The table/data unit name
     *
     * @return
     */
    public String getName() {
        return name;
    }

    /**
     * Fully Qualified name in Bytle Db (ie with the database Name)
     *
     * @return
     */
    @Override
    public String getId() {
        return null;
    }

    /**
     * @return
     */
    @Override
    public String getFullyQualifiedName() {
        return name;
    }

    @Override
    public List<ColumnDef> getColumnDefs() {

        return meta.getColumnDefs();

    }

    @Override
    public ColumnDef getColumnDef(String columnName) {
        return meta.getColumnDef(columnName);
    }

    @Override
    public ColumnDef getColumnDef(Integer columnIndex) {
        return getColumnDefs().get(columnIndex);
    }

    @Override
    public ColumnDef getColumnOf(String columnName) {
        return meta.getColumnOf(columnName);
    }

    public DataDef addColumn(String columnName) {
        meta.addColumn(columnName);
        return this;
    }

    public DataDef addColumn(String columnName, int type) {
        meta.addColumn(columnName, type);
        return this;
    }

    public DataDef addColumn(String columnName, int type, int precision) {
        meta.addColumn(columnName, type, precision);
        return this;
    }

    public DataDef addColumn(String columnName, int type, boolean nullable) {
        meta.addColumn(columnName, type, nullable);
        return this;
    }

    public DataDef addColumn(String columnName, int type, int precision, int scale) {
        meta.addColumn(columnName, type, precision, scale);
        return this;
    }

    public DataDef addColumn(String columnName, int type, int precision, boolean nullable) {
        meta.addColumn(columnName, type, precision, nullable);
        return this;
    }

    public Object getProperty(String key) {
        return properties.get(key);
    }

    public Object addProperty(String key, Object value) {
        return properties.put(key,value);
    }
}
