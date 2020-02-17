package net.bytle.db.gen;


import net.bytle.db.model.DataDefAbs;
import net.bytle.db.model.RelationDef;
import net.bytle.db.model.TableDef;
import net.bytle.db.spi.DataPath;
import net.bytle.db.spi.DataPathAbs;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Types;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

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

  public GenDataDef setMaxRows(long rows) {
    this.addProperty(GenDataDef.TOTAL_ROWS_PROPERTY_KEY, rows);
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
  public <T> GenColumnDef getColumnOf(String columnName, Class<T> clazz) {


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

  public Long getMaxSize() {
    return this.getPropertyAsLong(GenDataDef.TOTAL_ROWS_PROPERTY_KEY);
  }

  @Override
  public GenDataDef copy(DataPath sourceDataPath) {

    super.copy(sourceDataPath);
    return this;

  }


}
