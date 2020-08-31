package net.bytle.db.transfer;

import net.bytle.db.DbLoggers;
import net.bytle.db.model.ColumnDef;
import net.bytle.db.spi.DataPath;
import net.bytle.db.spi.Tabulars;
import net.bytle.log.Log;
import net.bytle.type.MapBiDirectional;
import net.bytle.type.Strings;

import java.sql.Types;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * A class that
 * * models a source target relationship (on data path but also columns level, see {@link #withColumnMappingByName()}
 * * has all source / target check method
 * * has source/target properties such as:
 * * {@link #withLoadOperation(TransferLoadOperation)}
 * * and columns mapping
 * <p>
 * For process related properties, see {@link TransferProperties}
 */
public class TransferSourceTarget {

  public static final Log LOGGER = Log.getLog(TransferSourceTarget.class);

  private final DataPath target;
  private final DataPath source;

  // How the column mapping of the transfer is done
  private int columnMappingMethod = COLUMN_MAPPING_BY_POSITION;
  // The method
  // The default
  private static final int COLUMN_MAPPING_BY_POSITION = 1;

  /**
   * The load operations
   * See {@link #withLoadOperation(TransferLoadOperation)}
   */
  private TransferLoadOperation loadOperation = TransferLoadOperation.INSERT;

  /**
   * By name, you use the function {@link #withColumnMappingByName()} to set it
   **/
  private static final int COLUMN_MAPPING_BY_NAME = 2;
  /**
   * By name, you use the function {@link #withColumnMappingByMap(Map<int, int>)} to set it
   **/
  private static final int COLUMN_MAPPING_BY_MAP = 3;
  /**
   * The variable that holds the custom column mapping that was set by {@link #withColumnMappingByMap(Map)}
   */
  private MapBiDirectional<Integer, Integer> columnMappingByMap = new MapBiDirectional<>();


  public TransferSourceTarget(DataPath sourceDataPath, DataPath targetDataPath) {
    this.source = sourceDataPath;
    this.target = targetDataPath;
  }

  public DataPath getSourceDataPath() {
    return source;
  }

  public DataPath getTargetDataPath() {
    return target;
  }

  @Override
  public String toString() {
    return " " + source + " > " + target + " ";
  }


  /**
   * @return a {@link MapBiDirectional bidirectional map} of the source and target by their column position
   */
  public MapBiDirectional<Integer, Integer> getColumnMapping() {
    switch (columnMappingMethod) {
      case COLUMN_MAPPING_BY_POSITION:
        // one on one (1=1, 2=2, ...)
        return Arrays.stream(source.getOrCreateDataDef().getColumnDefs())
          .collect(Collectors.toMap(ColumnDef::getColumnPosition, ColumnDef::getColumnPosition, (e1, e2) -> e1, MapBiDirectional::new));
      case COLUMN_MAPPING_BY_NAME:
        return Arrays.stream(source.getOrCreateDataDef().getColumnDefs())
          .collect(Collectors.toMap(ColumnDef::getColumnPosition, c -> target.createDataDef().getColumn(c.getColumnName()).getColumnPosition(), (e1, e2) -> e1, MapBiDirectional::new));
      case COLUMN_MAPPING_BY_MAP:
        return columnMappingByMap;
      default:
        throw new RuntimeException("Mapping method is unknown. There is a functional bug");
    }
  }

  /**
   * The column mapping will be done by {@link ColumnDef#getColumnName() column name}
   * <p>
   * By default, the column mapping is done by column position.
   * You can also give a custom column mapping relationship with the {@link #withColumnMappingByMap(Map)} function
   *
   * @return
   */
  public TransferSourceTarget withColumnMappingByName() {
    this.columnMappingMethod = COLUMN_MAPPING_BY_NAME;
    return this;
  }

  /**
   * The column mapping will be customized
   *
   * @param columnMappingByMap - A map of the source {@link ColumnDef#getColumnPosition() column position} against the target {@link ColumnDef#getColumnPosition() column position}
   * @return
   */
  public TransferSourceTarget withColumnMappingByMap(Map<Integer, Integer> columnMappingByMap) {
    // Reset the data to empty map
    this.columnMappingByMap = new MapBiDirectional<>();
    // Add each column - columnMapping function is the driver (it performs the test, set the method,...)
    columnMappingByMap.forEach(this::addColumnMapping);
    return this;
  }

  /**
   * The column mapping will happen with the {@link ColumnDef#getColumnPosition() position of the column}
   *
   * @return
   */
  public TransferSourceTarget withColumnMappingByPosition() {
    this.columnMappingMethod = COLUMN_MAPPING_BY_POSITION;
    return this;
  }

  /**
   * @return the column position of the source in the target order
   * This function is used during loading to retrieve the objects in a target order from the source
   * The id is the {@link ColumnDef#getColumnPosition() column position} and
   * you should extract 1 from to the column index used in {@link net.bytle.db.stream.SelectStream#getObject(int)}
   */
  public List<Integer> getSourceColumnPositionInTargetOrder() {

    MapBiDirectional<Integer, Integer> columnMapping = getColumnMapping();
    return columnMapping.values()
      .stream()
      .sorted()
      .map(targetId -> columnMapping.getKey(targetId))
      .collect(Collectors.toList());

  }

  /**
   * Add a column mapping between the source and the target
   *
   * @param sourceColumnPosition
   * @param targetColumnPosition
   * @return
   */
  public TransferSourceTarget addColumnMapping(int sourceColumnPosition, int targetColumnPosition) {
    assert source.getOrCreateDataDef().getColumnDef(sourceColumnPosition - 1) != null : "There is no column at the position (" + sourceColumnPosition + ") for the source (" + source + ")";
    assert target.getOrCreateDataDef().getColumnDef(targetColumnPosition - 1) != null : "There is no column at the position (" + targetColumnPosition + ") for the source (" + target + ")";
    this.columnMappingMethod = COLUMN_MAPPING_BY_MAP;
    columnMappingByMap.put(sourceColumnPosition, targetColumnPosition);
    return this;
  }

  /**
   * Check that the target has the same structure than the source.
   * Create it if it does not exist
   */
  private void checkOrCreateTargetStructureFromSource() {
    // If this for instance, the move of a file, the file may exist
    // but have no content and therefore no structure
    if (target.getOrCreateDataDef().getColumnsSize() != 0) {
      // Move
      if (loadOperation.equals(TransferLoadOperation.MOVE) || loadOperation.equals(TransferLoadOperation.COPY)) {
        for (ColumnDef columnDef : source.getOrCreateDataDef().getColumnDefs()) {
          ColumnDef targetColumnDef = target.getOrCreateDataDef().getColumnDef(columnDef.getColumnName());
          if (targetColumnDef == null) {
            String message = "Unable to " + loadOperation + " the data unit (" + source.toString() + ") because it exists already in the target location (" + target.toString() + ") with a different structure" +
              " (The source column (" + columnDef.getColumnName() + ") was not found in the target data unit)";
            DbLoggers.LOGGER_DB_ENGINE.severe(message);
            throw new RuntimeException(message);
          }
        }
      }
    } else {
      target.getOrCreateDataDef().copyDataDef(source);
    }
  }

  /**
   * Before a copy/move operations the target
   * table should exist.
   * <p>
   * If the target table:
   * - does not exist, creates the target table from the source
   * - exist, control that the column definition is the same
   */
  public void createOrCheckTargetFromSource() {
    // Check target
    final Boolean exists = Tabulars.exists(target);
    if (!exists) {
      target.getOrCreateDataDef().copyDataDef(source);
      Tabulars.create(target);
    } else {
      checkOrCreateTargetStructureFromSource();
    }
  }

  /**
   * Set load option:
   * * insert (append),
   * * update,
   * * merge (upsert)
   *
   * @param transferLoadOperation - an enum of {@link TransferLoadOperation}
   * @return the {@link TransferProperties} instance itself for chaining instantiation
   */
  public TransferSourceTarget withLoadOperation(TransferLoadOperation transferLoadOperation) {
    this.loadOperation = transferLoadOperation;
    return this;
  }

  /**
   * This function will check that the {@link #getColumnMapping() column mapping}
   * More particularly that the target data type must be able to receive the source data
   * Throws an {@link RuntimeException} if it's not the case
   */
  protected void checkColumnMappingDataType() {

    MapBiDirectional<Integer, Integer> columnMapping = getColumnMapping();
    columnMapping.entrySet().forEach(c -> {
      ColumnDef<Object> sourceColumn = source.getOrCreateDataDef().getColumnDef(c.getKey() - 1);
      ColumnDef<Object> targetColumn = target.getOrCreateDataDef().getColumnDef(c.getValue() - 1);
      if (sourceColumn.getDataType().getTypeCode() != targetColumn.getDataType().getTypeCode()) {
        String message = Strings.multiline(
          "There is a problem with a data loading mapping between two columns",
          "They have different data type and that may cause a problem during the load",
          "To resolve this problem, change the columns mapping or change the data type of the target column",
          "The problem is on the mapping (" + c + ") between the source column (" + sourceColumn + ") and the target column (" + targetColumn + ")",
          "where the source data type (" + sourceColumn.getDataType().getTypeName() + ") is different than the target data type (" + targetColumn.getDataType().getTypeName() + ")"
        );

        // A date in a varchar should work
        // A string in bigint should work if there is only numbers
        if (
          (sourceColumn.getDataType().getTypeCode() == Types.DATE && targetColumn.getDataType().getTypeCode() == Types.VARCHAR)
            || (sourceColumn.getDataType().getTypeCode() == Types.VARCHAR && targetColumn.getDataType().getTypeCode() == Types.BIGINT)
        ) {
          LOGGER.warning(message);
        } else {
          throw new RuntimeException(message);
        }
      }
    });
  }

  /**
   * Check a tabular source before moving
   * * check if it exists (except for query)
   * * check if it has a structure
   */
  public void checkSource() {
    // Check source
    if (!Tabulars.exists(source)) {
      // Is it a query definition
      if (source.getQuery() == null) {
        throw new RuntimeException("We cannot move the source data path (" + source + ") because it does not exist");
      }
    }
    if (source.getOrCreateDataDef().getColumnDefs().length == 0) {
      throw new RuntimeException("We cannot move this tabular data path (" + source + ") because it has no columns.");
    }
  }

}
