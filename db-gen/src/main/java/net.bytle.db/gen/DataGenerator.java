package net.bytle.db.gen;

import net.bytle.db.gen.generator.*;
import net.bytle.db.model.PrimaryKeyDef;
import net.bytle.type.Maps;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static net.bytle.db.gen.GenColumnDef.GENERATOR_PROPERTY_KEY;

public class DataGenerator {

  private final GenDataPath genDataPath;
  private final List<GenColumnDef> primaryColumns;
  private final List<GenColumnDef> allUniqueKeyColumns;

  public DataGenerator(GenDataPath genDataPath) {
    this.genDataPath = genDataPath;

    // Convenience shortcut
    PrimaryKeyDef primaryKey = genDataPath
      .getDataDef()
      .getPrimaryKey();
    if (primaryKey != null) {
      primaryColumns = primaryKey
        .getColumns().stream()
        .map(DataGens::castToGenColumnDef)
        .collect(Collectors.toList());
    } else {
      primaryColumns = new ArrayList<>();
    }

    allUniqueKeyColumns = genDataPath
      .getDataDef()
      .getUniqueKeys().stream()
      .flatMap(uk -> uk.getColumns().stream())
      .map(DataGens::castToGenColumnDef)
      .collect(Collectors.toList());


    // First pass to create a default generator if they are not specified
    for (GenColumnDef columnDef : genDataPath.getDataDef().getGenColumnsDef()) {

      if (columnDef.getGenerator() == null) {
        buildDefaultDataGeneratorForColumn(columnDef);
      }

    }

  }

  public static DataGenerator of(GenDataPath genDataPath) {
    return new DataGenerator(genDataPath);
  }

  /**
   * The build has a recursive shape because of the derived data generator that depends on another
   * We used this map to track what was build
   */
  private Map<GenColumnDef, CollectionGenerator> dataGenerators = new HashMap<>();

  /**
   * Function that is used to build the data generator for the column
   * It had the generators to the map of Generators.
   * <p>
   * This is a reflective function who can call itself when the generator depends on another column generator.
   * This is also a function that can create several generator for several columns (for instance, if the column is part
   * of an unique key, one generator will be created with all columns at once).
   */
  public <T> void buildDefaultDataGeneratorForColumn(GenColumnDef<T> columnDef) {


    CollectionGenerator generator = dataGenerators.get(columnDef);
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
        CollectionGenerator<T> dataGenerator;
        String name = nameProperty.toLowerCase();
        switch (name) {
          case "sequence":
            dataGenerator = SequenceCollectionGenerator.of(columnDef);
            break;
          case "unique":
            dataGenerator = SequenceCollectionGenerator.of(columnDef);
            break;
          case "derived":
            dataGenerator = DerivedCollectionGenerator.of(columnDef, this);
            break;
          case "random":
            dataGenerator = DistributionCollectionGenerator.of(columnDef);
            break;
          case "distribution":
            dataGenerator = DistributionCollectionGenerator.of(columnDef);
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

      final List<GenColumnDef> primaryColumnsForColumnDefTable = primaryColumns.stream().filter(c -> c.getDataDef().equals(columnDef.getDataDef())).collect(Collectors.toList());
      UniqueDataCollectionGenerator uniqueDataGenerator = new UniqueDataCollectionGenerator(primaryColumnsForColumnDefTable);
      for (GenColumnDef pkColumns : primaryColumnsForColumnDefTable) {
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

      List<GenColumnDef> uniqueKeyColumns = genDataPath.getDataDef().getUniqueKeys().stream()
        .filter(uk -> uk.getColumns().contains(columnDef))
        .flatMap(uk -> uk.getColumns().stream())
        .map(DataGens::castToGenColumnDef)
        .collect(Collectors.toList());

      UniqueDataCollectionGenerator uniqueDataGenerator = new UniqueDataCollectionGenerator(uniqueKeyColumns);
      for (GenColumnDef uniqueKeyColumn : allUniqueKeyColumns) {
        dataGenerators.put(uniqueKeyColumn, uniqueDataGenerator);
      }
      return;

    }

    // Else
    dataGenerators.put(columnDef, new DistributionCollectionGenerator<>(columnDef));


  }

  public <T> CollectionGenerator<T> getCollectionGenerator(GenColumnDef<T> genColumnDef) {
    return dataGenerators.get(genColumnDef);
  }

  public int getDataGeneratorsSize() {
    return dataGenerators.size();
  }

  /**
   *
   * @return The maximum size of data that can be generated
   */
  public long getMaxSize() {
    // Precision of a sequence (Pk of unique col) make that we cannot insert the number of rows that we want
    Integer maxNumberOfRowToInsert = 0;
    for (CollectionGenerator dataGenerator : dataGenerators.values()) {
      final Integer maxGeneratedValues = (dataGenerator.getMaxGeneratedValues()).intValue();
      if (maxNumberOfRowToInsert == 0) {
        maxNumberOfRowToInsert = maxGeneratedValues;
      } else {
        if (maxNumberOfRowToInsert > maxGeneratedValues) {
          maxNumberOfRowToInsert = maxGeneratedValues;
        }
      }
    }
    return maxNumberOfRowToInsert;
  }
}
