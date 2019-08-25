package net.bytle.db.gen;

import net.bytle.db.model.TableDef;
import net.bytle.type.Maps;

import java.util.List;
import java.util.stream.Collectors;

/**
 * A data gen def is a wrapper around tableDef
 * that adds the {@link #getTotalRows()} and {@link #setTotalRows(Integer)} methods
 */
public class DataGenDef {


    /**
     * The total number of rows that the table should have
     */
    public static final String TOTAL_ROWS = "TotalRows";
    private final TableDef tableDef;

    public DataGenDef(TableDef tableDef) {
        this.tableDef = tableDef;
    }

    /**
     *
     * @return The total number of rows that the table must have
     */
    public Integer getTotalRows() {
        return (Integer) Maps.getPropertyCaseIndependent(tableDef.getProperties(), TOTAL_ROWS);
    }

    /**
     * @param rows The total number of rows that the table must have
     * @return this object
     */
    public DataGenDef setTotalRows(Integer rows) {
        tableDef.addProperty(TOTAL_ROWS, rows);
        return this;
    }

    static public DataGenDef get(TableDef tableDef){
        return new DataGenDef(tableDef);
    }

    public TableDef getTableDef() {
        return this.tableDef;
    }

    public DataGenDefColumnDef getDataGenColumnDef(String columnName) {
        return DataGenDefColumnDef.get(tableDef.getColumnDef(columnName));
    }

    public List<DataGenDefColumnDef> getDataGenColumnDefs() {
        return tableDef.getColumnDefs()
                .stream()
                .map(s->DataGenDefColumnDef.get(s))
                .collect(Collectors.toList());
    }
}
