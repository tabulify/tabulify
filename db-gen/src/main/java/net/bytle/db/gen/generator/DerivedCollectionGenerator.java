package net.bytle.db.gen.generator;

import jdk.nashorn.api.scripting.ScriptObjectMirror;
import net.bytle.db.gen.GenColumnDef;
import net.bytle.type.Typess;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import java.sql.Date;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;


public class DerivedCollectionGenerator<T> implements CollectionGeneratorOnce<T> {


  private static final Logger LOGGER = LoggerFactory.getLogger(DerivedCollectionGenerator.class);

  private static final ScriptEngine engine;

  static {
    ScriptEngineManager mgr = new ScriptEngineManager();
    engine = mgr.getEngineByName("nashorn"); // name may be also "Javascript"
  }

  private final CollectionGeneratorOnce parentCollectionGenerator;
  private final String formula;
  private final GenColumnDef<T> columnDef;
  private final Class<T> clazz;
  private Object actualValue;


  public DerivedCollectionGenerator(GenColumnDef<T> columnDef, CollectionGenerator parentDataCollectionGenerator, String formula) {

    if (!(parentDataCollectionGenerator instanceof CollectionGeneratorOnce)){
      throw new RuntimeException("The parent generator ("+parentDataCollectionGenerator+") should only generate data for one column. Derived data generation are not supported");
    }
    clazz = columnDef.getClazz();
    this.columnDef = columnDef;
    this.parentCollectionGenerator = (CollectionGeneratorOnce) parentDataCollectionGenerator;
    this.formula = formula;
    if (formula == null) {
      throw new RuntimeException("The formula for the column " + columnDef.getFullyQualifiedName() + " is null");
    }

  }


  /**
   * @return a new generated data object every time it's called
   */
  @Override
  public T getNewValue() {

    Object derivedActualValue = parentCollectionGenerator.getActualValue();
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
      if (evalValue == null) {
        final String msg = "The derived generator for the column (" + columnDef.getFullyQualifiedName() + ") has returned a NULL value and it's not expected.\nThe formula was: " + evalScript;
        LOGGER.error(msg);
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
        if (this.clazz.equals(Integer.class)){
          this.actualValue = ((Double) evalValue).intValue();
        } else {
          this.actualValue = evalValue;
        }
      }
      return clazz.cast(this.actualValue);
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
  public GenColumnDef getColumn() {
    return columnDef;
  }


  @Override
  public Long getMaxGeneratedValues() {
    return parentCollectionGenerator.getMaxGeneratedValues();
  }


  /**
   * Build a derived data generator from properties (got from a tableDef that was created with a data definition file)
   *
   * @return a data generator for chaining
   */
  static public <T> DerivedCollectionGenerator<T> of(GenColumnDef<T> columnDef) {

    // Parent Generator
    final String columnParentKeyProperty = "ColumnParent";
    String columnParentName = Typess.safeCast(columnDef.getProperty(columnParentKeyProperty), String.class);
    if (columnParentName == null) {
      throw new IllegalArgumentException("The parent column is not defined in the '" + columnParentKeyProperty + "' properties for the column " + columnDef.getFullyQualifiedName());
    }
    GenColumnDef columnParent = columnDef.getDataDef().getColumnDef(columnParentName);
    if (columnDef.equals(columnParent)) {
      throw new RuntimeException("The column (" + columnDef.getFullyQualifiedName() + " has a derived generator and derived from itself creating a loop. Please choose another column as derived (parent) column.");
    }
    CollectionGenerator generator = columnParent.getGenerator();
    CollectionGeneratorOnce parentCollectionGenerator;
    if (generator instanceof CollectionGeneratorOnce) {
      parentCollectionGenerator = (CollectionGeneratorOnce) generator;
    } else {
      throw new RuntimeException("Derived generator are working only with a scalar generator. The generator (" + generator + ") generates values for a pair of columns");
    }

    // Formula
    String formula = Typess.safeCast(columnDef.getProperty("formula"), String.class);
    if (formula == null) {
      throw new RuntimeException("The 'formula' property is mandatory to create a derived data generator and is missing for the column (" + columnDef.getFullyQualifiedName() + ")");
    }

    // New Instance
    return new DerivedCollectionGenerator<>(columnDef, parentCollectionGenerator, formula);
  }


  public CollectionGeneratorOnce getParentGenerator() {
    return parentCollectionGenerator;
  }
}
