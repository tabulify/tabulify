package net.bytle.db.gen;


import net.bytle.db.model.ColumnDef;
import net.bytle.db.model.DataDefAbs;
import net.bytle.db.model.RelationDef;
import net.bytle.db.spi.DataPathAbs;

import java.sql.Types;
import java.util.HashSet;
import java.util.Set;

/**
 *
 */
public class GenDataDef extends DataDefAbs implements RelationDef {

  public static final String TYPE = "GEN";

  public static final int DEFAULT_DATA_TYPE = Types.VARCHAR;

  private Long maxRows;
  private Set<GenColumnDef> genColumns = new HashSet<>();

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
    Class clazz = this.getDataPath().getDataStore().getSqlDataType(DEFAULT_DATA_TYPE).getClazz();
    genColumns.add((GenColumnDef) GenColumnDef.of(this, columnName, clazz)
      .typeCode(type)
      .precision(precision)
      .scale(scale)
      .setNullable(nullable)
      .comment(comment)
      .setColumnPosition(genColumns.size()+1));
    return this;
  }

  @Override
  public ColumnDef[] getColumnDefs() {
    return new ColumnDef[0];
  }


  public GenDataDef addColumn(String columnName, Integer type, Integer precision, Boolean nullable) {
    this.addColumn(columnName, type, precision, null, nullable, null);
    return this;
  }

  public GenDataDef setMaxRows(long rows) {
    this.maxRows = rows;
    return this;
  }


  @Override
  public <T> GenColumnDef getColumnDef(String columnName) {
    return genColumns
      .stream()
      .filter(c->c.getColumnName().equals(columnName))
      .findFirst()
      .orElse(null);
  }

  @Override
  public <T> GenColumnDef getColumnDef(Integer columnIndex) {
    return genColumns
      .stream()
      .filter(c->c.getColumnPosition().equals(columnIndex))
      .findFirst()
      .orElse(null);
  }

  @Override
  public <T> GenColumnDef getColumnOf(String columnName, Class<T> clazz) {
    return (GenColumnDef) GenColumnDef.of(this, columnName, clazz);
  }

  @Override
  public int getColumnsSize() {
    return genColumns.size();
  }

  public Set<GenColumnDef> getGenColumnsDef() {
    return genColumns;
  }


  @Override
  public GenDataPath getDataPath() {
    return (GenDataPath) super.getDataPath();
  }

  public Long getMaxSize() {
    return maxRows;
  }
}
