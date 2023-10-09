package net.bytle.db.flow.step;

import net.bytle.db.flow.engine.FilterRunnable;
import net.bytle.db.flow.engine.FilterStepAbs;
import net.bytle.db.model.RelationDef;
import net.bytle.db.spi.AttributeProperties;
import net.bytle.db.spi.DataPath;
import net.bytle.db.spi.DataPathAttribute;
import net.bytle.db.stream.InsertStream;
import net.bytle.exception.NoValueException;
import net.bytle.type.Key;
import net.bytle.type.Variable;

import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Return a set of data path attributes in a three columns data path
 */
public class InfoStep extends FilterStepAbs implements Function<Set<DataPath>, DataPath> {


  public static InfoStep create() {
    return new InfoStep();
  }

  @Override
  public DataPath apply(Set<DataPath> dataPaths) {

    String feedbackName = "attribute";
    String description = "Attributes of data resources";
    if (dataPaths.size() == 1) {
      DataPath dataPath = dataPaths.iterator().next();
      feedbackName = dataPath.getName() + "_" + feedbackName;
      description = "Information about the data resource (" + dataPath.toDataUri() + ")";
    }
    RelationDef propertiesDataPath = tabular.getMemoryDataStore()
      .getAndCreateRandomDataPath()
      .setLogicalName(feedbackName)
      .setDescription(description)
      .getOrCreateRelationDef();

    if (dataPaths.size() > 1) {
      propertiesDataPath.addColumn(Key.toColumnName(DataPathAttribute.DATA_URI));
    }
    propertiesDataPath
      .addColumn(Key.toColumnName(AttributeProperties.ATTRIBUTE))
      .addColumn(Key.toColumnName(AttributeProperties.VALUE))
      .addColumn(Key.toColumnName(AttributeProperties.DESCRIPTION));

    List<Variable> variables = dataPaths
      .stream()
      .map(DataPath::getVariables)
      .flatMap(Collection::stream)
      .distinct()
      .sorted()
      .collect(Collectors.toList());

    try (InsertStream insertStream = propertiesDataPath.getDataPath().getInsertStream()) {
      for (DataPath dataPath : dataPaths) {
        for (Variable variable : variables) {
          Object attributeValue;
          try {
            attributeValue = variable.getValueOrDefault();
          } catch (NoValueException e) {
            continue;
          }
          List<Object> row = new ArrayList<>();
          if (dataPaths.size() > 1) {
            row.add(dataPath.toDataUri().toString());
          }
          row.add(variable.getPublicName());
          row.add(attributeValue);
          row.add(variable.getAttribute().getDescription());
          insertStream.insert(row);
        }
      }
    }

    return propertiesDataPath.getDataPath();

  }

  @Override
  public String getOperationName() {
    return "info";
  }


  @Override
  public FilterRunnable createRunnable() {
    return new InfoOperationFilterRunnable(this);
  }

  static class InfoOperationFilterRunnable implements FilterRunnable {

    private final InfoStep infoOperation;
    private final Set<DataPath> inputs = new HashSet<>();
    private DataPath dataPath;
    private boolean isDone = false;

    public InfoOperationFilterRunnable(InfoStep infoOperation) {
      this.infoOperation = infoOperation;
    }

    @Override
    public void addInput(Set<DataPath> inputs) {
      this.inputs.addAll(inputs);
    }

    @Override
    public void run() {
      this.dataPath = infoOperation.apply(inputs);
      isDone = true;
    }

    @Override
    public Set<DataPath> get() throws InterruptedException, ExecutionException {
      return Collections.singleton(dataPath);
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
    public Set<DataPath> get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
      return get();
    }
  }

}
