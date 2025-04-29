package com.tabulify.gen.generator;


import com.tabulify.gen.DataGenType;
import com.tabulify.gen.GenColumnDef;
import net.bytle.exception.CastException;
import net.bytle.exception.NoColumnException;
import net.bytle.type.Casts;
import net.bytle.type.Strings;
import net.bytle.type.time.Date;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.EvaluatorException;
import org.mozilla.javascript.ScriptableObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

import static com.tabulify.gen.DataGenAttribute.COLUMN_PARENTS;
import static com.tabulify.gen.DataGenAttribute.EXPRESSION;


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
    Object columnParentsValue = genColumnDef.getDataGeneratorValue(COLUMN_PARENTS);
    if (columnParentsValue == null) {
      throw new IllegalStateException("The " + COLUMN_PARENTS + " property is not defined in the '" + COLUMN_PARENTS + "' properties for the column " + genColumnDef.getFullyQualifiedName());
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
    Object expression = genColumnDef.getDataGeneratorValue(EXPRESSION);
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
    List<String> variableNames = Arrays.asList("x", "y", "z", "a", "b", "c", "d", "e", "f");
    int counter = 0;
    for (CollectionGenerator<?> parentCollectionGenerator : parentCollectionGenerators) {
      Object parentValue = parentCollectionGenerator.getActualValue();
      String value = parentValue.toString();

      if (Arrays.asList(java.util.Date.class, java.sql.Date.class).contains(parentValue.getClass())) {
        LocalDate actualDate;
        if (parentValue.getClass().equals(java.sql.Date.class)) {
          actualDate = ((java.sql.Date) parentValue).toLocalDate();
        } else {
          actualDate = Date.createFromDate((java.util.Date) parentValue).toLocalDate();
        }
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

      Object evalValue;
      try {
        evalValue = cx.evaluateString(scope, evalScript.toString(), "<cmd>", 1, null);
      } catch (Exception e) {
        throw new RuntimeException("Error while evaluating the expression for the column " + this.getColumnDef() + ". \nError: " + e.getMessage() + ". \nExpression:\n" + evalScript, e);
      }
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
      } catch (CastException ex) {
        try {
          if (clazz.equals(java.sql.Date.class)) {
            // Happens on formula
            // The object (org.mozilla.javascript.NativeDate@46678e49) has an class (NativeDate) that is not yet seen as a date.
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

}
