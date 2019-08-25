package net.bytle.db.model;

import net.bytle.db.database.Database;

import java.util.List;

/**
 * No setter because of chaining initialization please
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
    ColumnDef getColumnDef(String columnName);

    /**
     *
     * @param columnIndex
     * @return a column def by its index
     */
    ColumnDef getColumnDef(Integer columnIndex);

    /**
     *
     * @param columnName
     * @param clazz - The type of the column (Java needs the type to be a sort of type safe)
     * @return  a new columnDef
     */
    ColumnDef getColumnOf(String columnName, Class clazz);


}
