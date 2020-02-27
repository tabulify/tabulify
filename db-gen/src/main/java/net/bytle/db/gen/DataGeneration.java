package net.bytle.db.gen;


import net.bytle.db.engine.ForeignKeyDag;
import net.bytle.db.gen.generator.CollectionGenerator;
import net.bytle.db.gen.generator.CollectionGeneratorOnce;
import net.bytle.db.gen.generator.FkDataCollectionGenerator;
import net.bytle.db.gen.generator.SequenceGenerator;
import net.bytle.db.gen.memory.GenMemDataPath;
import net.bytle.db.model.ColumnDef;
import net.bytle.db.model.ForeignKeyDef;
import net.bytle.db.spi.DataPath;
import net.bytle.db.spi.Tabulars;
import net.bytle.db.stream.InsertStream;
import net.bytle.log.Log;
import net.bytle.type.Strings;

import java.util.*;
import java.util.stream.Collectors;

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
  private final Map<DataPath, GenDataPath> transfers = new HashMap<>();
  // private final Map<ColumnDef, ForeignKeyDef> columnForeignKeyMap = new HashMap<>();
  //private final Map<ColumnDef, UniqueKeyDef> columnUniqueKeyMap = new HashMap<>();
//  private List<ColumnDef> primaryColumns = new ArrayList<>();

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
  private Map<ColumnDef, CollectionGeneratorOnce> dataGenerators = new HashMap<>();


  public DataGeneration addTable(DataPath targetDataPath, Long totalRows) {

    GenDataPath sourceDataPath = GenMemDataPath.of(targetDataPath.getName())
      .getOrCreateDataDef()
      .copyDataDef(targetDataPath)
      .setMaxSize(totalRows)
      .getDataPath();
    // Adding the table into the list of tables to load
    addTransfer(sourceDataPath, targetDataPath);

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
  public <T> CollectionGeneratorOnce<T> getDataGenerator(ColumnDef<T> columnDef) {
    return dataGenerators.get(columnDef);
  }


  /**
   * This function starts the data generation and data insertion for all tables specified
   *
   * @return the tables loaded which could be more that the tables asked if the parent loading option is on
   */
  public List<DataPath> load() {

    // Target Parent check
    // Parent not in the table set to load ?
    // If yes, add a transfer with the parent tables
    List<DataPath> targetDataPaths = new ArrayList<>(transfers.keySet());
    for (DataPath targetDataPath : targetDataPaths) {
      if (targetDataPath.getOrCreateDataDef().getForeignKeys().size() != 0) {
        for (ForeignKeyDef foreignKeyDef : targetDataPath.getOrCreateDataDef().getForeignKeys()) {
          DataPath foreignDataPath = foreignKeyDef.getForeignPrimaryKey().getDataDef().getDataPath();
          if (!transfers.containsKey(foreignDataPath)) {
            long rows = Tabulars.getSize(foreignDataPath);
            if (rows == 0) {
              if (this.loadParent != null && this.loadParent) {
                LOGGER.info("The table (" + foreignDataPath.toString() + ") has no rows, the option to load the parent is on, therefore the table will be loaded.");
                addTable(foreignDataPath);
              } else {
                throw new RuntimeException("The table (" + targetDataPath.toString() + ") has a foreign key to the parent table (" + foreignDataPath.toString() + "). This table has no rows and the option to load parent is disabled, we cannot then generated rows in the table (" + targetDataPath.toString() + ")");
              }
            }
          }
        }
      }
    }

    // Source
    // Building the missing data generators
    transfers.values().forEach(sourceDataPath -> sourceDataPath.getOrCreateDataDef().buildMissingGenerators());

    // The load parent option may have added a transfer
    targetDataPaths = new ArrayList<>(transfers.keySet());
    // foreign data generators building
    for (DataPath targetDataPath : targetDataPaths) {

      // Add the foreign collection generator to the foreign columns
      targetDataPath
        .getOrCreateDataDef()
        .getForeignKeys()
        .forEach(foreignKey -> {

          // Only one column relationship is supported
          if (foreignKey.getChildColumns().size() > 1) {
            String cols = foreignKey.getChildColumns().stream().map(ColumnDef::getColumnName).collect(Collectors.joining(","));
            throw new RuntimeException("The foreign key of the data path (" + targetDataPath + ") has more than one columns (" + cols + "and its not yet supported");
          }

          // Get the foreign column
          String foreignTableName = targetDataPath.getName();
          String foreignColumnName = foreignKey.getChildColumns().get(0).getColumnName();
          GenColumnDef foreignColumn = transfers.values()
            .stream()
            .filter(dp -> dp.getName().equals(foreignTableName))
            .flatMap(dp -> Arrays.stream(dp.getOrCreateDataDef().getColumnDefs()))
            .filter(c -> c.getColumnName().equals(foreignColumnName))
            .findFirst()
            .orElse(null);
          assert foreignColumn != null : "The foreign column was not found (" + foreignTableName + "." + foreignColumnName + ")";

          // Try to get the primary table generator
          final String primaryKeyTableName = foreignKey.getForeignPrimaryKey().getDataDef().getDataPath().getName();
          GenDataPath genPrimaryTable = transfers.values()
            .stream()
            .filter(dp -> dp.getName().equals(primaryKeyTableName))
            .findFirst()
            .orElse(null);

          // If not defined
          if (genPrimaryTable == null) {

            // The table is not part of the data generation specification
            LOGGER.warning("The data generation for the column (" + foreignKey.getForeignPrimaryKey() + ") is not defined, we are then obliged to retrieve all data of this column to build the data generator for the foreign column (" + foreignColumn + ")");
            foreignColumn.setGenerator(new FkDataCollectionGenerator(foreignKey));

          } else {

            // The table is in the data generation definition
            ColumnDef primaryColumn = foreignKey.getForeignPrimaryKey().getColumns().get(0);
            CollectionGenerator primaryKeyCollectionGenerator = Arrays.stream(genPrimaryTable.getOrCreateDataDef().getColumnDefs())
              .filter(c -> c.getColumnName().equals(primaryColumn.getColumnName()))
              .map(GenColumnDef::getGenerator)
              .findFirst()
              .orElse(null);

            assert primaryKeyCollectionGenerator != null : "The primary key data generator was not found on the primary column (" + primaryColumn + ")";
            assert primaryKeyCollectionGenerator.getClass() == SequenceGenerator.class : "The generator of the primary column (" + primaryColumn + ") is not a sequence but (" + primaryKeyCollectionGenerator.getClass().getSimpleName() + "). Other generator than a sequence for a primary column are not yet supported";

            // Create the random distribution generator from the sequence
            SequenceGenerator<Object> sequenceGenerator = (SequenceGenerator<Object>) primaryKeyCollectionGenerator;
            ((GenColumnDef<Object>) foreignColumn)
              .addUniformDistributionGenerator()
              .setMin(sequenceGenerator.getDomainMin())
              .setMax(sequenceGenerator.getDomainMax())
              .step(sequenceGenerator.getStep());

          }
        })
      ;
    }

    // Load
    final List<DataPath> createOrderedTables = ForeignKeyDag.get(targetDataPaths).getCreateOrderedTables();

    for (
      DataPath dataPath : createOrderedTables) {

      GenDataPath genDataPath = transfers.get(dataPath);

      // The load
      LOGGER.info("Loading the table (" + dataPath.toString() + ")");
      LOGGER.info("The size of the table (" + dataPath.toString() + ") before insertion is : " + Tabulars.getSize(dataPath));

      Tabulars.create(genDataPath);
      long numberOfRowToInsert = Tabulars.getSize(genDataPath);
      if (numberOfRowToInsert > MAX_INSERT) {
        throw new RuntimeException(
          Strings.multiline("The generator (" + genDataPath + ") may generate (" + numberOfRowToInsert + ") records which is bigger than the upper limit of (" + MAX_INSERT + ").",
            "Set a row size number on the generator data path to resolve this issue."));
      }

      if (numberOfRowToInsert > 0) {
        LOGGER.info("Inserting " + numberOfRowToInsert + " rows into the table (" + dataPath.toString() + ")");

        try (
          InsertStream inputStream = Tabulars.getInsertStream(dataPath);
          GenSelectStream genSelectStream = new GenSelectStream(genDataPath)
        ) {
          while (genSelectStream.next()) {
            List<Object> objects = genSelectStream.getObjects();
            inputStream.insert(objects);
          }
        }

      }

      LOGGER.info(numberOfRowToInsert + " records where inserted into the table (" + dataPath.toString() + ")");
      LOGGER.info("The new size is: " + Tabulars.getSize(dataPath));

    }

    // Return the tables loaded (due to the parent options, this may be more than the configured one)
    return targetDataPaths;

  }

  private GenDataPath getGenDataPath(DataPath parentDataUnit) {
    return transfers.values().stream()
      .filter(dp -> dp.getName().equals(parentDataUnit.getName()))
      .findFirst()
      .orElse(null);
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
  public DataGeneration addTables(List<DataPath> dataPaths, Long totalRows) {
    for (DataPath dataPath : dataPaths) {
      addTable(dataPath, totalRows);
    }
    return this;
  }

  /**
   * @param sourceDataPaths
   * @param targetDataPath
   * @return
   */
  public DataGeneration addTransfers(List<GenDataPath> sourceDataPaths, DataPath targetDataPath) {
    for (GenDataPath sourceDataPath : sourceDataPaths) {
      addTransfer(sourceDataPath, targetDataPath);
    }
    return this;
  }

  public DataGeneration addTransfer(GenDataPath sourceDataPath, DataPath targetDataPath) {
    if (Tabulars.isContainer(targetDataPath)){
      targetDataPath = targetDataPath.getChild(sourceDataPath.getName());
    }
    // Add the transfers
    transfers.put(targetDataPath, sourceDataPath);
    return this;

  }
}
