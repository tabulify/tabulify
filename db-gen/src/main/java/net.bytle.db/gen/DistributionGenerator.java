package net.bytle.db.gen;


import net.bytle.db.database.DataTypeJdbc;
import net.bytle.db.database.JdbcDataType.DataTypesJdbc;
import net.bytle.db.model.ColumnDef;
import net.bytle.type.Maps;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static java.time.temporal.ChronoUnit.DAYS;


/**
 * Distribution Generator by default: random
 */
public class DistributionGenerator<T> implements DataGenerator<T> {

    private final Class<T> clazz;
    private final DataTypeJdbc type;
    private Object o;
    private ColumnDef columnDef;
    private Object min;
    private Object max;
    private Object range;
    private List<T> values;

    public DistributionGenerator(ColumnDef<T> columnDef) {

        this.columnDef = columnDef;
        clazz = columnDef.getClazz();
        type = DataTypesJdbc.ofClass(clazz);
        switch (type.getTypeCode()) {
            case (Types.DOUBLE):
                range = 10.0;
                min = 0.0;
                break;
            case Types.FLOAT:
                // Other name for double
                range = 10.0;
                min = 0.0;
                break;
            case Types.INTEGER:
                range = 10;
                min = 0;
                break;
            case Types.VARCHAR:
                o = getString();
                break;
            case Types.CHAR:
                o = getString();
                break;
            case Types.NUMERIC:
                range = BigDecimal.valueOf(10);
                min = BigDecimal.valueOf(0);
                break;
            case Types.DATE:
                o = Date.valueOf(LocalDate.now());
                range = 10;
                min = Date.valueOf(LocalDate.now().minusDays((int) range));
                max = Date.valueOf(LocalDate.now());
                break;
            case Types.TIMESTAMP:
                o = Timestamp.valueOf(LocalDateTime.now());
                range = 10;
                min = Timestamp.valueOf(LocalDateTime.now().minusDays((int) range));
                max = Timestamp.valueOf(LocalDateTime.now());
                break;
            default:
                throw new RuntimeException("The data type with the type code (" + type.getTypeCode() + "," + clazz.getSimpleName() + ") is not supported for the column " + columnDef.getFullyQualifiedName());

        }

    }

    public static <T> DistributionGenerator<T> of(ColumnDef<T> columnDef) {

        final DistributionGenerator<T> distributionGenerator = new DistributionGenerator<>(columnDef);

        Map<String, Object> generatorColumnProperties = DataGeneration.getProperties(columnDef);
        final Object bucketsObject = Maps.getPropertyCaseIndependent(generatorColumnProperties, "buckets");
        Map<T, Integer> buckets;
        try {
            buckets = (Map<T, Integer>) bucketsObject;
        } catch (ClassCastException e){
            throw new RuntimeException("The buckets definition of the column ("+columnDef.getFullyQualifiedName()+") are not in the map format <Object,Integer>. The values are: "+bucketsObject);
        }

        // DataType Check
        if (buckets != null) {
            Object o = buckets.entrySet().iterator().next().getKey();
            if (o.getClass() != columnDef.getDataType().getClazz()) {
                throw new RuntimeException("The data type of the key with the the value (" + o + ") in the buckets definition of the column " + columnDef.getFullyQualifiedName() + " is not a " + columnDef.getDataType().getClazz().getSimpleName() + " but a " + o.getClass().getSimpleName() + ".");
            }
            distributionGenerator.setBuckets(buckets);
        }
        return distributionGenerator;
    }


    private String getString() {
        Integer precision = this.columnDef.getPrecision();
        String s = "hello";
        if (s.length() > precision) {
            s = s.substring(0, precision);
        }
        return s;
    }

