package com.tabulify.gen.generator;


import com.tabulify.gen.DataGenType;
import com.tabulify.gen.GenColumnDef;
import net.bytle.exception.CastException;
import net.bytle.exception.NoColumnException;
import net.bytle.type.Casts;
import net.bytle.type.Strings;
import net.bytle.type.time.Timestamp;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.EvaluatorException;
import org.mozilla.javascript.ScriptableObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

import static com.tabulify.gen.generator.ExpressionArgument.*;


public class ExpressionGenerator<T> extends CollectionGeneratorAbs<T> implements CollectionGenerator<T>, java.util.function.Supplier<T> {

  public static final DataGenType TYPE = DataGenType.EXPRESSION;

  private static final Logger LOGGER = LoggerFactory.getLogger(ExpressionGenerator.class);


  private static final Context cx;

  private static final ScriptableObject scope;

  static {
    //https://rhino.github.io/docs/scopes_and_contexts/
    cx = Context.enter();
    scope = cx.initStandardObjects();
  }

  private final List<CollectionGenerator<?>> parentCollectionGenerators = new ArrayList<>();
  private final String expression;
  private Object actualValue;


  /**
   * @param clazz                          - the return type
   * @param expression                     - the expression
   * @param parentDataCollectionGenerators - the column that compose the expression.
   *                                       Note that we may discover them from the expression but
   *                                       as we create a graph to build the column in order
   *                                       this is still mandatory
   */
  public ExpressionGenerator(Class<T> clazz, String expression, List<CollectionGenerator<?>> parentDataCollectionGenerators) {
    super(clazz);

    this.parentCollectionGenerators.addAll(parentDataCollectionGenerators);


    this.expression = expression;
    if (expression == null) {
      throw new RuntimeException("The expression is null");
    }

  }

  /**
   * Instantiate an expression generator from the columns properties
   * This function is called via recursion by the function {@link GenColumnDef#getOrCreateGenerator()}
   * Don't delete
   */
  public static <T> ExpressionGenerator<T> createFromProperties(Class<T> tClass, GenColumnDef<T> genColumnDef) {

    Map<ExpressionArgument, Object> argumentMap = genColumnDef.getDataSupplierArgument(ExpressionArgument.class);
    // Parent Generator
    Object columnParentsValue = argumentMap.get(ExpressionArgument.COLUMN_VARIABLES);
    Object columnParentValue = argumentMap.get(ExpressionArgument.COLUMN_VARIABLE);
    if (columnParentsValue == null && columnParentValue == null) {
      throw new IllegalArgumentException("A " + COLUMN_VARIABLES + " or " + COLUMN_VARIABLE + " attribute was not found. It is mandatory in the data supplier of the column " + genColumnDef.getFullyQualifiedName());
    }
    if (columnParentsValue != null && columnParentValue != null) {
      throw new IllegalArgumentException("The " + COLUMN_VARIABLES + " or " + COLUMN_VARIABLE + " attributes have values. Only one of the 2 attributes should be present in the data supplier of the column " + genColumnDef.getFullyQualifiedName());
    }

    List<String> columnParentNames = new ArrayList<>();
    if (columnParentsValue != null) {
      try {
        columnParentNames = Casts.castToNewList(columnParentsValue, String.class);
      } catch (CastException e) {
        throw new IllegalArgumentException("The " + COLUMN_VARIABLES + " value of the column " + genColumnDef.getFullyQualifiedName() + " is not a list. Error: " + e.getMessage(), e);
      }
    }
    if (columnParentValue != null) {
      columnParentNames.add(columnParentValue.toString());
    }

    List<CollectionGenerator<?>> parentCollectionGenerator = new ArrayList<>();
    for (String columnParentName : columnParentNames) {
      GenColumnDef<?> columnParent;
      try {
        columnParent = genColumnDef.getGenRelationDef().getColumnDef(columnParentName);
      } catch (NoColumnException e) {
        throw new IllegalStateException("The parent column (" + columnParentName + ") was not found and is mandatory to create an expression generator for the column (" + genColumnDef + ")");
      }
      if (genColumnDef.equals(columnParent)) {
        throw new RuntimeException("The column (" + genColumnDef + ") has a expression generator with itself creating a loop. Please choose another column as parent column.");
      }
      CollectionGenerator<?> parentGenerator = columnParent.getOrCreateGenerator();
      parentCollectionGenerator.add(parentGenerator);
    }

    // Expression
    Object expression = argumentMap.get(ExpressionArgument.EXPRESSION);
    if (expression == null) {
      throw new IllegalStateException("The '" + EXPRESSION + "' property is mandatory to create a expression data generator and is missing for the column (" + genColumnDef.getFullyQualifiedName() + ")");
    }

    // New Instance
    return (ExpressionGenerator<T>) (new ExpressionGenerator<>(tClass, expression.toString(), parentCollectionGenerator))
      .setColumnDef(genColumnDef);

  }


