package net.bytle.db.transfer;

import net.bytle.db.model.ColumnDef;
import net.bytle.db.spi.DataPath;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * A class that model a transfer:
 * * make a relation between a source and a target
 * * make a relation between columns
 * * and got the properties
 */
public class TransferSourceTarget {


  private final DataPath target;
  private final DataPath source;
  private TransferProperties transferProperties;

  // How the column mapping of the transfer is done
  private int columnMappingMethod = COLUMN_MAPPING_BY_POSITION;
  // The method
  // The default
  private static final int COLUMN_MAPPING_BY_POSITION = 1;

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
  private Map<Integer, Integer> columnMappingByMap = null;


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

  public TransferProperties getTransferProperties() {
    return transferProperties;
  }

  public TransferSourceTarget setProperty(TransferProperties transferProperties) {
    this.transferProperties = transferProperties;
    return this;
  }

  /**
   * @return a map of the source and target by their column position
   */
  public Map<Integer, Integer> getColumnMapping() {
    switch (columnMappingMethod) {
      case COLUMN_MAPPING_BY_POSITION:
        return Arrays.stream(source.getOrCreateDataDef().getColumnDefs())
          .collect(Collectors.toMap(ColumnDef::getColumnPosition, ColumnDef::getColumnPosition));
      case COLUMN_MAPPING_BY_NAME:
        return Arrays.stream(source.getOrCreateDataDef().getColumnDefs())
          .collect(Collectors.toMap(ColumnDef::getColumnPosition, c -> target.createDataDef().getColumn(c.getColumnName()).getColumnPosition()));
      case COLUMN_MAPPING_BY_MAP:
        return columnMappingByMap;
      default:
        throw new RuntimeException("Mapping method is unknown. There is a functional bug");
    }
  }

  /**
   * The column mapping will be done by {@link ColumnDef#getColumnName() column name}
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
    this.columnMappingByMap = columnMappingByMap;
    this.columnMappingMethod = COLUMN_MAPPING_BY_MAP;
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
}