    /**
     * @return a new generated data object every time it's called
     */
    @Override
    public T getNewValue() {

        if (values == null) {
            switch (type.getTypeCode()) {
                case Types.DOUBLE:
                    o = Math.random() * (Double) range;
                    if (min != null) {
                        o = (Double) o + (Double) min;
                    }
                    break;
                case Types.INTEGER:
                    o = (int) (Math.random() * (int) range);
                    if (min != null) {
                        o = (int) o + (int) min;
                    }
                    break;
                case Types.NUMERIC:
                    o = BigDecimal.valueOf(Math.random() * ((BigDecimal) range).doubleValue());
                    if (min != null) {
                        o = ((BigDecimal) o).add(((BigDecimal) min));
                    }
                    break;
                case Types.DATE:
                    int i = (int) (Math.random() * (int) range);
                    LocalDate localValue = ((Date) min).toLocalDate();
                    o = Date.valueOf(localValue.plusDays(i));
                    break;
                case Types.TIMESTAMP:
                    int iTimestamp = (int) (Math.random() * (int) range);
                    LocalDateTime localValueTimestamp = ((Timestamp) min).toLocalDateTime();
                    o = Timestamp.valueOf(localValueTimestamp.plusDays(iTimestamp));
                    break;

            }

        } else {

            int i = (int) (Math.random() * values.size());
            o = values.get(i);

        }


        return clazz.cast(o);
    }

    /**
     * @return a generated value (used in case of derived data
     */
    @Override
    public T getActualValue() {
        return clazz.cast(o);
    }

    /**
     * @return the column attached to this generator
     * It permits to create parent relationship between generators
     * when asking a value for a column, we may need to ask the value for another column before
     */
    @Override
    public ColumnDef getColumn() {
        return columnDef;
    }

    /**
     * of a new value for a column
     *
     * @param columnDef
     * @return a new generated data object every time it's called
     */
    @Override
    public T getNewValue(ColumnDef columnDef) {
        if (columnDef.equals(this.columnDef)) {
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
    public T getActualValue(ColumnDef columnDef) {
        if (columnDef.equals(this.columnDef)) {
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
        columnDefs.add(columnDef);
        return columnDefs;
    }

    @Override
    public Double getMaxGeneratedValues() {
        return Double.valueOf(Integer.MAX_VALUE);
    }


    /**
     * The buckets defines the distribution of discrete variable
     * where:
     * * The discrete variable are in the first Object variable
     * * The ratios are in the second Integer variable
     * <p>
     * Example with the following, you will of 2 times much Red than Blue and Green
     * Blue: 1
     * Red: 2
     * Green: 1
     *
     * @param buckets
     * @return
     */
    public DistributionGenerator<T> setBuckets(Map<T, Integer> buckets) {
        if (buckets != null) {
            if (buckets.size() > 0) {
                // Create the values list and add the element according to their ratio
                // A ratio of 3 = 3 elements in the list
                values = new ArrayList<T>();
                for (Map.Entry<T, Integer> entry : buckets.entrySet()) {
                    for (int i = 0; i < entry.getValue(); i++) {
                        values.add(entry.getKey());
                    }
                }
            }
        }
        return this;
    }

    public DistributionGenerator<T> setMin(T min) {
        if (min != null) {
            this.min = min;
            updateRange();
        }
        return this;
    }

    public DistributionGenerator<T> setMax(T max) {
        if (max != null) {
            this.max = max;
            updateRange();
        }

        return this;
    }

    private void updateRange() {
        // Range
        switch (columnDef.getDataType().getTypeCode()) {
            case Types.DOUBLE:
                if (max != null) {
                    range = (double) max - (double) min;
                }
                break;
            case Types.INTEGER:
                if (max != null) {
                    range = (int) max - (int) min;
                }
                break;
            case Types.NUMERIC:
                if (max != null) {
                    range = ((BigDecimal) max).min((BigDecimal) min);
                }
                break;
            case Types.DATE:
                if (max != null) {
                    range = (int) DAYS.between(((Date) min).toLocalDate(), ((Date) max).toLocalDate());
                }
                break;

        }
    }

    @Override
    public String toString() {
        return "DistributionGenerator{" + columnDef + '}';
    }

}
