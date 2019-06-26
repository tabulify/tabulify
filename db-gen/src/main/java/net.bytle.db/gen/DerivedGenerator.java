package net.bytle.db.gen;

import net.bytle.db.model.ColumnDef;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import java.sql.Date;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;


public class DerivedGenerator implements DataGenerator {


    private static final ScriptEngine engine;

    static {
        ScriptEngineManager mgr = new ScriptEngineManager();
        engine = mgr.getEngineByName("nashorn"); // name may be also "Javascript"
    }

    private final DataGenerator dataGenerator;
    private final String formula;
    private final ColumnDef columnDef;
    private Object actualValue;


    public DerivedGenerator(ColumnDef columnDef, DataGenerator parentDataGenerator, String formula) {

        this.columnDef = columnDef;
        this.dataGenerator = parentDataGenerator;
        this.formula = formula;

    }

    /**
     * @return a new generated data object every time it's called
     */
    @Override
    public Object getNewValue() {

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
            this.actualValue = engine.eval(evalScript);
            return this.actualValue;
        } catch (ScriptException e) {

            throw new RuntimeException(evalScript,e);
        }

    }

    /**
     * @return a generated value (used in case of derived data
     */
    @Override
    public Object getActualValue() {
        return actualValue;
    }

    /**
     * @return the column attached to this generator
     */
    @Override
    public ColumnDef getColumn() {
        return columnDef;
    }

    /**
     * get a new value for a column
     *
     * @param columnDef
     * @return a new generated data object every time it's called
     */
    @Override
    public Object getNewValue(ColumnDef columnDef) {

        if (columnDef.equals(this.columnDef)) {
            return getNewValue();
        } else {
            throw new RuntimeException("Multiple column generator is not implemented");
        }

    }

    /**
     * get the actual value of a column
     *
     * @param columnDef
     * @return a generated value (used in case of derived data
     */
    @Override
    public Object getActualValue(ColumnDef columnDef) {

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


    public DataGenerator getParentGenerator() {
        return dataGenerator;
    }
}
