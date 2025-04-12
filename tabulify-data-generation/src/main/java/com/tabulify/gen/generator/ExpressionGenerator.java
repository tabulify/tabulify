package com.tabulify.gen.generator;


import com.tabulify.gen.GenColumnDef;
import net.bytle.exception.CastException;
import net.bytle.exception.NoColumnException;
import net.bytle.type.Casts;
import net.bytle.type.Strings;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ScriptableObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Date;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

import static com.tabulify.gen.GenColumnDef.GENERATOR_PROPERTY_KEY;


public class ExpressionGenerator<T> extends CollectionGeneratorAbs<T> implements CollectionGenerator<T>, java.util.function.Supplier<T> {

  public static final String TYPE = "expression";

  private static final Logger LOGGER = LoggerFactory.getLogger(ExpressionGenerator.class);


  public static final String EXPRESSION_PROPERTY = "expression";
  public static final String COLUMN_PARENT_PROPERTY = "ColumnParents";

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
   * This function is called via recursion by the function {@link GenColumnDef#getOrCreateGenerator(Class)}
   * Don't delete
   */
  public static <T> ExpressionGenerator<T> createFromProperties(Class<T> tClass, GenColumnDef genColumnDef) {

    // Parent Generator
    Object columnParentsValue = genColumnDef.getVariable(Object.class, GENERATOR_PROPERTY_KEY, COLUMN_PARENT_PROPERTY);
    if (columnParentsValue == null) {
      throw new IllegalStateException("The parent column is not defined in the '" + COLUMN_PARENT_PROPERTY + "' properties for the column " + genColumnDef.getFullyQualifiedName());
    }
    List<String> columnParentNames = new ArrayList<>();
    if (columnParentsValue instanceof Collection) {
      columnParentNames = Casts.castToListSafe(columnParentsValue, String.class);
    } else {
      columnParentNames.add(columnParentsValue.toString());
    }

    List<CollectionGenerator<?>> parentCollectionGenerator = new ArrayList<>();
    for (String columnParentName : columnParentNames) {
      GenColumnDef columnParent;
      try {
        columnParent = genColumnDef.getRelationDef().getColumnDef(columnParentName);
      } catch (NoColumnException e) {
        throw new IllegalStateException("The parent column (" + columnParentName + ") was not found and is mandatory to create an expression generator for the column (" + genColumnDef + ")");
      }
      if (genColumnDef.equals(columnParent)) {
        throw new RuntimeException("The column (" + genColumnDef + ") has a expression generator with itself creating a loop. Please choose another column as parent column.");
      }
      CollectionGenerator<?> parentGenerator = columnParent.getOrCreateGenerator(columnParent.getClazz());
      parentCollectionGenerator.add(parentGenerator);
    }

    // Expression
    String expression = genColumnDef.getVariable(String.class, GENERATOR_PROPERTY_KEY, EXPRESSION_PROPERTY);
    if (expression == null) {
      throw new IllegalStateException("The '" + EXPRESSION_PROPERTY + "' property is mandatory to create a expression data generator and is missing for the column (" + genColumnDef.getFullyQualifiedName() + ")");
    }

    // New Instance
    return (ExpressionGenerator<T>) (new ExpressionGenerator<>(tClass, expression, parentCollectionGenerator))
      .setColumnDef(genColumnDef);

  }


  /**
   * @return a new generated data object every time it's called
   */
  @Override
  public T getNewValue() {

    StringBuilder evalScript = new StringBuilder();
    List<String> variableNames = Arrays.asList("x", "y", "z", "a", "b", "c", "d", "e", "f");
    int counter = 0;
    for (CollectionGenerator<?> parentCollectionGenerator : parentCollectionGenerators) {
      Object parentValue = parentCollectionGenerator.getActualValue();
      String value = parentValue.toString();

      if (parentValue.getClass().equals(Date.class)) {
        LocalDate actualDate = ((Date) parentValue).toLocalDate();
        value = "new Date(\"" + actualDate.format(DateTimeFormatter.ISO_DATE) + "\")";
      } else if (parentValue.getClass().equals(String.class)) {
        value = "'" + value + "'";
      }

      String variableName = variableNames.get(counter);
      counter++;
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

      Object evalValue = cx.evaluateString(scope, evalScript.toString(), "<cmd>", 1, null);
      if (evalValue == null) {
        final String msg = "The expression generator for the column (" + this.getColumnDef() + ") has returned a NULL value and it's not expected.\nThe expression was: " + evalScript;
        LOGGER.error(msg);
        throw new RuntimeException(msg);
      }
      this.actualValue = evalValue;

      /**
       * The substring because javascript return always a float for numbers,
       * then you may get 2020.0 in place of 2020 for instance
       */
      if (this.clazz.equals(String.class)) {
        String processString = Strings.createFromObjectNullSafe(evalValue).toString();
        GenColumnDef columnDef = this.getColumnDef();
        if (columnDef.getPrecision() != null) {
          if (processString.length() > columnDef.getPrecisionOrMax()) {
            processString = processString.substring(0, columnDef.getPrecisionOrMax());
          }
        }
        this.actualValue = processString;
      }

      try {
        return Casts.cast(this.actualValue, clazz);
      } catch (CastException e) {
        throw new RuntimeException("The value (" + this.actualValue + ") can not be cast to " + clazz, e);
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
}
