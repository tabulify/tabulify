package com.tabulify.gen.generator;

import com.tabulify.Tabular;
import com.tabulify.connection.Connection;
import com.tabulify.connection.ConnectionBuiltIn;
import com.tabulify.csv.CsvDataPath;
import com.tabulify.gen.*;
import com.tabulify.model.ColumnDef;
import com.tabulify.spi.DataPath;
import com.tabulify.spi.SelectException;
import com.tabulify.spi.Tabulars;
import com.tabulify.stream.SelectStream;
import com.tabulify.uri.DataUriBuilder;
import com.tabulify.uri.DataUriNode;
import com.tabulify.exception.CastException;
import com.tabulify.exception.InternalException;
import com.tabulify.exception.NoColumnException;
import com.tabulify.type.Casts;
import com.tabulify.type.MediaType;
import com.tabulify.type.MediaTypes;

import java.util.*;
import java.util.stream.Collectors;

public class DataSetGenerator<T> extends CollectionGeneratorAbs<T> implements CollectionGenerator<T>, java.util.function.Supplier<T> {


  /**
   * String used when there is no dependent value (ie no filtering)
   */
  public static final String NO_DEPENDENT_VALUE = "";


  /**
   * The file (CSV, ...) where the data is taken from
   */
  private final DataPath dataSetPath;

  /**
   * The entity column that holds the value to return
   */
  private final ColumnDef valueColumn;

  /**
   * A build object where the sub histogram generator are stored
   */
  private final Map<String, HistogramGenerator<?>> histogramsByDependentValue = new HashMap<>();

  /**
   * The column index where there is a weight (ie a probability)
   */
  private Integer entityWeightColumnIndex = null;

  /**
   * The actual value
   */
  @SuppressWarnings("FieldCanBeLocal")
  private Long rowNumber;

  /**
   * The external dependency generator
   */
  private CollectionGenerator<?> dependencyGenerator;
  /**
   * The internal entity dependency columns
   */
  private ColumnDef dependencyColumn;
  /**
   * The column name searched for the factor
   */
  public static final List<String> WEIGHT_COLUMN_NAMES = Arrays.asList("probability", "weight", "factor");

  /**
   * A memory representation of the data set
   * Long is the row number
   */
  private final Map<Long, List<?>> dataSetMemory = new HashMap<>();


  /**
   * The actual value
   */
  private T actualValue;

  /**
   * The actual row
   * It can be passed to a {@link DataSetMetaColumnGenerator}
   */
  private List<?> actualRow;

  /**
   * @param clazz           - the column def that will select the value
   * @param dataSetPath     - the path to the csv file that contains the data set
   * @param valueColumnName - the name of the column in the entity file that contains the value
   */
  public DataSetGenerator(Class<T> clazz, DataPath dataSetPath, String valueColumnName) {

    super(clazz);
    this.dataSetPath = dataSetPath;


    /**
     * Which column to return
     */
    if (valueColumnName == null) {
      this.valueColumn = dataSetPath.getOrCreateRelationDef().getColumnDef(1);
    } else {
      try {
        this.valueColumn = dataSetPath.getOrCreateRelationDef().getColumnDef(valueColumnName);
      } catch (NoColumnException e) {
        throw new IllegalStateException("The column (" + valueColumnName + ") was not found not exist in the columns (" + dataSetPath.getOrCreateRelationDef().getColumnDefs().stream().map(ColumnDef::getColumnName).collect(Collectors.joining(", ")) + ") data resource (" + dataSetPath + "). We cannot create the entity generator.");
      }
    }

    /**
     * Do we have a probability column
     */
    ColumnDef<?> columnProb = null;
    for (String columnName : WEIGHT_COLUMN_NAMES) {
      try {
        columnProb = dataSetPath.getOrCreateRelationDef().getColumnDef(columnName);
        break;
      } catch (NoColumnException e) {
        //
      }

    }
    if (columnProb != null) {
      entityWeightColumnIndex = columnProb.getColumnPosition();
    }


  }

