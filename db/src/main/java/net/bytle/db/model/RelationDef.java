package net.bytle.db.model;

import net.bytle.db.database.Database;

import java.util.List;

/**
 * No setter because of chaining initialization please
 */
public interface RelationDef {

    int RELATION_TYPE_SQL = 0;
    int RELATION_TYPE_FILE = 1;


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

    ColumnDef getColumnDef(String columnName);

    ColumnDef getColumnDef(Integer columnIndex);


    ColumnDef getColumnOf(String columnName);


}
