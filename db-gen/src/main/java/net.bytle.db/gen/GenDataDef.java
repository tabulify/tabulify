package net.bytle.db.gen;


import net.bytle.db.gen.generator.CollectionGenerator;
import net.bytle.db.gen.generator.UniqueDataCollectionGenerator;
import net.bytle.db.model.*;
import net.bytle.db.spi.DataPath;
import net.bytle.db.spi.DataPathAbs;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Types;
import java.util.*;
import java.util.stream.Collectors;

/**
 *
 */
public class GenDataDef extends DataDefAbs implements RelationDef {

  private static final Logger LOGGER = LoggerFactory.getLogger(GenDataDef.class);

  public static final String TYPE = "GEN";

  public static final int DEFAULT_DATA_TYPE = Types.VARCHAR;
  /**
   * The {@link TableDef#getProperty(String)} key giving the total number of rows that the table should have
   */
  public static final String TOTAL_ROWS_PROPERTY_KEY = "TotalRows";

  private Map<String, GenColumnDef> genColumns = new HashMap<>();

  public GenDataDef(DataPathAbs dataPath) {
    super(dataPath);
  }


  public GenDataDef addColumn(String columnName) {
    this.addColumn(columnName, null, null, null, null, null);
    return this;
  }

  @Override
  public GenDataDef addColumn(String columnName, Integer typeCode) {
    this.addColumn(columnName, typeCode, null, null, null, null);
    return this;
  }

  public GenDataDef addColumn(String columnName, int typeCode) {
    this.addColumn(columnName, typeCode, null, null, null, null);
    return this;
  }

  public GenDataDef addColumn(String columnName, Integer type, Integer precision) {
    this.addColumn(columnName, type, precision, null, null, null);
    return this;
  }

  public GenDataDef addColumn(String columnName, Integer type, Boolean nullable) {
    this.addColumn(columnName, type, null, null, nullable, null);
    return this;
  }

  public GenDataDef addColumn(String columnName, Integer type, Integer precision, Integer scale) {
    this.addColumn(columnName, type, precision, scale, null, null);
    return this;
  }

  public GenDataDef addColumn(String columnName, Integer type, Integer precision, Integer scale, Boolean nullable) {
    this.addColumn(columnName, type, precision, scale, nullable, null);
    return this;
  }


  public GenDataDef addColumn(String columnName, Integer type, Integer precision, Integer scale, Boolean nullable, String comment) {
    if (type == null) {
      type = DEFAULT_DATA_TYPE;
    }
    Class clazz = this.getDataPath().getDataStore().getSqlDataType(type).getClazz();
    if (!genColumns.keySet().contains(columnName)) {
      genColumns.put(columnName, (GenColumnDef) GenColumnDef.of(this, columnName, clazz)
        .typeCode(type)
        .precision(precision)
        .scale(scale)
        .setNullable(nullable)
        .comment(comment)
        .setColumnPosition(genColumns.size() + 1));
    } else {
      LOGGER.warn("The column (" + columnName + ") was already defined, you can't add it");
    }
    return this;
  }

  @Override
  public GenColumnDef[] getColumnDefs() {
    return (new ArrayList<>(genColumns.values()))
      .stream()
      .sorted()
      .toArray(GenColumnDef[]::new);
  }


  public GenDataDef addColumn(String columnName, Integer type, Integer precision, Boolean nullable) {
    this.addColumn(columnName, type, precision, null, nullable, null);
    return this;
  }

  public GenDataDef setMaxSize(Long maxSize) {
    // Just to be able to have this function in a fluent code with a null value
    if (maxSize!=null) {
      this.addProperty(GenDataDef.TOTAL_ROWS_PROPERTY_KEY, maxSize);
    }
    return this;
  }


  @Override
  public <T> GenColumnDef getColumnDef(String columnName) {
    return genColumns.values()
      .stream()
      .filter(c -> c.getColumnName().equals(columnName))
      .findFirst()
      .orElse(null);
  }

  @Override
  public <T> GenColumnDef getColumnDef(Integer columnIndex) {
    return genColumns.values()
      .stream()
      .filter(c -> c.getColumnPosition().equals(columnIndex))
      .findFirst()
      .orElse(null);
  }

  @Override
  public <T> GenColumnDef<T> getColumnOf(String columnName, Class<T> clazz) {


    if (!genColumns.containsValue(columnName)) {
      GenColumnDef<T> of = (GenColumnDef<T>) GenColumnDef.of(this, columnName, clazz)
        .setColumnPosition(genColumns.values().size() + 1);
      genColumns.put(columnName, of);
      return of;
    } else {
      throw new RuntimeException("The column (" + columnName + ") is already defined");
    }
  }

