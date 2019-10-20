package net.bytle.db.model;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class PrimaryKeyDef {

    private final TableDef tableDef;
    private String name;
    private String[] columnNames;

    public static PrimaryKeyDef of(TableDef tableDef, String... columnNames) {
        assert tableDef != null;
        assert columnNames.length > 0;

        return new PrimaryKeyDef(tableDef,columnNames);
    }

    public String getName() {
        return name;
    }

    /**
     * Use {@link #PrimaryKeyDef(TableDef, String...)}
     */
    PrimaryKeyDef(){
        throw new RuntimeException("Don't use this");
    }

    private PrimaryKeyDef(TableDef tableDef, String... columnNames) {
        this.columnNames = columnNames;
        this.tableDef = tableDef;
    }

    public TableDef getDataDef() {
        return tableDef;
    }

    public PrimaryKeyDef setName(String name) {
        this.name = name;
        return this;
    }


    public List<ColumnDef> getColumns() {
        return Arrays.stream(columnNames)
                .map(tableDef::getColumnDef)
                .collect(Collectors.toList());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PrimaryKeyDef that = (PrimaryKeyDef) o;
        return tableDef.getDataPath().equals(that.tableDef.getDataPath()) &&
                Arrays.equals(columnNames, that.columnNames);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(tableDef.getDataPath().toString());
        result = 31 * result + Arrays.hashCode(columnNames);
        return result;
    }
}
