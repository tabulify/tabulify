package com.tabulify.gen;


import com.tabulify.Tabular;
import com.tabulify.engine.ForeignKeyDag;
import com.tabulify.gen.fs.GenFsDataPath;
import com.tabulify.gen.generator.ExpressionGenerator;
import com.tabulify.gen.generator.ForeignColumnGenerator;
import com.tabulify.gen.memory.GenMemDataPath;
import com.tabulify.memory.MemoryConnection;
import com.tabulify.memory.MemoryConnectionProvider;
import com.tabulify.model.ColumnDef;
import com.tabulify.model.ForeignKeyDef;
import com.tabulify.spi.DataPath;
import com.tabulify.spi.Tabulars;
import com.tabulify.transfer.TransferManager;
import com.tabulify.transfer.TransferOperation;
import com.tabulify.transfer.TransferProperties;
import com.tabulify.transfer.TransferSourceTarget;
import net.bytle.type.Strings;

import java.util.*;
import java.util.stream.Collectors;

import static com.tabulify.gen.DataGens.getSecondForeignKeysOnTheSameColumn;
import static com.tabulify.gen.DataGens.getSelfReferencingForeignKeys;

/**
 * The main class for data resource generation
 * <p>
 * You {@link #create(Tabular) initiate with a tabular}
 * and create DataPath with {@link #createGenDataPath(String)}
 * <p>
 * This is because the {@link #memoryConnection} has a special namespace
 * to not conflict with other memory data path
 * <p>
 * This class also:
 * * give context to a data generation builder - help build the data generation (give context, a {@link ExpressionGenerator} need to of access to the other data generations)
 * * got all meta necessary to trigger a {@link #load()}
 */
public class DataGenerator implements AutoCloseable {

  /**
   * The data store
   */
  private final MemoryConnection memoryConnection;

  TransferManager transferManager;

  /**
   * Max records to insert if there is no total rows defined
   */
  public static final Integer MAX_INSERT = 100000;

  /**
   * The table to load mapping
   * The target table is the driver, this is why it's in the first position
   * <p>
   * GenMemDataPath is mandatory to have all table in the same store (ie memory
   * datastore) in order to be able to manage foreign keys
   */
  private final Map<DataPath, GenMemDataPath> targetSources = new HashMap<>();


  /**
   * Do we need to load parent table even if they are not in the set
   */
  private Boolean loadParent;
  private boolean sourceTargetBuildingWasDone = false;


  private DataGenerator(Tabular tabular) {


    /**
     * We create another memory data store to have a temporary namespace.
     * Because during the cross transfer test, we create in the memory datastore
     * table, and we load them.
     * Because the data generation process translate all source paths to {@link #toGenMemDataPath(DataPath) memory data path},
     * there is a name conflict
     *
     */
    this.memoryConnection = (MemoryConnection) tabular.createRuntimeConnection("memgen", MemoryConnectionProvider.SCHEME + ":///gen");

  }


  /**
   * Add a target table
   * from a dummy table
   * <p>
   * A dummy table is a table that have the same structure
   * than the target table and default generators
   *
   * @param targetDataPath the target data path
   * @param maxRowCount    the maximum row count generated
   * @return the object for chaining
   */
  public DataGenerator addDummyTransfer(DataPath targetDataPath, Long maxRowCount) {

    /**
     * Get a memory data path typed for generation
     */
    GenDataPath sourceDataPath = toGenMemDataPath(targetDataPath)
      .setMaxRecordCount(maxRowCount);
    /**
     * Adding the table into the list of tables to load
     */
    this.addTransfer(sourceDataPath, targetDataPath);

    return this;
  }

  /**
   * Transform a {@link DataPath} into a {@link GenMemDataPath}
   * <p>
   * We use this function to copy to create a {@link GenMemDataPath}
   * * from a target data path
   * * from {@link GenFsDataPath}.
   * <p>
   * This is because we need a single source data store (memory) in order to keep
   * the foreign key consistency (ie there is no foreign key constraint possible between two different data store)
   */
  public GenMemDataPath toGenMemDataPath(DataPath dataPath) {

    if (dataPath instanceof GenMemDataPath) {
      return (GenMemDataPath) dataPath;
    }

    /**
     * No foreign Key merge, please, this is done later in the {@link #generateSourceTargetMap()}
     */
    return (GenMemDataPath) this.memoryConnection
      .getTypedDataPath(GenDataPathType.DATA_GEN, dataPath.getLogicalName())
      .mergeDataPathAttributesFrom(dataPath)
      .getOrCreateRelationDef()
      .mergeStruct(dataPath)
      .getDataPath();


  }

