package com.tabulify.excel;

import com.tabulify.conf.Attribute;
import com.tabulify.fs.FsConnection;
import com.tabulify.fs.FsDataPath;
import com.tabulify.fs.binary.FsBinaryDataPath;
import com.tabulify.model.RelationDef;
import com.tabulify.model.RelationDefDefault;
import com.tabulify.model.SqlDataTypeAnsi;
import com.tabulify.spi.DataPath;
import com.tabulify.stream.InsertStream;
import com.tabulify.stream.SelectStream;
import com.tabulify.transfer.TransferPropertiesSystem;
import com.tabulify.exception.InternalException;
import com.tabulify.exception.NoVariableException;
import org.apache.commons.collections4.bidimap.TreeBidiMap;
import org.apache.poi.openxml4j.opc.PackageAccess;
import org.apache.poi.ss.usermodel.Cell;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

public class ExcelDataPath extends FsBinaryDataPath {


  private ExcelSheet excelSheet;

  public ExcelDataPath(FsConnection fsConnection, Path path) {
    super(fsConnection, path, ExcelMediaType.castFromPath(path));

    this.addVariablesFromEnumAttributeClass(ExcelDataPathAttribute.class);
  }


  public ExcelDataPath(FsDataPath fsDataPath) {
    this(fsDataPath.getConnection(), fsDataPath.getNioPath());
  }


  @Override
  public RelationDef getOrCreateRelationDef() {
    if (this.relationDef == null) {

      this.relationDef = new RelationDefDefault(this);
      if (!Files.exists(this.getAbsoluteNioPath())) {
        return this.relationDef;
      }
      this.excelSheet = this.getExcelSheet(PackageAccess.READ);


      try (ExcelResultSet excelResultSet = new ExcelResultSet(this.excelSheet)) {
        TreeBidiMap<String, Integer> headerNames = excelResultSet.getHeaderNames();
        for (Map.Entry<Integer, Cell> type : excelResultSet.getHeaderColumnTypes().entrySet()) {
          Integer columnId = type.getKey();
          SqlDataTypeAnsi typeCode = ExcelSheets.toSqlType(type.getValue());
          String columnName = "col" + columnId;
          if (headerNames != null) {
            columnName = headerNames.getKey(columnId);
          }
          this.relationDef.addColumn(columnName, typeCode);
        }
      }

    }
    return this.relationDef;
  }


  /**
   * An internal function to build the  Excel result set only once lazily
   *
   * @param packageAccess - the package access if important (if not read, it will write and mess up with it)
   * @return the Excel result set
   */
  protected ExcelSheet getExcelSheet(PackageAccess packageAccess) {
    if (excelSheet == null) {
      this.excelSheet = ExcelSheet
        .config(this.getAbsoluteNioPath(), packageAccess)
        .setHeaderId(getHeaderRowId())
        .setTimestampFormat(getTimestampFormat())
        .setDateFormat(getDateFormat())
        .setSheetName(getSheetName())
        .setDataPath(this)
        .build();
    }
    return excelSheet;
  }

  private String getTimestampFormat() {
    try {
      Attribute attribute = this.getAttribute(ExcelDataPathAttribute.TIMESTAMP_FORMAT);
      return (String) attribute.getValueOrDefault();
    } catch (NoVariableException e) {
      throw new InternalException("The TIMESTAMP_FORMAT has already a default, this should not happen", e);
    }
  }

  private String getDateFormat() {
    try {
      Attribute attribute = this.getAttribute(ExcelDataPathAttribute.DATE_FORMAT);
      return (String) attribute.getValueOrDefault();
    } catch (NoVariableException e) {
      throw new InternalException("The DATE_FORMAT has already a default, this should not happen", e);
    }
  }

  /**
   * @return the header row number (0, no header)
   */
  public int getHeaderRowId() {

    try {
      Attribute attribute = this.getAttribute(ExcelDataPathAttribute.HEADER_ROW_ID);
      return (int) attribute.getValueOrDefault();
    } catch (NoVariableException e) {
      throw new InternalException("The HEADER_ROW_ID has already a default, this should not happen", e);
    }

  }


  /**
   * @return the header row number (0, no header)
   */
  public String getSheetName() {

    try {
      return (String) this.getAttribute(ExcelDataPathAttribute.SHEET_NAME).getValueOrDefault();
    } catch (NoVariableException e) {
      return null;
    }

  }

  @Override
  public SelectStream getSelectStream() {
    return new ExcelSelectStream(this);
  }

  public ExcelDataPath setHeaderId(int i) {
    try {
      this.getAttribute(ExcelDataPathAttribute.HEADER_ROW_ID).setPlainValue(i);
      return this;
    } catch (NoVariableException e) {
      throw new InternalException("The HEADER_ROW_ID has already been added in the constructor, it should not happen");
    }
  }

  public void createFile() {

    excelSheet = this.getExcelSheet(PackageAccess.WRITE);
    if (this.getHeaderRowId() != 0) {
      excelSheet
        .createHeaders();
    }
    excelSheet.close();

  }

  @Override
  public boolean hasHeaderInContent() {
    return this.getHeaderRowId() != 0;
  }

  @Override
  public InsertStream getInsertStream(DataPath source, TransferPropertiesSystem transferProperties) {
    return new ExcelInsertStream(this, transferProperties);
  }

}
