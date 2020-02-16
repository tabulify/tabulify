package net.bytle.db.model;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class PrimaryKeyDef {

    private final RelationDef tableDef;
    private String name;
    private String[] columnNames;

    public static PrimaryKeyDef of(RelationDef tableDef, String... columnNames) {
        assert tableDef != null;
        assert columnNames.length > 0;
        assert columnNames[0]!=null:"A column name must not be null";

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

    private PrimaryKeyDef(RelationDef tableDef, String... columnNames) {
        this.columnNames = columnNames;
        this.tableDef = tableDef;
    }

    public RelationDef getDataDef() {
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

  @Override
  public String toString() {
    return "PrimaryKey of " + tableDef.getDataPath() + " (" + Arrays.toString(columnNames) +")";
  }
}
