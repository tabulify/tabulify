package net.bytle.db.gen;

import net.bytle.db.model.ColumnDef;
import net.bytle.db.model.TableDef;
import net.bytle.db.spi.DataPath;
import net.bytle.db.stream.SelectStreamAbs;
import net.bytle.type.Maps;

import java.sql.Clob;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static net.bytle.db.gen.GenColumnDef.GENERATOR_PROPERTY_KEY;

public class GenSelectStream extends SelectStreamAbs {


  // Convenience shortcut
  private final List<ColumnDef> primaryColumns;
  private final List<ColumnDef> allUniqueKeyColumns;

  public GenSelectStream(DataPath dataPath) {
    super(dataPath);
    GenDataPath genDataPath = (GenDataPath) dataPath;
    GenDataDef dataDef = genDataPath.getDataDef();

    // Convenience shortcut
    primaryColumns = this.getDataPath().getDataDef().getPrimaryKey().getColumns();
    allUniqueKeyColumns = this.getDataPath().getDataDef().getUniqueKeys().stream().flatMap(uk->uk.getColumns().stream()).collect(Collectors.toList());

    // First pass to create a default generator if they are not specified
    for (GenColumnDef columnDef : dataDef.getGenColumnsDef()) {

      if (columnDef.getGenerator() == null) {
        buildDefaultDataGeneratorForColumn(columnDef);
      }

    }


  }

  /**
   * The build has a recursive shape because of the derived data generator that depends on another
   * We used this map to track what was build
   */
  private Map<ColumnDef, DataGenerator> dataGenerators = new HashMap<>();

  /**
   * Function that is used to build the data generator for the column
   * It had the generators to the map of Generators.
   * <p>
   * This is a reflective function who can call itself when the generator depends on another column generator.
   * This is also a function that can create several generator for several columns (for instance, if the column is part
   * of an unique key, one generator will be created with all columns at once).
   */
  public <T> void buildDefaultDataGeneratorForColumn(GenColumnDef<T> columnDef) {


    DataGenerator generator = dataGenerators.get(columnDef);
    if (generator == null) {

      // When read from a data definition file into the column property
      final Object generatorProperty = Maps.getPropertyCaseIndependent(columnDef.getProperties(), GENERATOR_PROPERTY_KEY);
      if (generatorProperty != null) {

        final Map<String, Object> generatorColumnProperties;
        try {
          generatorColumnProperties = (Map<String, Object>) generatorProperty;
        } catch (ClassCastException e) {
          throw new RuntimeException("The values of the property (" + GENERATOR_PROPERTY_KEY + ") for the column (" + columnDef.toString() + ") should be a map value. Bad values:" + generatorProperty);
        }

        final String nameProperty = (String) Maps.getPropertyCaseIndependent(generatorColumnProperties, "name");
        if (nameProperty == null) {
          throw new RuntimeException("The name property of the generator was not found within the property (" + GENERATOR_PROPERTY_KEY + ") of the column " + columnDef.toString() + ".");
        }
        DataGenerator<T> dataGenerator;
        String name = nameProperty.toLowerCase();
        switch (name) {
          case "sequence":
            dataGenerator = SequenceGenerator.of(columnDef);
            break;
          case "unique":
            dataGenerator = SequenceGenerator.of(columnDef);
            break;
          case "derived":
            dataGenerator = DerivedGenerator.of(columnDef, this);
            break;
          case "random":
            dataGenerator = DistributionGenerator.of(columnDef);
            break;
          case "distribution":
            dataGenerator = DistributionGenerator.of(columnDef);
            break;
          default:
            throw new RuntimeException("The generator (" + name + ") defined for the column (" + columnDef.toString() + ") is unknown");
        }
        dataGenerators.put(columnDef, dataGenerator);
        return;

      }
    }

    // A data generator was not yet fund, we will find one with the column constraint
    if (this.primaryColumns.contains(columnDef)) {

      final List<ColumnDef> primaryColumnsForColumnDefTable = primaryColumns.stream().filter(c -> c.getDataDef().equals(columnDef.getDataDef())).collect(Collectors.toList());
      UniqueDataGenerator uniqueDataGenerator = new UniqueDataGenerator(primaryColumnsForColumnDefTable);
      for (ColumnDef pkColumns : primaryColumnsForColumnDefTable) {
        dataGenerators.put(pkColumns, uniqueDataGenerator);
      }
      return;

      // TODO
      //    } else if (columnForeignKeyMap.keySet().contains(columnDef)) {
      //
      //      final FkDataGenerator dataGenerator = new FkDataGenerator(columnForeignKeyMap.get(columnDef));
      //      dataGenerators.put(columnDef, dataGenerator);
      //      return;

    } else if (allUniqueKeyColumns.contains(columnDef)) {

      List<ColumnDef> uniqueKeyColumns = this.getDataPath().getDataDef().getUniqueKeys().stream()
        .filter(uk->uk.getColumns().contains(columnDef))
        .flatMap(uk->uk.getColumns().stream())
        .collect(Collectors.toList());

      UniqueDataGenerator uniqueDataGenerator = new UniqueDataGenerator(uniqueKeyColumns);
      for (ColumnDef uniqueKeyColumn : allUniqueKeyColumns) {
        dataGenerators.put(uniqueKeyColumn, uniqueDataGenerator);
      }
      return;

    }

    // Else
    dataGenerators.put(columnDef, new DistributionGenerator<>(columnDef));


  }

  @Override
  public boolean next() {
    return false;
  }

  @Override
  public void close() {

  }

  @Override
  public String getString(int columnIndex) {
    return null;
  }

  @Override
  public int getRow() {
    return 0;
  }

  @Override
  public Object getObject(int columnIndex) {
    return null;
  }

  @Override
  public TableDef getSelectDataDef() {
    return null;
  }

  @Override
  public double getDouble(int columnIndex) {
    return 0;
  }

  @Override
  public Clob getClob(int columnIndex) {
    return null;
  }

  @Override
  public boolean next(Integer timeout, TimeUnit timeUnit) {
    return false;
  }

  @Override
  public Integer getInteger(int columnIndex) {
    return null;
  }

  @Override
  public Object getObject(String columnName) {
    return null;
  }

  @Override
  public void beforeFirst() {

  }

  @Override
  public void execute() {

  }

  /**
   * @param columnDef
   * @return
   */
  public <T> DataGenerator<T> getDataGenerator(ColumnDef<T> columnDef) {
    return dataGenerators.get(columnDef);
  }

}
