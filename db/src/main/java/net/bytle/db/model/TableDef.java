package net.bytle.db.model;


import net.bytle.db.spi.DataPath;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by gerard on 01-02-2016.
 * A class that contains the data structure definition
 * <p>
 * A table can be:
 * * "SQL TABLE",
 * * "SQL VIEW"
 * * "CSV"
 *
 * <p>
 * A table definition may be created:
 * * manually
 * * or through the metadata of the driver of a result set (in a sql database)
 * * or through a data def file
 * * or manually via code
 */
public class TableDef implements RelationDef  {

    protected final DataPath dataPath;
    protected RelationMeta meta = new RelationMeta(this);


    private PrimaryKeyDef primaryKeyDef;

    /**
     * Table Property that can be used by other type of relation
     */
    private HashMap<String,Object> properties = new HashMap<>();

    /**
     * The identity string is for now the name of the foreign key
     * TODO ? but it would be better to implement on the column names
     * because not all foreign keys have a name (for instance Sqlite)
     */
    private HashMap<String, ForeignKeyDef> foreignKeys = new HashMap<>();


    private Set<UniqueKeyDef> uniqueKeys = new HashSet<>();

    // Not used but we keep it because there is some doc
    // on the setter
    private String tableType;

    public TableDef(DataPath dataPath) {
        this.dataPath = dataPath;
    }

    public static TableDef of(DataPath dataPath) {
        return new TableDef(dataPath);
    }


    public PrimaryKeyDef getPrimaryKey() {
        return primaryKeyDef;
    }


    public List<ForeignKeyDef> getForeignKeys() {

        return new ArrayList<>(foreignKeys.values());

    }


    /**
     * Return the foreign Key for this primary key and this set of columns
     * If the key is already defined
     *
     * @param columnNames    the columns on the table
     * @param primaryKeyDef the foreign primary key
     * @return the foreignKeyDef
     */
    public ForeignKeyDef foreignKeyOf(PrimaryKeyDef primaryKeyDef, String... columnNames) {

        assert primaryKeyDef!=null;
        assert columnNames.length>0;

        // if the foreign key exist already, return it
        for (ForeignKeyDef foreignKeyDef : getForeignKeys()) {
            final PrimaryKeyDef foreignPrimaryKey = foreignKeyDef.getForeignPrimaryKey();
            if (foreignPrimaryKey!=null) {
                if (foreignPrimaryKey.equals(primaryKeyDef)) {
                    final List<String> childColumns = foreignKeyDef.getChildColumns().stream()
                            .map(s->s.getColumnName())
                            .collect(Collectors.toList());
                    if (childColumns.equals(Arrays.asList(columnNames))) {
                        return foreignKeyDef;
                    }
                }
            }
        }

        final String fkName = dataPath.getName() + "_fk" + foreignKeys.size();
        List<ColumnDef> columnDefs = Arrays.asList(columnNames).stream()
                .map(this::getColumnDef)
                .collect(Collectors.toList());

        ForeignKeyDef foreignKeyDef = ForeignKeyDef.of(primaryKeyDef,columnDefs)
                .setName(fkName);

        this.foreignKeys.put(fkName, foreignKeyDef);
        return foreignKeyDef;

    }




    public List<UniqueKeyDef> getUniqueKeys() {
        return new ArrayList(uniqueKeys);
    }



    /**
     * The JDBC table type
     * <p>
     * "TABLE", "VIEW", "SYSTEM TABLE", "GLOBAL TEMPORARY", "LOCAL TEMPORARY", "ALIAS", "SYNONYM"
     * <p>
     * Constant are also available inside this class
     *
     * @param tableType
     * @return
     */
    public TableDef JdbcType(String tableType) {
        this.tableType = tableType;
        return this;
    }

    /**
     * Get and Create function
     *
     * @param columnDefs
     * @return
     */
    public UniqueKeyDef getOrCreateUniqueKey(ColumnDef... columnDefs) {

        UniqueKeyDef uniqueKeyDefToReturn = null;
        for (UniqueKeyDef uniqueKeyDef : uniqueKeys) {

            if (uniqueKeyDef.getColumns().equals(Arrays.asList(columnDefs))) {
                uniqueKeyDefToReturn = uniqueKeyDef;
            }

        }
        if (uniqueKeyDefToReturn == null) {
            uniqueKeyDefToReturn = UniqueKeyDef.of(this)
                    .addColumns(Arrays.asList(columnDefs));
            uniqueKeys.add(uniqueKeyDefToReturn);
        }
        return uniqueKeyDefToReturn;


    }

    /**
     * Shortcut to add two columns as primary key
     *
     * @param columnNames - The column name of the primary key
     * @return - the table Def for a chaining initialization
     */
    public TableDef setPrimaryKey(String... columnNames) {

        this.primaryKeyOf(columnNames);
        return this;

    }


    /**
     * Add a unique key
     *
     * @param columnNames
     * @return the table def for chaining initialization
     */
    public TableDef addUniqueKey(String... columnNames) {
        getOrCreateUniqueKey(getColumns(columnNames));
        return this;
    }



