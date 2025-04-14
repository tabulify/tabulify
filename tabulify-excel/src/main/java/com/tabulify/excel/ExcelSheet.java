package com.tabulify.excel;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CellType;

import java.sql.Types;


public class ExcelSheet {


  public static int toSqlType(Cell cell, String defaultFormatString) {

    CellType cellType = cell.getCellType();
    if (cellType == CellType.NUMERIC || cellType == CellType.BLANK) {
      // A numeric can be a date
      CellStyle cellstyle = cell.getCellStyle();
      String formatString = cellstyle.getDataFormatString();
      // A backslash comes from nowhere
      // Example: dd/mm/yyyy\ hh:mm:ss
      // Suppressing it
      formatString = formatString.replace("\\ ", " ");
      if (formatString.equals(defaultFormatString)) {
        return Types.DATE;
      }
      if (cellType == CellType.NUMERIC) {
        return Types.NUMERIC;
      }
      return Types.VARCHAR;
    }

    switch (cellType) {
      case BOOLEAN:
        return Types.BOOLEAN;
      case STRING:
      case _NONE:
        return Types.VARCHAR;
      default:
        throw new IllegalArgumentException("The cell type (" + cellType + ") of the cell (" + cell.getRowIndex() + "," + cell.getColumnIndex() + ") can not be mapped to a intern data type.");

    }


  }
}