  /**
   * @return a new generated data object every time it's called
   */
  @Override
  public T getNewValue() {

    StringBuilder evalScript = new StringBuilder();
    for (CollectionGenerator<?> parentCollectionGenerator : parentCollectionGenerators) {
      Object parentValue = parentCollectionGenerator.getActualValue();
      String value = parentValue.toString();

      if (parentValue.getClass().equals(java.sql.Date.class)) {
        LocalDate actualDate = ((java.sql.Date) parentValue).toLocalDate();
        value = "new Date(\"" + actualDate.format(DateTimeFormatter.ISO_DATE) + "\")";
      } else if (parentValue.getClass().equals(String.class)) {
        value = "'" + value + "'";
      } else if (Arrays.asList(java.util.Date.class, java.sql.Timestamp.class).contains(parentValue.getClass())) {
        java.sql.Timestamp actualTimestamp = Timestamp.createFromObjectSafeCast(parentValue).toSqlTimestamp();
        String formatted = actualTimestamp.toLocalDateTime()
          .format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        value = "new Date(\"" + formatted + "\")";
      }

      /**
       * To lowercase in order to have a naming consistent
       * (ie ORACLE will return UPPER case column name if the column name in the `create` statement are not quoted)
       * because key should be idempotent, we put them in lower case
       */
      String variableName = parentCollectionGenerator.getColumnDef().getColumnName().toLowerCase();
      evalScript
        .append("var ")
        .append(variableName)
        .append(" = ")
        .append(value)
        .append(";\n");
    }

    // engine.eval load the script
    // on eval can load a function
    // and another can start the function
    evalScript.append(expression);


    try (Context cx = Context.enter()) {

      Object evalValue;
      try {
        evalValue = cx.evaluateString(scope, evalScript.toString(), "<cmd>", 1, null);
      } catch (Exception e) {
        throw new IllegalArgumentException("Error while evaluating the expression for the column " + this.getColumnDef() + ". \nError: " + e.getMessage() + ". \nExpression:\n" + evalScript, e);
      }
      if (evalValue == null) {
        final String msg = "The expression generator for the column (" + this.getColumnDef() + ") has returned a NULL value and it's not expected.\nThe expression was: " + evalScript;
        LOGGER.error(msg);
        throw new IllegalArgumentException(msg);
      }
      this.actualValue = evalValue;

      /**
       * Nan check
       */
      if (evalValue instanceof Double) {
        if (Double.isNaN((Double) evalValue)) {
          throw new IllegalArgumentException("NAN returned. Not a Number value found. Error while evaluating the expression for the column " + this.getColumnDef() + ". Expression:\n" + evalScript);
        }
      }

      /**
       * The substring because javascript return always a float for numbers,
       * then you may get 2020.0 in place of 2020 for instance
       */
      if (this.clazz.equals(String.class)) {
        String processString = Strings.createFromObjectNullSafe(evalValue).toString();
        GenColumnDef columnDef = this.getColumnDef();
        if (columnDef.getPrecision() != 0) {
          if (processString.length() > columnDef.getPrecisionOrMax()) {
            processString = processString.substring(0, columnDef.getPrecisionOrMax());
          }
        }
        this.actualValue = processString;
      }

      try {
        return Casts.cast(this.actualValue, clazz);
      } catch (CastException ex) {
        try {
          if (clazz.equals(java.sql.Date.class) || clazz.equals(java.sql.Timestamp.class)) {
            // Happens on formula
            // The object (org.mozilla.javascript.NativeDate@46678e49) has a class (NativeDate) that is not yet seen as a date.
            this.actualValue = Context.jsToJava(this.actualValue, java.util.Date.class);
            return Casts.cast(this.actualValue, clazz);
          }
          return clazz.cast(Context.jsToJava(this.actualValue, clazz));
        } catch (EvaluatorException | CastException e) {
          throw new RuntimeException("The value (" + this.actualValue + ") can not be cast to " + clazz, e);
        }

      }

    }

  }

  /**
   * @return a generated value
   */
  @Override
  public T getActualValue() {
    //noinspection unchecked
    return (T) actualValue;
  }


  @Override
  public Set<CollectionGenerator<?>> getDependencies() {
    return new HashSet<>(this.parentCollectionGenerators);
  }


  @Override
  public long getCount() {
    return parentCollectionGenerators
      .stream()
      .mapToLong(CollectionGenerator::getCount)
      .min()
      .orElse(Long.MAX_VALUE);
  }

  @Override
  public void reset() {
    parentCollectionGenerators.forEach(CollectionGenerator::reset);
  }


  public String getExpression() {
    return this.expression;
  }

  @Override
  public DataGenType getGeneratorType() {
    return DataGenType.EXPRESSION;
  }

  @Override
  public Boolean isNullable() {
    // not sure if you could return null
    return false;
  }

}
