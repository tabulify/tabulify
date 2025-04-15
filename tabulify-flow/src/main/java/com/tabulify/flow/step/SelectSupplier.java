package com.tabulify.flow.step;


import net.bytle.dag.Dag;
import com.tabulify.Tabular;
import com.tabulify.connection.Connection;
import com.tabulify.engine.ForeignKeyDag;
import com.tabulify.flow.engine.OperationStep;
import com.tabulify.flow.engine.StepAbs;
import com.tabulify.flow.stream.DataPathSupplier;
import com.tabulify.spi.DataPath;
import com.tabulify.spi.Tabulars;
import com.tabulify.uri.DataUri;
import net.bytle.exception.CastException;
import net.bytle.exception.InternalException;
import net.bytle.exception.NoValueException;
import net.bytle.exception.NoVariableException;
import net.bytle.template.TextTemplate;
import net.bytle.template.TextTemplateEngine;
import net.bytle.type.Casts;
import net.bytle.type.Enums;
import net.bytle.type.MapKeyIndependent;
import net.bytle.type.MediaType;
import net.bytle.type.yaml.YamlCast;

import java.util.*;

import static java.util.stream.Collectors.*;
import static com.tabulify.flow.step.SelectSupplierArgument.*;


/**
 * A start operation in a stream
 * that generates a list of data path
 */
public class SelectSupplier extends StepAbs implements DataPathSupplier, OperationStep, Iterator<Set<DataPath>> {


  private List<Set<DataPath>> dataPathSets;

  /**
   * If a data selector does not return any data
   * an error is thrown
   * <p>
   * Used when the next operation is a drop
   * By default, a drop/truncate tabli command is strict (ie not ifExist drop)
   * in a flow, this is not the case
   */
  private boolean isStrict = false;

  /**
   * The counter
   */
  int counter = 0;
  private MediaType mediaType = null;
  private String logicalName;
  private Map<String, Object> inlineDataDef;

  public SelectSupplier() {

    this.getOrCreateArgument(DATA_SELECTOR).setValueProvider(this::getDataSelector);
    this.getOrCreateArgument(LOGICAL_NAME).setValueProvider(() -> this.logicalName);
    this.getOrCreateArgument(STRICT).setValueProvider(() -> this.isStrict);
    this.getOrCreateArgument(TYPE).setValueProvider(() -> this.mediaType);
    this.getOrCreateArgument(WITH_DEPENDENCIES).setValueProvider(() -> this.withDependencies);

  }

  public static SelectSupplier create() {
    return new SelectSupplier();

  }


  public DataUri getDataSelector() {
    return this.dataSelectors.iterator().next();
  }


  /**
   * Selection with dependencies
   */
  private Boolean withDependencies = false;
  /**
   * The data selectors
   */
  private Set<DataUri> dataSelectors = new HashSet<>();
  /**
   * The attributes of the selected data resources
   */
  private Map<String, ?> attributes = new HashMap<>();


  public SelectSupplier setAttributes(Map<String, ?> attributes) {
    this.attributes = attributes;
    return this;
  }

  public SelectSupplier setDataSelector(DataUri dataSelector) {
    this.dataSelectors = Collections.singleton(dataSelector);
    return this;
  }

  public SelectSupplier setDataSelectors(Set<DataUri> dataSelectors) {
    this.dataSelectors = dataSelectors;
    return this;
  }


  @Override
  public Set<DataPath> get() {

    return next();

  }

  private void buildIfNeededAndSelectTheDataPaths() {

    if (dataPathSets != null) {
      return;
    } else {
      dataPathSets = new ArrayList<>();
    }

    Set<DataPath> dataPathSet = tabular.select(dataSelectors, isStrict, mediaType);


    /**
     * Have we selected something ?
     *
     */
    if (dataPathSet.size() == 0) {

      return;

    }

    /**
     * Apply the attributes if any
     */
    dataPathSet
      .stream()
      .map(dataPath -> dataPath.setDataAttributes(attributes))
      .forEach(CastStep.create());

    /**
     * Apply the logical name if any
     */
    if (this.logicalName != null) {
      TextTemplate textTemplate = TextTemplateEngine.getOrCreate().compile(this.logicalName);
      for (DataPath dataPath : dataPathSet) {
        Map<String, Object> map = new HashMap<>();
        textTemplate.getVariableNames().forEach(
          variableName -> {
            try {
              map.put(variableName, dataPath.getVariable(variableName).getValueOrDefault());
            } catch (NoVariableException | NoValueException e) {
              throw new RuntimeException("The variable name (" + variableName + ") is an attribute without any defined value");
            }
          }
        );
        String logicalName = textTemplate.applyVariables(map).getResult();
        dataPath.setLogicalName(logicalName);
      }
    }

    /**
     * Apply the data definition if any
     */
    if (this.inlineDataDef != null) {
      for (DataPath dataPath : dataPathSet) {
        dataPath.mergeDataDefinitionFromYamlMap(this.inlineDataDef);
      }
    }

    /**
     * By connection, the data set are not dependent
     */
    Map<Connection, Set<DataPath>> mapDataPathsByConnection = dataPathSet
      .stream()
      .collect(
        groupingBy(
          DataPath::getConnection,
          mapping(e -> e, toSet()))
      );

    for (Set<DataPath> dataPaths : mapDataPathsByConnection.values()) {
      Dag<DataPath> dag = ForeignKeyDag.createFromPaths(dataPaths);
      if (withDependencies) {
        dag.setWithDependency(true);
      }
      List<DataPath> orderedDataPaths = dag.getDropOrdered();

      /**
       * Split between data path with dependencies
       * and without
       */
      Set<DataPath> dependenciesDataSet = new HashSet<>();
      Set<DataPath> independentDataSet = new HashSet<>();
      for (DataPath orderedDataPath : orderedDataPaths) {
        if (orderedDataPath.getDependencies().size() != 0 || Tabulars.getReferences(orderedDataPath).size() != 0) {
          dependenciesDataSet.add(orderedDataPath);
        } else {
          independentDataSet.add(orderedDataPath);
          this.dataPathSets.add(independentDataSet);
          independentDataSet = new HashSet<>();
        }
      }
      if (dependenciesDataSet.size() > 0) {
        this.dataPathSets.add(dependenciesDataSet);
      }


    }

  }



