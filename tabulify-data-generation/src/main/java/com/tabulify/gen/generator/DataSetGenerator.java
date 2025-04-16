package com.tabulify.gen.generator;

import com.tabulify.Tabular;
import com.tabulify.connection.ConnectionBuiltIn;
import com.tabulify.csv.CsvDataPath;
import com.tabulify.connection.Connection;
import com.tabulify.gen.GenColumnDef;
import com.tabulify.gen.GenLog;
import com.tabulify.model.ColumnDef;
import com.tabulify.spi.DataPath;
import com.tabulify.spi.SelectException;
import com.tabulify.spi.Tabulars;
import com.tabulify.stream.SelectStream;
import net.bytle.exception.NoColumnException;
import net.bytle.type.Casts;
import net.bytle.type.MediaType;
import net.bytle.type.MediaTypes;

import java.util.*;
import java.util.stream.Collectors;

public class DataSetGenerator<T> extends CollectionGeneratorAbs<T> implements CollectionGenerator<T>, java.util.function.Supplier<T> {


  /**
   * String used when there is no dependent value (ie no filtering)
   */
  public static final String NO_DEPENDENT_VALUE = "";


  /**
   * The CSV file where the list of entities are stored
   */
  private final DataPath entityPath;

  /**
   * The entity column that holds the value to return
   */
  private final ColumnDef entityValueColumn;

  /**
   * A build object where the sub histogram generator are stored
   */
  private final Map<String, HistogramGenerator<?>> nameStreams = new HashMap<>();

  /**
   * The column index where there is a weight (ie a probability)
   */
  private Integer entityWeigthColumnIndex = null;

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
  public static final List<String> WEIGTH_COLUMN_NAMES = Arrays.asList("probability", "weight", "factor");

  /**
   * A memory representation of the entity
   */
  private final Map<Long, List<?>> entitySet = new HashMap<>();


  /**
   * The actual value
   */
  private T actualValue;

  /**
   * The actual row
   * It can be passed to a {@link DataSetColumnGenerator}
   */
  private List<?> actualRow;

  /**
   * @param clazz           - the column def that will select the value
   * @param entityPath      - the path to the csv file that contains the data set
   * @param valueColumnName - the name of the column in the entity file that contains the value
   */
  public DataSetGenerator(Class<T> clazz, DataPath entityPath, String valueColumnName) {

    super(clazz);
    this.entityPath = entityPath;


    /**
     * Which column to return
     */

    if (valueColumnName == null) {
      this.entityValueColumn = entityPath.getOrCreateRelationDef().getColumnDef(1);
    } else {
      try {
        this.entityValueColumn = entityPath.getOrCreateRelationDef().getColumnDef(valueColumnName);
      } catch (NoColumnException e) {
        throw new IllegalStateException("The column (" + valueColumnName + ") was not found not exist in the columns (" + entityPath.getOrCreateRelationDef().getColumnDefs().stream().map(ColumnDef::getColumnName).collect(Collectors.joining(", ")) + ") data resource (" + entityPath + "). We cannot create the entity generator.");
      }
    }

    /**
     * Do we have a probability column
     */
    ColumnDef columnProb = null;
    for (String columnName : WEIGTH_COLUMN_NAMES) {
      try {
        columnProb = entityPath.getOrCreateRelationDef().getColumnDef(columnName);
        break;
      } catch (NoColumnException e) {
        //
      }

    }
    if (columnProb != null) {
      entityWeigthColumnIndex = columnProb.getColumnPosition();
    }


  }

  public static <T> DataSetGenerator<T> create(Class<T> clazz, DataPath entityPath, String valueColumnName) {
    return new DataSetGenerator<>(clazz, entityPath, valueColumnName);
  }


  /**
   * This function is called via recursion by the function {@link GenColumnDef#getOrCreateGenerator(Class)}
   * Don't delete
   *
   */
  public static <T> DataSetGenerator<T> createFromProperties(Class<T> clazz, GenColumnDef genColumnDef) {
    String valueColumnName = genColumnDef.getGeneratorProperty(String.class, "column");
    String entityKey = "entity";
    String entity = genColumnDef.getGeneratorProperty(String.class, entityKey);
    String locale = genColumnDef.getGeneratorProperty(String.class, "locale");
    Tabular tabular = genColumnDef.getRelationDef().getDataPath().getConnection().getTabular();
    DataPath dataPath;
    if (entity != null) {
      dataPath = getEntityPath(tabular, entity, locale);
    } else {
      String dataUriKey = "dataUri";
      String dataUri = genColumnDef.getGeneratorProperty(String.class, dataUriKey);
      if (dataUri != null) {
        dataPath = tabular.getDataPath(dataUri);
      } else {
        throw new RuntimeException("The data generation definition of the column (" + genColumnDef + ") does not have an `" + entityKey + "` or `dataUriKey` that defines the data set.");
      }
    }
    return (DataSetGenerator<T>) (new DataSetGenerator<>(clazz, dataPath, valueColumnName))
      .setColumnDef(genColumnDef);
  }