  /**
   * @return a data generation process
   */
  public static DataGenerator create(Tabular tabular) {
    return new DataGenerator(tabular);
  }


  /**
   * This function starts the data generation and data insertion for all tables specified
   *
   * @return the tables loaded which could be more that the tables asked if the parent loading option is on
   */
  public DataGenerator load() {

    // Add the parent if needed
    // build the generators
    this.generateSourceTargetMap();

    TransferManager transferManager = TransferManager.create();
    targetSources.keySet().forEach(targetDataPath -> {
        // Set the column mapping by name
        // The column on the generator may have not the same column position than the target
        // but have the same name, we set then the column mapping to be by name (and not by position - default)
        TransferSourceTarget transferSourceTarget = new TransferSourceTarget(
          targetSources.get(targetDataPath),
          targetDataPath,
          TransferProperties
            .create()
            .setOperation(TransferOperation.UPSERT)
            .withColumnMappingByName()
        );
        transferManager.addTransfer(transferSourceTarget);
      }
    );

    this.transferManager = transferManager.run();

    this.close();

    return this;

  }


  /**
   * When all transfers has been added,
   * this function will build the transfer
   * ie:
   * * add the dependent table if the option {@link #loadDependencies(Boolean)} is true
   * * add the missing generators and take into account the target constraints (foreign key generator)
   * * verify that the size to insert is not abyssal (ie bigger than {@link #MAX_INSERT})
   * <p>
   * You can get the modified data generator path by calling the {@link #getGenDataPaths()}
   */
  public DataGenerator generateSourceTargetMap() {

    /**
     * Circuit breaker
     */
    if (!sourceTargetBuildingWasDone) {

      sourceTargetBuildingWasDone = true;

      // Metadata check
      // Check that there is no self referencing key
      // ie a column that references itself so that we got a cycle
      targetSources.keySet().forEach(dp -> {
        List<ForeignKeyDef> fk = getSelfReferencingForeignKeys(dp);
        if (!fk.isEmpty()) {
          throw new RuntimeException(Strings.createMultiLineFromStrings(
            "The data path (" + dp + ") has one more foreign key that references itself",
            "We have the following self referencing foreign key: " + fk.stream()
              .map(f -> f.getChildColumns().stream().map(ColumnDef::getColumnName).collect(Collectors.joining(", ")))
              .collect(Collectors.joining(" - "))).toString());
        }
      });

      // Check that there is only one foreign key on one column
      // ie a foreign column that have two foreign table references (not supported)
      targetSources.keySet().forEach(dp -> {
        List<ForeignKeyDef> fk = getSecondForeignKeysOnTheSameColumn(dp);
        if (!fk.isEmpty()) {
          throw new RuntimeException(Strings.createMultiLineFromStrings(
            "The data path (" + dp + ") has more than one foreign key definition on a column and that's not permitted",
            "We have the following double foreign keys: " + fk.stream()
              .map(f -> f.getChildColumns().stream().map(ColumnDef::getColumnName).collect(Collectors.joining(", ")))
              .collect(Collectors.joining(" - "))).toString());
        }
      });

      // Target Parent check
      // Parent not in the table set to load ?
      // If yes, add a transfer with the parent tables
      List<DataPath> targetDataPaths = new ArrayList<>(targetSources.keySet());
      for (DataPath targetDataPath : targetDataPaths) {
        if (!targetDataPath.getOrCreateRelationDef().getForeignKeys().isEmpty()) {
          for (ForeignKeyDef foreignKeyDef : targetDataPath.getOrCreateRelationDef().getForeignKeys()) {
            DataPath foreignDataPath = foreignKeyDef.getForeignPrimaryKey().getRelationDef().getDataPath();
            if (!targetSources.containsKey(foreignDataPath)) {
              long rows = foreignDataPath.getCount();
              if (rows == 0) {
                if (this.loadParent != null && this.loadParent) {
                  GenLog.LOGGER.info("The table (" + foreignDataPath + ") has no records, the option to load the parent is on, therefore the table will be loaded.");
                  this.addDummyTransfer(foreignDataPath);
                } else {
                  throw new RuntimeException("The table (" + targetDataPath + ") has a foreign key to the parent table (" + foreignDataPath + "). This table has no rows and the option to load parent is disabled, we cannot then generated rows in the table (" + targetDataPath + ")");
                }
              }
            }
          }
        }
      }

      // The load parent option above may have added a transfer
      targetDataPaths = new ArrayList<>(targetSources.keySet());


      // Primary Key
      // Merge the data def
      // We do this only now because the transfers may be added
      // in every order but when the merge add the foreign key,
      // the primary key should be already present
      // We start with the foreign table (ie createOrderedTables) to finish to the fact table
      for (DataPath targetDataPath : ForeignKeyDag.createFromPaths(targetDataPaths).getCreateOrdered()) {
        GenDataPath sourceDataPath = targetSources.get(targetDataPath);
        /**
         * Merge the data def and create primary keys
         */
        sourceDataPath.getOrCreateRelationDef().mergeDataDef(targetDataPath);
        /**
         * Build primary key generator
         * if missing
         * Needed in the next step to build the foreign key generators
         */
        sourceDataPath
          .getGenDataPathUtility()
          .buildMissingPrimaryKeyGenerators();
      }


      /**
       * Source Foreign data generator building
       * from the target definition
       */
      for (DataPath targetDataPath : targetDataPaths) {

        // Add the foreign collection generator to the foreign columns
        targetDataPath
          .getOrCreateRelationDef()
          .getForeignKeys()
          .forEach(targetForeignKey -> {

            // Only one column relationship is supported
            if (targetForeignKey.getChildColumns().size() > 1) {
              String cols = targetForeignKey.getChildColumns().stream().map(ColumnDef::getColumnName).collect(Collectors.joining(", "));
              throw new RuntimeException("The foreign key of the data path (" + targetDataPath + ") has more than one columns (" + cols + "and it's not yet supported");
            }

            /**
             * Get the source foreign column
             */
            String targetForeignTableName = targetDataPath.getName();
            ColumnDef targetForeignColumn = targetForeignKey.getChildColumns().get(0);
            GenColumnDef sourceForeignColumn = targetSources.values()
              .stream()
              .filter(dp -> dp.getName().equals(targetForeignTableName))
              .flatMap(dp -> dp.getOrCreateRelationDef().getColumnDefs().stream())
              .filter(c -> c.getColumnName().equals(targetForeignColumn.getColumnName()))
              .findFirst()
              .orElse(null);
            assert sourceForeignColumn != null : "The foreign column (" + targetForeignTableName + "." + targetForeignColumn + ") was not found in the source data paths to transfer.";

            // Try to get the primary table generator
            final String primaryKeyTableName = targetForeignKey.getForeignPrimaryKey().getRelationDef().getDataPath().getName();
            GenDataPath sourcePrimaryTable = targetSources.values()
              .stream()
              .filter(dp -> dp.getName().equals(primaryKeyTableName))
              .findFirst()
              .orElse(null);

            // The primary column (used in the two block of the if statement below)
            ColumnDef targetPrimaryColumn = targetForeignKey.getForeignPrimaryKey().getColumns().get(0);

            // If not defined
            if (sourcePrimaryTable == null) {

              // The table is not part of the data generation specification
              GenLog.LOGGER.warning("The data generation for the column (" + targetForeignKey.getForeignPrimaryKey() + ") is not defined, we are then obliged to retrieve all data of this column to build the data generator for the foreign column (" + sourceForeignColumn + ")");
              sourceForeignColumn.setGenerator(
                (new ForeignColumnGenerator<>(sourceForeignColumn.getClazz(), targetPrimaryColumn))
                  .setColumnDef(sourceForeignColumn)
              );

            } else {

              /**
               * The table is in the data generation definition, we give then this column
               * to the foreign key generator
               */
              GenColumnDef sourcePrimaryColumn = sourcePrimaryTable.getOrCreateRelationDef().getColumnDefs()
                .stream()
                .filter(c -> c.getColumnName().equals(targetPrimaryColumn.getColumnName()))
                .findFirst()
                .orElse(null);

              if (sourcePrimaryColumn == null) {
                throw new RuntimeException("The data resource ( " + sourcePrimaryTable + ") has a data generation definition that is missing the primary column " + targetPrimaryColumn.getColumnName() + " of its target");
              } else {
                sourceForeignColumn.setGenerator(
                  (new ForeignColumnGenerator<>(sourceForeignColumn.getClazz(), sourcePrimaryColumn))
                    .setColumnDef(sourceForeignColumn)
                );
              }

            }
          })
        ;
      }

      // Building the data generators not defined
      // This should stay at the end
      targetSources.values().forEach(sourceDataPath -> sourceDataPath
        .getGenDataPathUtility()
        .buildMissingGenerators()
      );

      // Check that the size is not abyssal
      targetSources.values().forEach(dp -> {
        Tabulars.createIfNotExist(dp);
        long numberOfRowToInsert = dp.getCount();
        if (numberOfRowToInsert > MAX_INSERT) {
          throw new RuntimeException(
            Strings.createMultiLineFromStrings("The generator (" + dp + ") may generate (" + numberOfRowToInsert + ") records which is bigger than the upper limit of (" + MAX_INSERT + ").",
              "Set a MaxSize property in your data generation file or at the command line to resolve this issue.").toString());
        }
      });

    }

    return this;
  }


