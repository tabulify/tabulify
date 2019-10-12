package net.bytle.db.model;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class PrimaryKeyDef {

    private final RelationDef relationDef;
    private String name;
    private String[] columnNames;

    public static PrimaryKeyDef of(RelationDef tableDef, String... columnNames) {
        assert tableDef != null;
        assert columnNames.length > 0;

        return new PrimaryKeyDef(tableDef,columnNames);
    }

    public String getName() {
        return name;
    }

    /**
     * Use {@link #PrimaryKeyDef(RelationDef, String...)}
     */
    PrimaryKeyDef(){
        throw new RuntimeException("Don't use this");
    }

    private PrimaryKeyDef(RelationDef relationDef, String... columnNames) {
        this.columnNames = columnNames;
        this.relationDef = relationDef;
    }

    public RelationDef getRelationDef() {
        return relationDef;
    }

    public PrimaryKeyDef setName(String name) {
        this.name = name;
        return this;
    }


    public List<ColumnDef> getColumns() {
        return Arrays.stream(columnNames)
                .map(relationDef::getColumnDef)
                .collect(Collectors.toList());
    }



}