  public static <T> DataSetGenerator<T> create(Class<T> clazz, DataPath entityPath, String valueColumnName) {
    return new DataSetGenerator<>(clazz, entityPath, valueColumnName);
  }


  /**
   * This function is called via recursion by the function {@link GenColumnDef#getOrCreateGenerator()}
   * Don't delete
   */
  public static <T> DataSetGenerator<T> createFromArguments(Class<T> clazz, GenColumnDef<T> genColumnDef) {
    String type = (String) genColumnDef.getDataSupplierAttributeValue(DataSupplierAttribute.TYPE);
    Tabular tabular = genColumnDef.getRelationDef().getDataPath().getConnection().getTabular();
    DataGenType dataGenType = Casts.castSafe(type, DataGenType.class);

    DataPath dataPath;
    String valueColumnName;
    Object metaColumnsObjects;
    switch (dataGenType) {
      case ENTITY:
        Map<DataSetEntityArgument, Object> entityArgumentMap = genColumnDef.getDataSupplierArgument(DataSetEntityArgument.class);
        String locale = (String) entityArgumentMap.get(DataSetEntityArgument.LOCALE);
        if (locale == null) {
          locale = "en";
        }
        // Entity
        String entity = (String) entityArgumentMap.get(DataSetEntityArgument.NAME);
        if (entity == null) {
          entity = genColumnDef.getColumnName();
        }
        // Column
        valueColumnName = (String) entityArgumentMap.get(DataSetEntityArgument.COLUMN);
        if (valueColumnName == null) {
          valueColumnName = entity;
        }
        dataPath = getEntityPath(tabular, entity, locale);
        // Meta columns
        metaColumnsObjects = entityArgumentMap.get(DataSetEntityArgument.META_COLUMNS);
        break;
      case DATA_SET:
        Map<DataSetArgument, Object> argumentMap = genColumnDef.getDataSupplierArgument(DataSetArgument.class);
        // Data Uri
        String dataUri = (String) argumentMap.get(DataSetArgument.DATA_URI);
        if (dataUri == null) {
          throw new IllegalArgumentException("The " + DataSetArgument.DATA_URI + " attribute on the column " + genColumnDef + " is mandatory for a data set generator and was not found. It specifies the data set to use.");
        }
        DataPath generatorDataPath = genColumnDef.getRelationDef().getDataPath();
        if (generatorDataPath instanceof GenDataPath) {
          DataUriNode dataUriNode = ((GenDataPath) generatorDataPath).getDataUriBuilder().apply(dataUri);
          dataPath = tabular.getDataPath(dataUriNode, null);
        } else {
          dataPath = tabular.getDataPath(dataUri);
        }
        if (!Tabulars.exists(dataPath)) {
          throw new IllegalArgumentException("The " + DataSetArgument.DATA_URI + " attribute on the column " + genColumnDef + " specifies a data resource (" + dataUri + ") that does not exists.");
        }
        // Column
        valueColumnName = (String) argumentMap.get(DataSetArgument.COLUMN);
        if (valueColumnName == null) {
          valueColumnName = genColumnDef.getColumnName();
        }
        // Meta columns
        metaColumnsObjects = argumentMap.get(DataSetArgument.META_COLUMNS);
        break;
      default:
        throw new InternalException(dataGenType + " is not a data set type");
    }

    DataSetGenerator<T> dataSetGenerator = (DataSetGenerator<T>) (new DataSetGenerator<>(clazz, dataPath, valueColumnName))
      .setColumnDef(genColumnDef);

    /**
     * Dependent Meta Columns
     */
    if (metaColumnsObjects != null) {
      Map<String, String> metaColumnsMap;
      try {
        metaColumnsMap = Casts.castToSameMap(metaColumnsObjects, String.class, String.class);
      } catch (CastException e) {
        throw new IllegalArgumentException("The " + DataSetArgument.META_COLUMNS + " attribute for the column " + genColumnDef + " is not a valid key/map of string/string");
      }
      int metaColumnsSize = metaColumnsMap.size();
      switch (metaColumnsSize) {
        case 0:
          // empty map
          if (genColumnDef.getRelationDef().getDataPath().getConnection().getTabular().isStrictExecution()) {
            throw new IllegalArgumentException("The " + DataSetArgument.META_COLUMNS + " attribute for the column " + genColumnDef + " is empty");
          }
          break;
        case 1:
          Map.Entry<String, String> dependentColumnName = metaColumnsMap.entrySet().iterator().next();
          String localColumn = dependentColumnName.getKey();
          String dataSetColumn = dependentColumnName.getValue();
          dataSetGenerator.addDependency(localColumn, dataSetColumn);
          break;
        default:
          throw new IllegalArgumentException("The " + DataSetArgument.META_COLUMNS + " attribute for the column " + genColumnDef + " cannot contain more than one column mapping. We found " + metaColumnsSize);
      }

    }

    return dataSetGenerator;
  }


