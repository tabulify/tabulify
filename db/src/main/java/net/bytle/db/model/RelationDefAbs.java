package net.bytle.db.model;

import net.bytle.db.spi.DataPath;

import java.util.List;

/**
 * Abstract class that implements columns structure
 */
public abstract class RelationDefAbs implements RelationDef {


    protected final DataPath dataPath;
    protected RelationMeta meta = new RelationMeta(this);

    public RelationDefAbs(DataPath dataPath) {
        this.dataPath  = dataPath;
    }



    @Override
    public DataPath getDataPath() {
        return dataPath;
    }

    @Override
    public List<ColumnDef> getColumnDefs() {
        return meta.getColumnDefs();
    }

    @Override
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
     * The columns must exist otherwise you get a exception
     */
    protected ColumnDef[] getColumns(String... columnNames) {

        return meta.getColumns(columnNames);
    }

    /**
     * @param columnNames
     * @return an array of columns
     * The columns must exist otherwise you get a exception
     */
    protected ColumnDef[] getColumns(List<String> columnNames) {

        return meta.getColumns(columnNames.toArray(new String[0]));
    }



}