  @Override
  public int getColumnsSize() {
    return genColumns.size();
  }


  @Override
  public GenDataPath getDataPath() {
    return (GenDataPath) super.getDataPath();
  }

  /**
   *
   * @return the max size of rows generated is set or null
   */
  public Long getMaxSize() {
    return this.getPropertyAsLong(GenDataDef.TOTAL_ROWS_PROPERTY_KEY);
  }

  @Override
  public GenDataDef copyDataDef(DataPath sourceDataPath) {

    super.copyDataDef(sourceDataPath);
    return this;

  }

  @Override
  public <T> GenColumnDef<T> getColumn(String columnName, Class<T> clazz) {


    GenColumnDef genColumnDef = genColumns.get(columnName);
    if (!genColumnDef.getClazz().equals(clazz)){
      throw new RuntimeException("The column ("+genColumnDef+") does not have a clazz of ("+clazz+") but of "+genColumnDef.getClazz());
    }
    return genColumnDef;

  }

  /**
   * @return The size of data that will be generated
   */
  public long   getSize() {

    Long maxNumberOfRowToInsert = getMaxSizeFromGenerators();

    // Max size capping ?
    Long maxSize = this.getMaxSize();
    if (maxSize != null && maxNumberOfRowToInsert > maxSize) {
      return maxSize;
    } else {
      return maxNumberOfRowToInsert;
    }

  }

  /**
   *
   * @return The maximum size of data that can be generated by the collection of generators
   *
   */
  private Long getMaxSizeFromGenerators() {
    buildMissingGenerators();
    Long maxSizeFromGenerators = 0L;
    for (CollectionGenerator dataGenerator : Arrays.stream(getColumnDefs()).map(GenColumnDef::getGenerator).collect(Collectors.toList())) {
      final Long maxGeneratedValues = dataGenerator.getMaxGeneratedValues();
      if (maxSizeFromGenerators == 0) {
        maxSizeFromGenerators = maxGeneratedValues;
      } else {
        if (maxSizeFromGenerators > maxGeneratedValues) {
          maxSizeFromGenerators = maxGeneratedValues;
        }
      }
    }
    return maxSizeFromGenerators;
  }

  /**
   * This function will build the generator for the columns without one
   */
  public void buildMissingGenerators() {

    Arrays.stream(getColumnDefs())
      .filter(c->c.getGenerator()==null)
      .forEach(this::buildMissingGeneratorForColumn);

  }


  /**
   * Function that is used to build a data generator if missing for the column
   *   *
   * This is also a function that can create several generator for several columns (for instance, if the column is part
   * of an unique key, one generator will be created with all columns at once).
   */
  private <T> void buildMissingGeneratorForColumn(GenColumnDef<T> columnDef) {

    CollectionGenerator generator = columnDef.getGenerator();
    if (generator != null) {
      return;
    }

    // If primary key
    PrimaryKeyDef primaryKey = this.getPrimaryKey();
    if (primaryKey!=null) {
      List<ColumnDef> primaryColumns = primaryKey.getColumns();
      if (primaryColumns.contains(columnDef)) {

        if (primaryColumns.size() == 1) {

          GenColumnDef primaryCol = (GenColumnDef) primaryColumns.get(0);
          primaryCol.addSequenceGenerator();

        } else {

          List<GenColumnDef> genPrimaryColumns = primaryColumns.stream().map(DataGens::castToGenColumnDef).collect(Collectors.toList());
          UniqueDataCollectionGenerator uniqueDataGenerator = new UniqueDataCollectionGenerator(genPrimaryColumns);
          for (GenColumnDef pkColumn : genPrimaryColumns) {
            pkColumn.setGenerator(uniqueDataGenerator);
          }

        }

        return;

      }

    }

    // Not yet returned
    List<UniqueKeyDef> uniqueKeys = this.getUniqueKeys().stream()
      .filter(uk -> uk.getColumns().contains(columnDef))
      .collect(Collectors.toList());

    if (uniqueKeys.size()>0) {

      uniqueKeys.forEach(uk->{
        List<GenColumnDef> ukCols = uk.getColumns().stream().map(DataGens::castToGenColumnDef).collect(Collectors.toList());
        UniqueDataCollectionGenerator uniqueDataGenerator = new UniqueDataCollectionGenerator(ukCols);
        ukCols.forEach(col->col.setGenerator(uniqueDataGenerator));
      });

      return;

    }

    // Else
    columnDef.addDistributionGenerator();


  }


}
