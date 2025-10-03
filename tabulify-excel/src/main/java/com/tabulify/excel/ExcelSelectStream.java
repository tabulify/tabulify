package com.tabulify.excel;

import com.tabulify.model.ColumnDef;
import com.tabulify.model.RelationDef;
import com.tabulify.stream.SelectStreamAbs;
import org.apache.poi.openxml4j.opc.PackageAccess;

import java.sql.SQLException;

public class ExcelSelectStream extends SelectStreamAbs {

  private final ExcelDataPath excelDataPath;
  private final ExcelResultSet resultSet;

  public ExcelSelectStream(ExcelDataPath excelDataPath) {
    super(excelDataPath);
    this.excelDataPath = excelDataPath;
    this.resultSet = new ExcelResultSet(excelDataPath.getExcelSheet(PackageAccess.READ));
  }

  @Override
  public boolean next() {
    return resultSet.next();
  }

  @Override
  public void close() {
    resultSet.close();
  }

  @Override
  public boolean isClosed() {
    return resultSet.isClosed();
  }

  @Override
  public String getString(int columnIndex) {
    return String.valueOf(getObject(columnIndex));
  }

  @Override
  public long getRecordId() {
    return resultSet.getRow();
  }


  @Override
  public Object getObject(ColumnDef columnDef) {
    Class<?> typeCode = columnDef.getDataType().getValueClass();
    try {
      return resultSet.getObject(columnDef.getColumnPosition(), typeCode);
    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public RelationDef getRuntimeRelationDef() {
    return this.excelDataPath.getOrCreateRelationDef();
  }

  @Override
  public Double getDouble(int columnIndex) {
    try {
      return this.resultSet.getDouble(columnIndex);
    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
  }


  @Override
  public Integer getInteger(int columnIndex) {
    try {
      return resultSet.getInt(columnIndex);
    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public Object getObject(String columnName) {
    try {
      return resultSet.getObject(columnName);
    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public void beforeFirst() {
    resultSet.beforeFirst();
  }


}