    /**
     * Add a foreign key
     *
     * @param primaryKeyDef
     * @param columnNames
     * @return the table for initialization chaining
     */
    public TableDef addForeignKey(PrimaryKeyDef primaryKeyDef, String... columnNames) {
        try {
            foreignKeyOf(primaryKeyDef, columnNames);
        } catch (Exception e) {
            throw new RuntimeException("A problem occurs when trying to add a foreign to the table (" + this + ") towards the table (" + primaryKeyDef.getDataDef().getDataPath() + "). See the message below.", e);
        }
        return this;
    }

    /**
     * Add a foreign key
     *
     * @param primaryKeyDef
     * @param columnNames
     * @return the table for initialization chaining
     */
    public TableDef addForeignKey(PrimaryKeyDef primaryKeyDef, List<String> columnNames) {
        try {
            foreignKeyOf(primaryKeyDef, columnNames.toArray(new String[0]));
        } catch (Exception e) {
            throw new RuntimeException("A problem occurs when trying to add a foreign to the table (" + this + ") towards the table (" + primaryKeyDef.getDataDef().getDataPath() + "). See the message below.", e);
        }
        return this;
    }

    /**
     *
     * @param dataPath - the foreign primary table
     * @param columnNames - the column names of this tables
     * @return the tableDef for chaining initialization
     */
    public TableDef addForeignKey(DataPath dataPath, String... columnNames) {
        this.foreignKeyOf(dataPath.getDataDef().getPrimaryKey(), columnNames);
        return this;
    }


    public TableDef setPrimaryKey(List<String> columnNames) {
        primaryKeyOf(columnNames.toArray(new String[0]));
        return this;
    }


    public TableDef addColumn(String columnName) {
        meta.addColumn(columnName);
        return this;
    }

    public TableDef addColumn(String columnName, int type) {
        meta.addColumn(columnName, type);
        return this;
    }

    public TableDef addColumn(String columnName, int type, int precision) {
        meta.addColumn(columnName, type, precision);
        return this;
    }

    public TableDef addColumn(String columnName, int type, boolean nullable) {
        meta.addColumn(columnName, type, nullable);
        return this;
    }

    public TableDef addColumn(String columnName, int type, int precision, int scale) {
        meta.addColumn(columnName, type, precision, scale);
        return this;
    }

    public TableDef addColumn(String columnName, int type, int precision, boolean nullable) {
        meta.addColumn(columnName, type, precision, nullable);
        return this;
    }


    public void deleteForeignKey(ForeignKeyDef foreignKeyDef) {

        foreignKeyDef = foreignKeys.remove(foreignKeyDef.getName());
        if (foreignKeyDef == null) {

            throw new RuntimeException("The foreign key (" + foreignKeyDef.getName() + ") does not belong to the table (" + this + ") and could not be removed");
        }

    }


    /**
     * Property value are generally given via a {@link DataDefs data definition file}
     * @param key
     * @return the property value of this table def
     */
    public Object getProperty(String key) {
        return properties.get(key);
    }

    /**
     * Add a property for this table def
     * @param key
     * @param value
     * @return the tableDef for initialization chaining
     */
    public TableDef addProperty(String key, Object value) {
        properties.put(key,value);
        return this;
    }

    /**
     * Property value are generally given via a {@link DataDefs data definition file}
     * @return the properties value of this table def
     */
    public Map<String, Object> getProperties() {
        return properties;
    }


    public ForeignKeyDef foreignKeyOf(PrimaryKeyDef primaryKey, List<String> columns) {

        return null;
    }

    public PrimaryKeyDef primaryKeyOf(String... columnNames) {
        assert columnNames.length > 0;

        this.primaryKeyDef = PrimaryKeyDef.of(this,columnNames);
        return this.primaryKeyDef;
    }




    public DataPath getDataPath() {
        return dataPath;
    }


    public List<ColumnDef> getColumnDefs() {
        return meta.getColumnDefs();
    }


    public <T> ColumnDef<T> getColumnDef(String columnName) {
        return meta.getColumnDef(columnName);
    }

    /**
     * @param columnIndex
     * @return a columnDef by index starting at 0
     */
    public ColumnDef getColumnDef(Integer columnIndex) {

        return getColumnDefs().get(columnIndex);
    }


    /**
     * @param columnName - The column name
     * @param clazz - The type of the column (Java needs the type to be a sort of type safe)
     * @return  a new columnDef
     */
    public <T> ColumnDef<T> getColumnOf(String columnName, Class<T> clazz) {

        return meta.getColumnOf(columnName, clazz);

    }

    /**
     * @param columnNames
     * @return an array of columns
     * The columns must exist otherwise you of a exception
     */
    protected ColumnDef[] getColumns(String... columnNames) {

        return meta.getColumns(columnNames);
    }

    /**
     * @param columnNames
     * @return an array of columns
     * The columns must exist otherwise you of a exception
     */
    protected ColumnDef[] getColumns(List<String> columnNames) {

        return meta.getColumns(columnNames.toArray(new String[0]));
    }

}
