package net.bytle.db.transfer;

import net.bytle.db.model.ColumnDef;
import net.bytle.db.spi.DataPath;
import net.bytle.type.MapBiDirectional;

import java.util.Arrays;
import java.util.List;
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
  private MapBiDirectional<Integer, Integer> columnMappingByMap = null;


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
   *
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
    this.columnMappingByMap = new MapBiDirectional();
    this.columnMappingByMap.putAll(columnMappingByMap);
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

  /**
   *
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
      .map(targetId->columnMapping.getKey(targetId))
      .collect(Collectors.toList());

  }
}
