package com.tabulify.excel;

import net.bytle.exception.CastException;
import net.bytle.type.Casts;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DateUtil;

import java.sql.Types;

/**
 * Static Utility
 */
public class ExcelSheets {
  /**
   * @param cell - the cell
   * @return Sql types code from {@link Types}
   */
  public static int toSqlType(Cell cell) {

    CellType cellType = cell.getCellType();


    switch (cellType) {
      case BOOLEAN:
        return Types.BOOLEAN;
      case STRING:
      case _NONE:
      case FORMULA:
        return Types.VARCHAR;
      case BLANK:
        if (DateUtil.isCellDateFormatted(cell)) {
          return Types.DATE;
        }
        return Types.VARCHAR;
      case NUMERIC:
        if (DateUtil.isCellDateFormatted(cell)) {
          return Types.DATE;
        }
        return Types.NUMERIC;
      default:
        throw new IllegalArgumentException("The cell type (" + cellType + ") of the cell (" + cell.getRowIndex() + "," + cell.getColumnIndex() + ") can not be mapped to a intern data type.");

    }


  }

  static <T> T getCellValue(Cell cell, Class<T> clazz) {

    // https://poi.apache.org/components/spreadsheet/quick-guide.html#CellContents

    String sheetRowIndex = String.valueOf(cell.getRowIndex());
    String sheetColumnIndex = String.valueOf(cell.getColumnIndex());

    Object value;
    CellType cellType = cell.getCellType();
    switch (cellType) {
      case STRING:
        value = cell.getRichStringCellValue().getString();
        break;
      case NUMERIC:
        // Special case because Date are stored as numeric
        // Example ise: DateUtil.isCellDateFormatted(cell)
        if (DateUtil.isCellDateFormatted(cell)) {
          if (java.util.Date.class.equals(clazz)) {
            //noinspection unchecked
            return (T) cell.getDateCellValue();
          }
          if (java.sql.Date.class.equals(clazz)) {
            java.util.Date dateCellValue = cell.getDateCellValue();
            if (dateCellValue == null) {
              return null;
            }
            //noinspection unchecked
            return (T) new java.sql.Date(dateCellValue.getTime());
          }
        }
        value = cell.getNumericCellValue();
        break;
      case BLANK:
        if (clazz.equals(String.class)) {
          value = "";
          break;
        }
        value = null;
        break;
      case _NONE:
        value = null;
        break;
      case BOOLEAN:
        value = cell.getBooleanCellValue();
        break;
      case FORMULA:
        value = cell.getCellFormula();
        break;
      case ERROR:
        throw new RuntimeException("Error type have no value. The cell with the Excel coordinates (" + sheetRowIndex + "," + sheetColumnIndex + ") is of type " + cellType);
      default:
        throw new RuntimeException("Internal Error: Type not yet supported. The cell with the Excel coordinates (" + sheetRowIndex + "," + sheetColumnIndex + ") is of type " + cellType);
    }

    try {
      return Casts.cast(value, clazz);
    } catch (CastException e) {
      throw new RuntimeException(e);
    }
  }
}
