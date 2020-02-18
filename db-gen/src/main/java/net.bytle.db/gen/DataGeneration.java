package net.bytle.db.gen;


import net.bytle.db.engine.ForeignKeyDag;
import net.bytle.db.gen.generator.CollectionGenerator;
import net.bytle.db.gen.memory.GenMemDataPath;
import net.bytle.db.model.ColumnDef;
import net.bytle.db.model.ForeignKeyDef;
import net.bytle.db.model.PrimaryKeyDef;
import net.bytle.db.model.UniqueKeyDef;
import net.bytle.db.spi.DataPath;
import net.bytle.db.spi.Tabulars;
import net.bytle.db.stream.InsertStream;
import net.bytle.log.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Represents an data generation instance
 * <p>
 * This class:
 * * give context to a data generation builder - help build the data generation (give context, a derived data generator need to of access to the other data generations)
 * * got all meta necessary to trigger a {@link #load()}
 */
public class DataGeneration {

  static final Log GEN_LOG = Log.getLog(DataGeneration.class);
  private static final Log LOGGER = GEN_LOG;


  /**
   * Max records to insert if there is no total rows defined
   */
  private static final Integer MAX_INSERT = 100000;

  /**
   * The table to load mapping
   * The target is the driver, this is why it's in the first position
   */
  private final Map<DataPath, GenDataPath> tablesToLoad = new HashMap<>();
  private final Map<ColumnDef, ForeignKeyDef> columnForeignKeyMap = new HashMap<>();
  private final Map<ColumnDef, UniqueKeyDef> columnUniqueKeyMap = new HashMap<>();
  private List<ColumnDef> primaryColumns = new ArrayList<>();

  /**
   * Do we need to load parent table even if they are not in the set
   */
  private Boolean loadParent;


  private DataGeneration() {


  }

  /**
   * The build has a recursive shape because of the derived data generator that depends on another
   * We used this map to of track of what was build
   */
  private Map<ColumnDef, CollectionGenerator> dataGenerators = new HashMap<>();

  public DataGeneration addTable(DataPath targetDataPath, Integer totalRows) {

    GenDataPath sourceDataPath = GenMemDataPath.of(targetDataPath.getName())
      .getDataDef()
      .copy(targetDataPath)
      .setMaxRows(totalRows)
      .getDataPath();
    // Adding the table into the list of tables to load
    tablesToLoad.put(targetDataPath, sourceDataPath);

    // Building the foreign, primary and unique keys

    // Self referencing foreign key check
    List<ForeignKeyDef> selfReferencingForeignKeys = DataGens.getSelfReferencingForeignKeys(targetDataPath);
    if (selfReferencingForeignKeys.size() > 0) {
      for (ForeignKeyDef foreignKeyDef : selfReferencingForeignKeys) {
        LOGGER.severe("The foreign key " + foreignKeyDef.getName() + " on the table (" + foreignKeyDef.getTableDef().getDataPath().toString() + ") references itself and it's not supported.");
      }
      throw new RuntimeException("Self referencing foreign key found in the table " + targetDataPath.toString());
    }


    // Primary Key with only one column are supported
    PrimaryKeyDef primaryKeyDef = targetDataPath.getDataDef().getPrimaryKey();
    // Extract the primary column
    if (primaryKeyDef != null) {
      primaryColumns.addAll(primaryKeyDef.getColumns());
    }

    // Foreign Key with only one column are supported
    List<ForeignKeyDef> foreignKeys = targetDataPath.getDataDef().getForeignKeys();
    for (ForeignKeyDef foreignKeyDef : foreignKeys) {
      int size = foreignKeyDef.getChildColumns().size();
      if (size > 1) {
        throw new RuntimeException("Foreign Key on more than one column are not yet supported. The foreignKey (" + foreignKeyDef.getName() + ") has " + size);
      }
      ColumnDef foreignKeyColumn = foreignKeyDef.getChildColumns().get(0);
      if (this.columnForeignKeyMap.get(foreignKeyColumn) != null) {
        throw new RuntimeException("Two foreign keys on the same column are not supported. The column (" + foreignKeyColumn.toString() + ") has more than one foreign key.");
      }
      this.columnForeignKeyMap.put(foreignKeyColumn, foreignKeyDef);
    }

    // Unique Keys
    for (UniqueKeyDef uniqueKeyDef : targetDataPath.getDataDef().getUniqueKeys()) {
      for (ColumnDef columnDef : uniqueKeyDef.getColumns()) {
        this.columnUniqueKeyMap.put(columnDef, uniqueKeyDef);
      }
    }

    return this;
  }

  /**
   * @return a data generation process
   */
  public static DataGeneration of() {
    return new DataGeneration();
  }

  /**
   * @param columnDef
   * @return
   */
  public <T> CollectionGenerator<T> getDataGenerator(ColumnDef<T> columnDef) {
    return dataGenerators.get(columnDef);
  }


  /**
   * This function starts the data generation and data insertion for all tables specified
   *
   * @return the tables loaded which could be more that the tables asked if the parent loading option is on
   */
  public List<DataPath> load() {

    final List<DataPath> tablesLoaded = new ArrayList<>(tablesToLoad.keySet());

    // Parent check
    // Parent not in the table set to load ?
    // If yes, add the parent to the tables to loaded
    for (DataPath dataPath : tablesLoaded) {
      if (dataPath.getDataDef().getForeignKeys().size() != 0) {
        for (ForeignKeyDef foreignKeyDef : dataPath.getDataDef().getForeignKeys()) {
          DataPath parentDataUnit = foreignKeyDef.getForeignPrimaryKey().getDataDef().getDataPath();
          if (!tablesToLoad.keySet().contains(parentDataUnit)) {

            long rows = Tabulars.getSize(parentDataUnit);
            if (rows == 0) {
              if (this.loadParent!=null && this.loadParent) {
                LOGGER.info("The table (" + parentDataUnit.toString() + ") has no rows, the option to load the parent is on, therefore the table will be loaded.");
                tablesLoaded.add(parentDataUnit);
              } else {
                throw new RuntimeException("The table (" + dataPath.toString() + ") has a foreign key to the parent table (" + parentDataUnit.toString() + "). This table has no rows and the option to load parent is disabled, we cannot then generated rows in the table (" + dataPath.toString() + ")");
              }
            }

          }
        }
      }
    }

    // Load
    final List<DataPath> createOrderedTables = ForeignKeyDag.get(tablesLoaded).getCreateOrderedTables();

    for (DataPath dataPath : createOrderedTables) {

      GenDataPath genDataPath = tablesToLoad.get(dataPath);

      // The load
      LOGGER.info("Loading the table (" + dataPath.toString() + ")");
      LOGGER.info("The size of the table (" + dataPath.toString() + ") before insertion is : " + Tabulars.getSize(dataPath));


      long numberOfRowToInsert = genDataPath.getDataDef().getMaxSize();

      if (numberOfRowToInsert > 0) {
        LOGGER.info("Inserting " + numberOfRowToInsert + " rows into the table (" + dataPath.toString() + ")");

        try (
          InsertStream inputStream = Tabulars.getInsertStream(dataPath);
          GenSelectStream genSelectStream = new GenSelectStream(genDataPath)
        ) {
          while(genSelectStream.next()){
            List<Object> objects = genSelectStream.getObjects();
            inputStream.insert(objects);
          }
        }

      }

      LOGGER.info(numberOfRowToInsert + " records where inserted into the table (" + dataPath.toString() + ")");
      LOGGER.info("The new size is: " + Tabulars.getSize(dataPath));

    }
    return tablesLoaded;

  }


  public DataGeneration loadDependencies(Boolean loadParent) {
    this.loadParent = loadParent;
    return this;
  }




  public DataGeneration addTable(DataPath dataPath) {
    return addTable(dataPath, null);
  }

  public DataGeneration addTables(List<DataPath> dataPaths) {
    for (DataPath dataPath : dataPaths) {
      addTable(dataPath);
    }
    return this;
  }

  /**
   * @param dataPaths
   * @param totalRows - the totalRows
   * @return
   */
  public DataGeneration addTables(List<DataPath> dataPaths, Integer totalRows) {
    for (DataPath dataPath : dataPaths) {
      addTable(dataPath, totalRows);
    }
    return this;
  }

  public DataGeneration addTransfer(GenDataPath genDataPath, DataPath dataPath) {

    return this;
  }
}
