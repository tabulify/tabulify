package com.tabulify.excel;

import com.tabulify.fs.FsConnection;
import com.tabulify.fs.binary.FsBinaryDataPath;
import com.tabulify.model.RelationDef;
import com.tabulify.model.RelationDefDefault;
import com.tabulify.stream.SelectStream;
import net.bytle.exception.InternalException;
import net.bytle.exception.NoValueException;
import net.bytle.exception.NoVariableException;
import net.bytle.type.MediaTypes;
import net.bytle.type.Variable;
import org.apache.commons.collections4.bidimap.TreeBidiMap;
import org.apache.poi.openxml4j.opc.PackageAccess;

import java.nio.file.Path;
import java.util.Map;

public class ExcelDataPath extends FsBinaryDataPath {

  private ExcelResultSet excelResultSet;

  public ExcelDataPath(FsConnection fsConnection, Path path) {
    super(fsConnection, path, MediaTypes.EXCEL_FILE);

    this.addVariablesFromEnumAttributeClass(ExcelDataPathAttribute.class);
  }

  @Override
  public RelationDef getOrCreateRelationDef() {
    if (this.relationDef == null) {

      this.relationDef = new RelationDefDefault(this);
      excelResultSet = this.getExcelResultSet(PackageAccess.READ);

      TreeBidiMap<String, Integer> headerNames = excelResultSet.getHeaderNames();
      for (Map.Entry<Integer, Integer> type : excelResultSet.getColumnTypes().entrySet()) {
        Integer columnId = type.getKey();
        Integer typeCode = type.getValue();
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
  @SuppressWarnings("SameParameterValue")
  protected ExcelResultSet getExcelResultSet(PackageAccess packageAccess) {
    if (excelResultSet == null) {
      excelResultSet = new ExcelResultSet(this.getNioPath(), isHeaderPresent(), getSheetName(), packageAccess);
    }
    return this.excelResultSet;
  }

  /**
   * @return the header row number (0, no header)
   */
  public boolean isHeaderPresent() {

    try {
      Variable variable = this.getVariable(ExcelDataPathAttribute.HEADER_ROW_ID);
      int valueOrDefault = (int) variable.getValueOrDefault();
      switch (valueOrDefault) {
        case 0:
          return false;
        case 1:
          return true;
        default:
          throw new RuntimeException("The excel variable " + variable + " value should be 1 or 0, not " + valueOrDefault);
      }
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
}
