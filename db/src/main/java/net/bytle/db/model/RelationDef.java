package net.bytle.db.model;

import net.bytle.db.spi.DataPath;

import java.util.List;

/**
 *
 * The structure of the data
 *
 */
public interface RelationDef {


    List<ColumnDef> getColumnDefs();

    /**
     *
     * @param columnName
     * @return a column def by its name
     */
    <T> ColumnDef<T> getColumnDef(String columnName);

    /**
     *
     * @param columnIndex
     * @return a column def by its index
     */
    <T> ColumnDef<T> getColumnDef(Integer columnIndex);

    /**
     *
     * @param columnName
     * @param clazz - The type of the column (Java needs the type to be a sort of type safe)
     * @return  a new columnDef
     */
    <T> ColumnDef<T> getColumnOf(String columnName, Class<T> clazz);

    DataPath getDataPath();
}
