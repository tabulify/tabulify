package com.tabulify.flow.step;

import com.tabulify.DbLoggers;
import com.tabulify.flow.engine.FilterRunnable;
import com.tabulify.flow.engine.FilterStepAbs;
import com.tabulify.flow.engine.OperationStep;
import com.tabulify.memory.MemoryDataPath;
import com.tabulify.model.RelationDef;
import com.tabulify.spi.DataPath;
import com.tabulify.spi.DataPathAttribute;
import com.tabulify.stream.InsertStream;
import net.bytle.exception.CastException;
import net.bytle.exception.NoValueException;
import net.bytle.exception.NoVariableException;
import net.bytle.type.Casts;
import net.bytle.type.MapKeyIndependent;

import java.sql.Types;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;

/**
 * The list operation.
 * <p>
 * A function that takes a list of data path
 * and collect their attributes in a data path
 * <p>
 * T: The input class
 * A: The accumulator class
 * R: The result class returned
 */
public class ListCollector extends FilterStepAbs implements Collector<DataPath, InsertStream, DataPath>, OperationStep {


  public static final String DEFAULT_TARGET_LOGICAL_NAME = "data_resource_list";
  private List<String> attributes = Collections.singletonList(DataPathAttribute.DATA_URI.toString());
  private String targetLogicalName = DEFAULT_TARGET_LOGICAL_NAME;
  private String targetDescription = "";

  public static ListCollector create() {
    return new ListCollector();
  }


  /**
   * The creator of the stream
   * that will be used to accumulate
   *
   * @return the insert stream
   */
  protected InsertStream insertStreamCreator() {
    MemoryDataPath dataPath = (MemoryDataPath) this.tabular.getMemoryDataStore().getAndCreateRandomDataPath()
      .setLogicalName(targetLogicalName)
      .setDescription(targetDescription);

    RelationDef accumulatorRelationDef = dataPath.getOrCreateRelationDef();
    boolean countPresent = false;
    for (String attribute : this.attributes) {
      DataPathAttribute dataPathAttribute;
      try {
        dataPathAttribute = Casts.cast(attribute, DataPathAttribute.class);
      } catch (CastException e) {
        // may be a dynamic attribute (backref reference from a regexp, ...)
        accumulatorRelationDef.addColumn(attribute, Types.VARCHAR);
        continue;
      }
      switch (dataPathAttribute) {
        case COUNT:
          accumulatorRelationDef.addColumn(attribute, Types.INTEGER);
          countPresent = true;
          break;
        case SIZE:
          accumulatorRelationDef.addColumn(attribute, Types.INTEGER);
          break;
        default:
          accumulatorRelationDef.addColumn(attribute, Types.VARCHAR);
      }
    }
    if (!countPresent) {
      DbLoggers.LOGGER_DB_ENGINE.tip("You can add the `count` attribute to see the number of records. Example: `-a name -a count`");
    }
    return accumulatorRelationDef.getDataPath().getInsertStream();
  }

  /**
   * The accumulator function
   */
  protected BiConsumer<InsertStream, DataPath> accumulator = (stream, dp) -> {
    List<Object> row = new ArrayList<>();
    for (String attribute : attributes) {
      try {
        row.add(dp.getAttribute(attribute).getValueOrDefault());
      } catch (NoVariableException | NoValueException e) {
        // ok
        row.add(null);
      }
    }
    stream.insert(row);
  };

  /**
   * The finisher function that
   * transform the stream in data path
   */
  Function<InsertStream, DataPath> finisher = (stream) -> {
    stream.close();
    return stream.getDataPath();
  };

  public ListCollector setAttributes(List<String> attributes) {
    this.attributes = attributes;
    return this;
  }


  /**
   * The start step that creates the accumulator object
   *
   * @return the supplier
   */
  @Override
  public Supplier<InsertStream> supplier() {
    return this::insertStreamCreator;
  }


  /**
   * The step that accumulates the object
   *
   * @return the bi consumer
   */
  @Override
  public BiConsumer<InsertStream, DataPath> accumulator() {

    return accumulator;
  }

  /**
   * If there is two threads, this step would
   * combine them
   *
   * @return the binary operator
   */
  @Override
  public BinaryOperator<InsertStream> combiner() {
    throw new UnsupportedOperationException("Not yet implemented");
  }

  /**
   * A function which transforms the accumulator to the final result
   *
   * @return the finisher
   */
  @Override
  public Function<InsertStream, DataPath> finisher() {

    return finisher;

  }

  @Override
  public Set<Characteristics> characteristics() {
    // Example:
    // Collections.singleton(Characteristics.IDENTITY_FINISH);
    // Indicates that the finisher() function is the identity function and can be left out
    return new HashSet<>();
  }

  @Override
  public String getOperationName() {
    return "list";
  }


  @Override
  public ListCollector setArguments(MapKeyIndependent<Object> arguments) {
    List<String> attributes = Arrays.asList("attributes", "att", "attribute");
    Object att = null;
    for (String attribute : attributes) {
      att = arguments.get(attribute);
      if (att != null) {
        break;
      }
    }

    if (att != null) {
      if (att instanceof List) {
        this.attributes = Casts.castToListSafe(att, String.class);
      } else {
        this.attributes = Collections.singletonList(att.toString());
      }
    }
    return this;
  }

  @Override
  public FilterRunnable createRunnable() {
    return new ListRunnable(this);
  }

  @Override
  public boolean isAccumulator() {
    return true;
  }

  public ListCollector setTargetLogicalName(String logicalName) {
    this.targetLogicalName = logicalName;
    return this;
  }

  public ListCollector setTargetDescription(String targetDescription) {
    this.targetDescription = targetDescription;
    return this;
  }

  /**
   * A runnable
   */
  private static class ListRunnable implements FilterRunnable {


    private final ListCollector listCollector;
    private final InsertStream insertStream;
    private final List<DataPath> inputs = new ArrayList<>();
    private boolean isDone = false;

    public ListRunnable(ListCollector listCollector) {
      this.listCollector = listCollector;
      this.insertStream = listCollector.insertStreamCreator();
    }


    @Override
    public void addInput(Set<DataPath> inputs) {
      this.inputs.addAll(inputs);
    }

    @Override
    public void run() {
      inputs.stream().sorted().forEach(
        e -> this.listCollector.accumulator.accept(this.insertStream, e)
      );
      isDone = true;
    }

    @Override
    public Set<DataPath> get() {

      return Collections.singleton(this.listCollector.finisher.apply(this.insertStream));

    }


    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
      return false;
    }

    @Override
    public boolean isCancelled() {
      return false;
    }

    @Override
    public boolean isDone() {
      return isDone;
    }

    @Override
    public Set<DataPath> get(long timeout, TimeUnit unit) {
      return get();
    }


  }

}
