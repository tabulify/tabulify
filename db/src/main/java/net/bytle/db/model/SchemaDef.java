package net.bytle.db.model;

import net.bytle.db.database.Database;
import net.bytle.regexp.Globs;

import java.sql.DatabaseMetaData;
import java.util.*;
import java.util.stream.Collectors;

/**
 *
 * Schema implementation
 *
 * See also:
 * {@link DatabaseMetaData#supportsSchemasInDataManipulation()}
 * {@link DatabaseMetaData#supportsSchemasInIndexDefinitions()}
 * {@link DatabaseMetaData#supportsSchemasInPrivilegeDefinitions()}
 * {@link DatabaseMetaData#supportsSchemasInProcedureCalls()}
 * {@link DatabaseMetaData#supportsSchemasInTableDefinitions()}
 *
 * {@link DatabaseMetaData#supportsCatalogsInDataManipulation()}
 * {@link DatabaseMetaData#supportsCatalogsInIndexDefinitions()}
 * {@link DatabaseMetaData#supportsCatalogsInPrivilegeDefinitions()}
 * {@link DatabaseMetaData#supportsCatalogsInProcedureCalls()}
 * {@link DatabaseMetaData#supportsCatalogsInTableDefinitions()}
 *
 */
public class SchemaDef {


    private final Database database;
    private String name;

    /**
     * Should be private. Can be called only from a database object
     * May be the database object can dedicate this task by creating the DbObjectBuilder
     *
     * @param database
     */
    public SchemaDef(Database database) {
        this.database = database;
    }

    /**
     * {@link DatabaseMetaData#getMaxSchemaNameLength()}
     *
     * @param name
     * @return
     */
    public SchemaDef name(String name) {
        this.name = name;
        return this;
    }

    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return this.getDatabase().getDatabaseName() + "." + getName();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        SchemaDef schemaDef = (SchemaDef) o;

        if (!database.equals(schemaDef.database)) return false;
        return name.equals(schemaDef.name);
    }

    @Override
    public int hashCode() {
        int result = database.hashCode();
        result = 31 * result + name.hashCode();
        return result;
    }

    public List<TableDef> getTables() {

        // Scan the database
        List<TableDef> tableDefs = database.getObjectBuilder().buildSchema(this);
        Collections.sort(tableDefs);
        return tableDefs;

    }

    public Database getDatabase() {
        return database;
    }


    public TableDef getTableOf(String tableName) {

        return database
                .getTable(tableName)
                .setSchema(this);

    }

    public List<TableDef> getTables(String... patterns) {

        return getTables(Arrays.asList(patterns));

    }

    public List<TableDef> getTables(List<String> globPatterns) {
        List<TableDef> tableDefList = new ArrayList<>();

        for (String globPattern : globPatterns) {
            String regexpPattern = Globs.toRegexPattern(globPattern);
            tableDefList.addAll(
                    getTables()
                            .stream()
                            .filter(s -> s.getName().matches(regexpPattern))
                            .collect(Collectors.toList())
            );
        }
        Collections.sort(tableDefList);
        return tableDefList;
    }

    /**
     * Retrieve the relationship (ie foreigns key and external key) of tables
     *
     * @param pattern - the name of a table or a pattern
     * @return
     */
    public List<ForeignKeyDef> getForeignKeys(String pattern) {
        Set<ForeignKeyDef> foreignKeys = new HashSet<>();
        for (TableDef tableDef : this.getTables(pattern)) {
            foreignKeys.addAll(tableDef.getForeignKeys());
            foreignKeys.addAll(tableDef.getExternalForeignKeys());
        }
        return foreignKeys.stream().collect(Collectors.toList());
    }

    public List<ForeignKeyDef> getForeignKeys() {
        return getForeignKeys("*");
    }



}
