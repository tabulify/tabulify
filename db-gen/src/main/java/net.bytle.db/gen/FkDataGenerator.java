package net.bytle.db.gen;


import net.bytle.db.model.ColumnDef;
import net.bytle.db.model.ForeignKeyDef;
import net.bytle.db.spi.Tabulars;
import net.bytle.db.stream.SelectStream;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;


public class FkDataGenerator implements DataGenerator {


    private final ForeignKeyDef foreignKeyDef;
    private final ColumnDef foreignColumnDef;

    private Object value;
    private List<Object> values = new ArrayList<>();

    /**
     * Get a random foreign value when the {@link #getNewValue()} is called
     */
    public FkDataGenerator(ForeignKeyDef foreignKeyDef) {

        this.foreignKeyDef = foreignKeyDef;

        // Building the map of value
        foreignColumnDef = foreignKeyDef.getForeignPrimaryKey().getColumns().get(0);
        try (
                SelectStream selectStream = Tabulars.getSelectStream(foreignKeyDef.getForeignPrimaryKey().getDataDef().getDataPath())
        ) {

            while (selectStream.next()) {
                values.add(selectStream.getObject(foreignColumnDef.getColumnName()));
            }
            if (values.size() == 0) {
                throw new RuntimeException("The foreign table (" + foreignColumnDef.getRelationDef().getDataPath().toString() + ") has no data for the column (" + foreignKeyDef.getChildColumns().get(0) + ")");
            }
        }

    }

    /**
     * @return a new generated data object every time it's called
     */
    @Override
    public Object getNewValue() {
        int i = (int) (Math.random() * values.size());
        value = values.get(i);
        return value;
    }

    /**
     * @return a generated value (used in case of derived data
     */
    @Override
    public Object getActualValue() {
        return value;
    }

    /**
     * @return the column attached to this generator
     * It permits to create parent relationship between generators
     * when asking a value for a column, we may need to ask the value for another column before
     */
    @Override
    public ColumnDef getColumn() {
        return foreignKeyDef.getChildColumns().get(0);
    }

    /**
     * of a new value for a column
     *
     * @param columnDef
     * @return a new generated data object every time it's called
     */
    @Override
    public Object getNewValue(ColumnDef columnDef) {

        if (columnDef.equals(foreignColumnDef)) {
            return getNewValue();
        } else {
            throw new RuntimeException("Multiple column generator is not implemented");
        }

    }

    /**
     * of the actual value of a column
     *
     * @param columnDef
     * @return a generated value (used in case of derived data
     */
    @Override
    public Object getActualValue(ColumnDef columnDef) {

        if (columnDef.equals(foreignColumnDef)) {
            return getActualValue();
        } else {
            throw new RuntimeException("Multiple column generator is not implemented");
        }
    }

    /**
     * @return the columns attached to this generator
     */
    @Override
    public List<ColumnDef> getColumns() {
        List<ColumnDef> columnDefs = new ArrayList<>();
        columnDefs.add(foreignColumnDef);
        return columnDefs;
    }

    @Override
    public Double getMaxGeneratedValues() {
        return Double.valueOf(Integer.MAX_VALUE);
    }
}