  /**
   * Do we load the foreign target table ?
   * Default: false
   *
   * @param loadParent load also the parent ?
   * @return the data generator
   */
  public DataGenerator loadDependencies(Boolean loadParent) {
    this.loadParent = loadParent;
    return this;
  }


  /**
   * Add a {@link #generateSourceTargetMap()}
   * without totalRows specification
   */
  public DataGenerator addDummyTransfer(DataPath dataPath) {
    return addDummyTransfer(dataPath, null);
  }

  /**
   * Add several target table at once with their total rows
   *
   * @param maxRowCount - the maximum total of Rows
   */
  public DataGenerator addDummyTransfers(List<DataPath> dataPaths, Long maxRowCount) {
    for (DataPath dataPath : dataPaths) {
      this.addDummyTransfer(dataPath, maxRowCount);
    }
    return this;
  }

  /**
   * Add several transfer to the same target at once
   */
  public DataGenerator addTransfers(List<GenDataPath> sourceDataPaths, DataPath targetDataPath) {
    for (GenDataPath sourceDataPath : sourceDataPaths) {
      this.addTransfer(sourceDataPath, targetDataPath);
    }
    return this;
  }

  /**
   * Add a transfer
   */
  public DataGenerator addTransfer(GenDataPath sourceDataPath, DataPath targetDataPath) {

    Objects.requireNonNull(sourceDataPath);
    Objects.requireNonNull(targetDataPath);

    if (Tabulars.isContainer(targetDataPath)) {
      targetDataPath = targetDataPath.getChild(sourceDataPath.getLogicalName());
    }

    // Add the transfers
    GenMemDataPath sourceGenMemDataPath;
    if (sourceDataPath instanceof GenMemDataPath) {
      sourceGenMemDataPath = (GenMemDataPath) sourceDataPath;
    } else {
      /**
       * This is a {@link GenFsDataPath}
       * and we need to migrate it to a single data store (ie the chosen one is the memory data store)
       * in order to keep fk consistency
       */
      sourceGenMemDataPath = toGenMemDataPath(sourceDataPath);
    }

    /**
     * Copy primary key from target
     * Foreign Key happens in order
     * in the {@link #generateSourceTargetMap()}
     */
    sourceGenMemDataPath
      .getOrCreateRelationDef()
      .mergeStruct(targetDataPath)
      .getDataPath();

    /**
     * Add the transfers
     */
    targetSources.put(targetDataPath, sourceGenMemDataPath);
    return this;

  }

  public List<GenDataPath> getGenDataPaths() {
    return new ArrayList<>(targetSources.values());
  }


  /**
   * A utility function used mostly during testing
   */
  public DataGenerator throwErrorIfFail() {

    if (transferManager.getExitStatus() != 0) {
      throw new RuntimeException("The load has seen (" + transferManager.getExitStatus() + ")");
    }
    return this;
  }


  @Override
  public void close() {
    this.memoryConnection.getTabular().dropConnection(this.memoryConnection);
  }


  public Map<GenMemDataPath, DataPath> getSourceTarget() {
    return targetSources
      .entrySet()
      .stream()
      .collect(Collectors.toMap(
        Map.Entry::getValue,
        Map.Entry::getKey
      ));
  }

  public GenMemDataPath createGenDataPath(String name) {
    return (GenMemDataPath) this.memoryConnection.getTypedDataPath(GenDataPathType.DATA_GEN, name);
  }

}
