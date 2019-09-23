package net.bytle.db.model;

import net.bytle.db.database.Database;

import java.util.List;

/**
 * No setter because of chaining initialization please
 *
 *   * A query
 *   * A table
 */
public interface RelationDef {


    Database getDatabase();

    SchemaDef getSchema();

    String getName();

    /**
     * Fully Qualified name in Bytle Db (ie with the database Name)
     *
     * @return
     */
    String getId();

    /**
     * Full qualified name inside the database scope (ie in a sql)
     *
     * @return
     */
    String getFullyQualifiedName();

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


}
