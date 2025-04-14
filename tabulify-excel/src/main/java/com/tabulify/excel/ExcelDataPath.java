package com.tabulify.excel;

import com.tabulify.fs.FsConnection;
import com.tabulify.fs.binary.FsBinaryDataPath;
import com.tabulify.model.RelationDef;
import com.tabulify.model.RelationDefDefault;
import com.tabulify.spi.DataPath;
import com.tabulify.stream.InsertStream;
import com.tabulify.stream.SelectStream;
import com.tabulify.transfer.TransferProperties;
import net.bytle.exception.InternalException;
import net.bytle.exception.NoValueException;
import net.bytle.exception.NoVariableException;
import net.bytle.type.MediaTypes;
import net.bytle.type.Variable;
import org.apache.commons.collections4.bidimap.TreeBidiMap;
import org.apache.poi.openxml4j.opc.PackageAccess;
import org.apache.poi.ss.usermodel.Cell;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

public class ExcelDataPath extends FsBinaryDataPath {


  private ExcelSheet excelSheet;

  public ExcelDataPath(FsConnection fsConnection, Path path) {
    super(fsConnection, path, MediaTypes.EXCEL_FILE);

    this.addVariablesFromEnumAttributeClass(ExcelDataPathAttribute.class);
  }

  @Override
  public RelationDef getOrCreateRelationDef() {
    if (this.relationDef == null) {

      this.relationDef = new RelationDefDefault(this);
      if (!Files.exists(this.getNioPath())) {
        return this.relationDef;
      }
      this.excelSheet = this.getExcelSheet(PackageAccess.READ);


      ExcelResultSet excelResultSet = new ExcelResultSet(this.excelSheet);
      TreeBidiMap<String, Integer> headerNames = excelResultSet.getHeaderNames();
      for (Map.Entry<Integer, Cell> type : excelResultSet.getColumnTypes().entrySet()) {
        Integer columnId = type.getKey();
        Integer typeCode = ExcelSheets.toSqlType(type.getValue());
        String columnName = "col" + columnId;
        if (headerNames != null) {
          columnName = headerNames.getKey(columnId);
        }
        this.relationDef.addColumn(columnName, typeCode);
      }

    }
    return this.relationDef;
  }

  /**
   * A internal function to build the  Excel result set only once lazily
   *
   * @param packageAccess - the package access if important (if not read, it will write and mess up with it)
   * @return the Excel result set
   */
  protected ExcelSheet getExcelSheet(PackageAccess packageAccess) {
    if (excelSheet == null) {
      this.excelSheet = ExcelSheet.config(this.getNioPath(), packageAccess)
        .setHeaderId(getHeaderRowId())
        .setSheetName(getSheetName())
        .setDataPath(this)
        .build();
    }
    return excelSheet;
  }

  private String getDefaultDateFormat() {
    try {
      Variable variable = this.getVariable(ExcelDataPathAttribute.DATE_FORMAT);
      return (String) variable.getValueOrDefault();
    } catch (NoVariableException | NoValueException e) {
      throw new InternalException("The DATE_FORMAT has already a default, this should not happen", e);
    }
  }

  /**
   * @return the header row number (0, no header)
   */
  public int getHeaderRowId() {

    try {
      Variable variable = this.getVariable(ExcelDataPathAttribute.HEADER_ROW_ID);
      return (int) variable.getValueOrDefault();
    } catch (NoVariableException | NoValueException e) {
      throw new InternalException("The HEADER_ROW_ID has already a default, this should not happen", e);
    }

  }


  /**
   * @return the header row number (0, no header)
   */
  public String getSheetName() {

    try {
      return (String) this.getVariable(ExcelDataPathAttribute.SHEET_NAME).getValueOrDefault();
    } catch (NoVariableException | NoValueException e) {
      return null;
    }

  }

  @Override
  public SelectStream getSelectStream() {
    return new ExcelSelectStream(this);
  }

  public ExcelDataPath setHeaderId(int i) {
    try {
      this.getVariable(ExcelDataPathAttribute.HEADER_ROW_ID).setOriginalValue(i);
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
  public InsertStream getInsertStream(DataPath source, TransferProperties transferProperties) {
    return new ExcelInsertStream(this, transferProperties);
  }

}