  /**
   * Return the internal CSV entity path
   */
  public static CsvDataPath getEntityPath(Tabular tabular, String entityName, String locale) {

    MediaType csvMediaType = MediaTypes.TEXT_CSV;
    Connection entityConnection = tabular.getConnection(ConnectionBuiltIn.ENTITY_CONNECTION_NAME);


    CsvDataPath csvDataPath = (CsvDataPath) entityConnection.getDataPath(entityName + "/" + entityName + ".csv", csvMediaType)
      .setLogicalName(entityName);

    if (!Tabulars.exists(csvDataPath)) {
      if (locale == null) {
        locale = "en";
      }
      String logicalName = entityName + "_" + locale;
      CsvDataPath enDataPath = (CsvDataPath) entityConnection.getDataPath(entityName + "/" + logicalName + ".csv", csvMediaType)
        .setLogicalName(logicalName);
      if (!Tabulars.exists(enDataPath)) {
        throw new RuntimeException("The entity (" + entityName + ") has no file located at (" + csvDataPath.getAbsoluteNioPath() + ")");
      } else {
        csvDataPath = enDataPath;
        GenLog.LOGGER.fine("The entity file without locale does not exist. The english entity was chosen (" + csvDataPath.getAbsoluteNioPath().toString() + ")");
      }
    }
    /**
     * Be sure that there is a header defined
     */
    csvDataPath.setHeaderRowId(1);
    return csvDataPath;
  }


  @Override
  public T getNewValue() {

    /**
     * First Building of the histogram generators
     */
    if (histogramsByDependentValue.isEmpty()) {

      /**
       * Dependency Value -> Bucket of Row Number, Probability
       */
      Map<String, Map<Long, Double>> distProbabilities = new HashMap<>();
      try (SelectStream dataSetSelectStream = dataSetPath.getSelectStream()) {
        while (dataSetSelectStream.next()) {
          String dependencyValue = NO_DEPENDENT_VALUE;
          if (this.dependencyColumn != null) {
            dependencyValue = dataSetSelectStream.getString(this.dependencyColumn.getColumnPosition());
          }
          // Bucket of Dataset Row Number, Probability
          Map<Long, Double> histogramBuckets = distProbabilities.computeIfAbsent(dependencyValue, k -> new HashMap<>());
          long rowNum = dataSetSelectStream.getRecordId();
          this.dataSetMemory.put(rowNum, dataSetSelectStream.getObjects());
          Double probability = 1.0;
          if (entityWeightColumnIndex != null) {
            Double probabilityEntity = dataSetSelectStream.getDouble(entityWeightColumnIndex);
            if (probabilityEntity != null) {
              probability = probabilityEntity;
            }
          }
          histogramBuckets.put(rowNum, probability);
        }
      } catch (SelectException e) {
        throw new RuntimeException(e);
      }
      distProbabilities.forEach((key, buckets) -> histogramsByDependentValue.put(key, HistogramGenerator.create(Long.class, buckets)));


    }

    Object dependentValue = NO_DEPENDENT_VALUE;
    if (this.dependencyColumn != null) {
      dependentValue = dependencyGenerator.getActualValue();
    }
    HistogramGenerator<?> generator = histogramsByDependentValue.get(dependentValue.toString());
    if (generator == null) {
      throw new RuntimeException("The dependent generator (" + dependencyGenerator + ") has generated the value (" + dependentValue + ") but this value is unknown in the column (" + dependencyColumn.getColumnName() + ") of the entity file (" + this.dataSetPath + ")");
    }
    this.rowNumber = Casts.castSafe(generator.getNewValue(), Long.class);


    /**
     * Get the value
     */
    this.actualRow = this.dataSetMemory.get(this.rowNumber);
    this.actualValue = Casts.castSafe(actualRow.get(this.valueColumn.getColumnPosition() - 1), this.clazz);
    return this.actualValue;

  }


