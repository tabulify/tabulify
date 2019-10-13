package net.bytle.db.gen;

import jdk.nashorn.api.scripting.ScriptObjectMirror;
import net.bytle.cli.Log;
import net.bytle.db.model.ColumnDef;
import net.bytle.type.Maps;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import java.sql.Date;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;


public class DerivedGenerator<T> implements DataGenerator<T> {


    static final Log LOGGER = DataGeneration.GEN_LOG;

    private static final ScriptEngine engine;

    static {
        ScriptEngineManager mgr = new ScriptEngineManager();
        engine = mgr.getEngineByName("nashorn"); // name may be also "Javascript"
    }

    private final DataGenerator dataGenerator;
    private final String formula;
    private final ColumnDef<T> columnDef;
    private Object actualValue;


    public DerivedGenerator(ColumnDef<T> columnDef, DataGenerator parentDataGenerator, String formula) {

        this.columnDef = columnDef;
        this.dataGenerator = parentDataGenerator;
        this.formula = formula;
        if (formula==null){
            throw new RuntimeException("The formula for the column "+columnDef.getFullyQualifiedName()+" is null");
        }

    }


    /**
     * @return a new generated data object every time it's called
     */
    @Override
    public T getNewValue() {

        Object derivedActualValue = dataGenerator.getActualValue();
        String value = derivedActualValue.toString();

        if (derivedActualValue.getClass().equals(Date.class)) {

            LocalDate actualDate = ((Date) derivedActualValue).toLocalDate();
            value = "new Date(\"" + actualDate.format(DateTimeFormatter.ISO_DATE) + "\")";

        }

        // engine.eval load the script
        // on eval can load a function
        // and an other can start the function
        String evalScript = "var x = " + value + ";\n" + formula;

        try {
            Object evalValue = engine.eval(evalScript);
            if (evalValue==null){
                final String msg = "The derived generator for the column (" + columnDef.getFullyQualifiedName() + ") has returned a NULL value and it's not expected.\nThe formula was: " + evalScript;
                LOGGER.severe(msg);
                throw new RuntimeException(msg);
            }
            if (evalValue.getClass() == ScriptObjectMirror.class) {
                ScriptObjectMirror evalValueMirror = (ScriptObjectMirror) evalValue;
                if (evalValueMirror.getClassName().equals("Date")) {
                    // https://stackoverflow.com/questions/25385911/nashorn-nativedate-conversion-to-java-util-date
                    // js date returns timestamp in local time so you need to adjust it...
                    long timestampLocalTime = (long) (double) evalValueMirror.callMember("getTime");
                    // java.util.Date constructor utilizes UTC timestamp
                    int timezoneOffsetMinutes = (int) (double) evalValueMirror.callMember("getTimezoneOffset");
                    this.actualValue = new Date(timestampLocalTime + timezoneOffsetMinutes * 60 * 1000);
                } else {
                    this.actualValue = evalValue;
                }
            } else {
                this.actualValue = evalValue;
            }
            return (T) this.actualValue;
        } catch (ScriptException e) {
            throw new RuntimeException(evalScript, e);
        }

    }

    /**
     * @return a generated value (used in case of derived data
     */
    @Override
    public T getActualValue() {
        return (T) actualValue;
    }

    /**
     * @return the column attached to this generator
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
        return dataGenerator.getMaxGeneratedValues();
    }

    /**
     *
     * Build a derived data generator from properties (got from a tableDef that was created with a data definition file)
     * @param dataGeneration - The context object (giving access to the build method and other context method)
     * @return a data generator for chaining
     */
    static public <T> DerivedGenerator<T> of(ColumnDef<T> columnDef, DataGeneration dataGeneration) {

        Map<String, Object> properties = DataGeneration.getProperties(columnDef);
        // Parent Generator
        final String columnParentKeyProperty = "ColumnParent";
        String columnName = (String) Maps.getPropertyCaseIndependent(properties,columnParentKeyProperty);
        if (columnName == null) {
            throw new IllegalArgumentException("The parent column is not defined in the '" + columnParentKeyProperty + "' properties for the column " + columnDef.getFullyQualifiedName());
        }
        ColumnDef columnParent = columnDef.getRelationDef().getColumnDef(columnName);
        DataGenerator parentGenerator = dataGeneration.getDataGenerator(columnParent);
        if (parentGenerator == null) {
            if (columnDef.equals(columnParent)) {
                throw new RuntimeException("The column (" + columnDef.getFullyQualifiedName() + " has a derived generator and derived from itself creating a loop. Please choose another column as derived (parent) column.");
            }
            // Build it
            dataGeneration.buildDefaultDataGeneratorForColumn(columnParent);
        }
        parentGenerator = dataGeneration.getDataGenerator(columnParent);

        // Formula
        String formula = (String) Maps.getPropertyCaseIndependent(properties,"formula");
        if (formula==null){
            throw new RuntimeException("The 'formula' property is mandatory to create a derived data generator and is missing for the column ("+columnDef.getFullyQualifiedName()+")");
        }

        // New Instance
        return new DerivedGenerator<>(columnDef,parentGenerator,formula);
    }


    public DataGenerator getParentGenerator() {
        return dataGenerator;
    }
}
