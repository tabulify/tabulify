package com.tabulify.flow.step;

import com.tabulify.conf.Attribute;
import com.tabulify.flow.engine.FilterRunnable;
import com.tabulify.flow.engine.FilterStepAbs;
import com.tabulify.model.RelationDef;
import com.tabulify.spi.AttributeProperties;
import com.tabulify.spi.DataPath;
import com.tabulify.spi.DataPathAttribute;
import com.tabulify.stream.InsertStream;
import net.bytle.exception.NoValueException;
import net.bytle.type.KeyNormalizer;

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
      propertiesDataPath.addColumn(KeyNormalizer.createSafe(DataPathAttribute.DATA_URI).toSqlCaseSafe());
    }
    propertiesDataPath
      .addColumn(KeyNormalizer.createSafe(AttributeProperties.ATTRIBUTE).toSqlCaseSafe())
      .addColumn(KeyNormalizer.createSafe(AttributeProperties.VALUE).toSqlCaseSafe())
      .addColumn(KeyNormalizer.createSafe(AttributeProperties.DESCRIPTION).toSqlCaseSafe());

    List<Attribute> attributes = dataPaths
      .stream()
      .map(DataPath::getAttributes)
      .flatMap(Collection::stream)
      .distinct()
      .sorted()
      .collect(Collectors.toList());

    try (InsertStream insertStream = propertiesDataPath.getDataPath().getInsertStream()) {
      for (DataPath dataPath : dataPaths) {
        for (Attribute attribute : attributes) {
          Object attributeValue;
          try {
            attributeValue = attribute.getValueOrDefault();
          } catch (NoValueException e) {
            continue;
          }
          List<Object> row = new ArrayList<>();
          if (dataPaths.size() > 1) {
            row.add(dataPath.toDataUri().toString());
          }
          row.add(tabular.toPublicName(attribute.getAttributeMetadata().toString()));
          row.add(attributeValue);
          row.add(attribute.getAttributeMetadata().getDescription());
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