  @Override
  public long getCount() {
    return histogramsByDependentValue.values().stream()
      .mapToLong(HistogramGenerator::getCount)
      .max()
      .orElse(Long.MAX_VALUE);
  }

  @Override
  public void reset() {
    // nothing to do
  }


  /**
   * Shortcut utility function to add a dependency on a column when the name
   * of the data resource column is the same as the name
   * in the entity file
   */
  @SuppressWarnings("unused")
  public DataSetGenerator<?> addDependency(String dependentColumnName) {
    addDependency(dependentColumnName, dependentColumnName);
    return this;

  }


  /**
   * @param localDependentColumnName   - the name of the data resource column that will generate the value
   * @param dataSetDependentColumnName - the name of the entity column that should match the value of the dependent column
   */
  public DataSetGenerator<?> addDependency(String localDependentColumnName, String dataSetDependentColumnName) {

    ColumnDef columnDef;
    try {
      columnDef = this.getColumnDef().getRelationDef()
        .getColumnDef(localDependentColumnName);
    } catch (NoColumnException e) {
      throw new IllegalStateException("The column (" + localDependentColumnName + ") was not found in the resource (" + this.getColumnDef().getRelationDef().getDataPath() + ")");
    }

    if (columnDef == null) {
      throw new IllegalStateException("The dependent column (" + localDependentColumnName + ") of the resource (" + this.getColumnDef().getRelationDef().getDataPath() + ") is not a generator column and it's not supported");
    }

    dependencyGenerator = ((GenColumnDef<?>) columnDef).getOrCreateGenerator();
    if (dependencyGenerator == null) {
      throw new IllegalStateException("The dependent column (" + localDependentColumnName + ") for the generator (" + this + ") was not found on the data generation resource (" + this.getColumnDef().getRelationDef().getDataPath() + ")");
    }

    try {
      dependencyColumn = this.dataSetPath.getOrCreateRelationDef().getColumnDef(dataSetDependentColumnName);
    } catch (NoColumnException e) {
      throw new IllegalStateException("The entity dependent column named (" + dataSetDependentColumnName + ") for the generator (" + this + ") was not found in the data set (" + dataSetPath + ")");
    }
    return this;
  }

  @Override
  public String toString() {
    return dataSetPath.getLogicalName() + " " + this.getClass().getSimpleName() + " for the column " + this.getColumnDef();
  }


  @Override
  public T getActualValue() {
    return actualValue;
  }

  @Override
  public Set<CollectionGenerator<?>> getDependencies() {
    if (dependencyGenerator != null) {
      return Collections.singleton(dependencyGenerator);
    } else {
      return new HashSet<>();
    }
  }


  /**
   * The actual row
   */
  public List<?> getActualRow() {
    return this.actualRow;
  }

  public DataPath getDataSet() {
    return this.dataSetPath;
  }


  @Override
  public DataGenType getGeneratorType() {
    return DataGenType.DATA_SET;
  }

  @Override
  public Boolean isNullable() {
    return false;
  }

}