  /**
   * Return the internal CSV entity path
   *
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
    if (nameStreams.size() == 0) {

      Map<String, Map<Long, Double>> distProbabilities = new HashMap<>();
      Map<Long, Double> distProb;
      if (this.dependencyColumn != null) {

        try (SelectStream selectStream = entityPath.getSelectStream()) {
          while (selectStream.next()) {
            String dependencyValue = selectStream.getString(this.dependencyColumn.getColumnPosition());
            distProb = distProbabilities.computeIfAbsent(dependencyValue, k -> new HashMap<>());
            long rowNum = selectStream.getRow();
            this.entitySet.put(rowNum, selectStream.getObjects());
            Double probability = 1.0;
            if (entityWeigthColumnIndex != null) {
              Double probabilityEntity = selectStream.getDouble(entityWeigthColumnIndex);
              if (probabilityEntity != null) {
                probability = probabilityEntity;
              }
            }
            distProb.put(rowNum, probability);
          }
        } catch (SelectException e) {
          throw new RuntimeException(e);
        }
        distProbabilities.forEach((key, buckets) -> nameStreams.put(key, HistogramGenerator.create(Long.class, buckets)));

      } else {
        try (SelectStream selectStream = entityPath.getSelectStream()) {
          distProb = new HashMap<>();
          while (selectStream.next()) {
            /**
             * Build the memory set
             */
            long rowNum = selectStream.getRow();
            this.entitySet.put(rowNum, selectStream.getObjects());
            /**
             * Build the histogram
             */
            Double probability = 1.0;
            if (entityWeigthColumnIndex != null) {
              Double probabilityEntity = selectStream.getDouble(entityWeigthColumnIndex);
              if (probabilityEntity != null) {
                probability = probabilityEntity;
              }
            }
            distProb.put(rowNum, probability);
          }
        } catch (SelectException e) {
          throw new RuntimeException(e);
        }
        nameStreams.put(NO_DEPENDENT_VALUE, HistogramGenerator.create(Long.class, distProb));
      }
    }

    Object dependentValue = NO_DEPENDENT_VALUE;
    if (this.dependencyColumn != null) {
      dependentValue = dependencyGenerator.getActualValue();
    }
    HistogramGenerator<?> generator = nameStreams.get(dependentValue.toString());
    if (generator == null) {
      throw new RuntimeException("The dependent generator (" + dependencyGenerator + ") has generated the value (" + dependentValue + ") but this value is unknown in the column (" + dependencyColumn.getColumnName() + ") of the entity file (" + this.entityPath + ")");
    }
    this.rowNumber = Casts.castSafe(generator.getNewValue(), Long.class);


    /**
     * Get the value
     */
    this.actualRow = this.entitySet.get(this.rowNumber);
    this.actualValue = Casts.castSafe(actualRow.get(this.entityValueColumn.getColumnPosition() - 1), this.clazz);
    return this.actualValue;

  }


  @Override
  public long getCount() {
    return nameStreams.values().stream()
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
   * of the data resource column is the same than the name
   * in the entity file
   *
   */
  public DataSetGenerator<?> addDependency(String dependentColumnName) {
    addDependency(dependentColumnName, dependentColumnName);
    return this;

  }


  /**
   * @param dependentColumnName       - the name of the data resource column that will generate the value
   * @param entityDependentColumnName - the name of the entity column that should match the value of the dependent column
   */
  public DataSetGenerator<?> addDependency(String dependentColumnName, String entityDependentColumnName) {

    GenColumnDef columnDef;
    try {
      columnDef = this.getRelationDef()
        .getColumnDef(dependentColumnName);
    } catch (NoColumnException e) {
      throw new IllegalStateException("The column ("+dependentColumnName+") was not found in the resource ("+this.getRelationDef().getDataPath()+")");
    }

    dependencyGenerator = columnDef
      .getOrCreateGenerator(columnDef.getClazz());
    if (dependencyGenerator == null) {
      throw new IllegalStateException("The dependent column (" + dependentColumnName + ") for the generator (" + this + ") was not found on the data generation resource (" + this.getRelationDef().getDataPath() + ")");
    }
    try {
      dependencyColumn = this.entityPath.getOrCreateRelationDef().getColumnDef(entityDependentColumnName);
    } catch (NoColumnException e) {
      throw new IllegalStateException("The entity dependent column named (" + entityDependentColumnName + ") for the generator (" + this + ") was not found in the entity file (" + entityPath + ")");
    }
    return this;
  }

  @Override
  public String toString() {
    return entityPath.getLogicalName() + " " + this.getClass().getSimpleName() + " for the column " + this.getColumnDef();
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
   *
   */
  public List<?> getActualRow() {
    return this.actualRow;
  }

  public DataPath getEntity() {
    return this.entityPath;
  }



}