  public SelectSupplier setWithDependencies(Boolean withDependencies) {
    if (withDependencies != null) {
      this.withDependencies = withDependencies;
    }
    return this;
  }


  @Override
  public int getSetCount() {
    buildIfNeededAndSelectTheDataPaths();
    return dataPathSets.size();
  }

  @Override
  public String getOperationName() {
    return "select";
  }

  @Override
  public boolean hasNext() {
    buildIfNeededAndSelectTheDataPaths();
    return counter < dataPathSets.size();
  }

  @Override
  public Set<DataPath> next() {
    buildIfNeededAndSelectTheDataPaths();
    try {
      Set<DataPath> dataPaths = dataPathSets.get(counter);
      counter++;
      return dataPaths;
    } catch (IndexOutOfBoundsException e) {
      throw new NoSuchElementException();
    }

  }

  @Override
  public SelectSupplier setTabular(Tabular tabular) {
    return (SelectSupplier) super.setTabular(tabular);
  }

  /**
   * @return all data path
   */
  public Set<DataPath> getAll() {
    buildIfNeededAndSelectTheDataPaths();
    return dataPathSets.stream()
      .flatMap(Collection::stream)
      .collect(toSet());
  }

  public SelectSupplier setIsStrict(boolean strictness) {
    this.isStrict = strictness;
    return this;
  }

  @Override
  public OperationStep setArguments(MapKeyIndependent<Object> arguments) {

    for (Map.Entry<String, Object> argument : arguments.entrySet()) {
      String argumentKey = argument.getKey();
      SelectSupplierArgument selectArgument;
      try {
        selectArgument = Casts.cast(argumentKey, SelectSupplierArgument.class);
      } catch (CastException e) {
        throw new RuntimeException("The argument (" + argumentKey + ") is not a valid argument for the step (" + this + "). You can choose one of: " + Enums.toConstantAsStringOfUriAttributeCommaSeparated(SelectSupplierArgument.class));
      }
      Object value = argument.getValue();
      switch (selectArgument) {
        case DATA_SELECTOR:
          this.setDataSelector(tabular.createDataUri((String) value));
          break;
        case WITH_DEPENDENCIES:
          Boolean localWithDependencies;
          try {
            localWithDependencies = Casts.cast(value.toString(), Boolean.class);
          } catch (CastException e) {
            throw new IllegalArgumentException("The " + WITH_DEPENDENCIES + " value (" + value + ") of the step (" + this + ") is not a boolean.");
          }
          this.setWithDependencies(localWithDependencies);
          break;
        case ATTRIBUTES:
          Map<String, ?> attributes;
          try {
            attributes = Casts.castToSameMap(value, String.class, Object.class);
          } catch (CastException e) {
            throw new InternalException("String and Object should not throw a cast exception", e);
          }
          this.setAttributes(attributes);
          break;
        case STRICT:
          boolean strict;
          try {
            strict = Casts.cast(value.toString(), Boolean.class);
          } catch (CastException e) {
            throw new IllegalArgumentException("The " + STRICT + " value (" + value + ") of the step (" + this + ") is not a boolean.");
          }
          this.setIsStrict(strict);
          break;
        case LOGICAL_NAME:
          this.setLogicalName((String) value);
          break;
        case DATA_DEFINITION:
          Map<String, Object> dataDef;
          try {
            dataDef = YamlCast.castToSameMap(value, String.class, Object.class);
          } catch (CastException e) {
            throw new InternalException("String and Object should not throw a cast exception", e);
          }
          this.setDataDefinition(dataDef);
          break;
        default:
          throw new InternalException("The argument (" + argumentKey + ") for the step (" + this + ") should have a branch in the switch");
      }

    }

    if (this.getDataSelector() == null) {
      throw new IllegalArgumentException("The argument (" + DATA_SELECTOR + ") is mandatory and was not found for the step (" + this + ")");
    }

    return this;

  }

  private SelectSupplier setDataDefinition(Map<String, Object> dataDef) {
    this.inlineDataDef = dataDef;
    return this;
  }

  SelectSupplier setLogicalName(String logicalName) {
    this.logicalName = logicalName;
    return this;
  }


  public SelectSupplier setMediaType(MediaType mediaType) {
    this.mediaType = mediaType;
    return this;
  }
}
