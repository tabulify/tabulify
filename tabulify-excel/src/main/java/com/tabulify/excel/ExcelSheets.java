package com.tabulify.excel;

import com.tabulify.model.SqlDataTypeAnsi;
import net.bytle.exception.CastException;
import net.bytle.type.Casts;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.ExcelNumberFormat;

import java.sql.SQLException;
import java.sql.Types;

/**
 * Static Utility
 */
public class ExcelSheets {
  /**
   * @param cell - the cell
   * @return Sql types code from {@link Types}
   */
  public static SqlDataTypeAnsi toSqlType(Cell cell) {

    CellType cellType = cell.getCellType();

    switch (cellType) {
      case BOOLEAN:
        return SqlDataTypeAnsi.BOOLEAN;
      case STRING:
      case _NONE:
      case FORMULA:
        return SqlDataTypeAnsi.CHARACTER_VARYING;
      case BLANK:
        if (DateUtil.isCellDateFormatted(cell)) {
          String format = ExcelNumberFormat.from(cell, null).getFormat();
          if (format.length() <= 10) {
            return SqlDataTypeAnsi.DATE;
          }
          return SqlDataTypeAnsi.TIMESTAMP;
        }
        return SqlDataTypeAnsi.CHARACTER_VARYING;
      case NUMERIC:
        if (DateUtil.isCellDateFormatted(cell)) {
          String format = ExcelNumberFormat.from(cell, null).getFormat();
          if (format.length() <= 10) {
            return SqlDataTypeAnsi.DATE;
          }
          return SqlDataTypeAnsi.TIMESTAMP;
        }
        return SqlDataTypeAnsi.DOUBLE_PRECISION;
      default:
        throw new IllegalArgumentException("The cell type (" + cellType + ") of the cell (" + cell.getRowIndex() + "," + cell.getColumnIndex() + ") can not be mapped to a intern data type.");

    }


  }

  /**
   * @throws SQLException because {@link ExcelResultSet} was first created and SQLException is mandatory. Use {@link #getCellValueSafe(Cell, Class)} to get rid of it
   *                      See {@link ExcelSheet#setCellValue(Cell, Object)}
   */
  static <T> T getCellValue(Cell cell, Class<T> clazz) throws SQLException {

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

          // We manipulate only java.sql.Date or java.sql.Timestamp
          // not (java.util.Date.class)
          java.util.Date dateCellValue = cell.getDateCellValue();
          if (dateCellValue == null) {
            return null;
          }
          if (clazz.equals(java.sql.Date.class)) {
            return clazz.cast(new java.sql.Date(dateCellValue.getTime()));
          }

          if (clazz.equals(java.sql.Timestamp.class)) {
            return clazz.cast(new java.sql.Timestamp(dateCellValue.getTime()));
          }

          // case when an Excel sheet has a header but the header is set to zero
          // All column type are then set to the class varchar
          if (clazz.equals(String.class)) {
            String format = ExcelNumberFormat.from(cell, null).getFormat();
            if (format.length() <= 10) {
              return clazz.cast((new java.sql.Date(dateCellValue.getTime())).toString());
            }
            return clazz.cast((new java.sql.Timestamp(dateCellValue.getTime())).toString());

          }

          if (clazz.equals(Object.class)) {
            String format = ExcelNumberFormat.from(cell, null).getFormat();
            if (format.length() <= 10) {
              return clazz.cast((new java.sql.Date(dateCellValue.getTime())));
            }
            return clazz.cast((new java.sql.Timestamp(dateCellValue.getTime())));
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
        throw new SQLException("Error type have no value. The cell with the Excel coordinates (" + sheetRowIndex + "," + sheetColumnIndex + ") is of type " + cellType);
      default:
        throw new SQLException("Internal Error: Type not yet supported. The cell with the Excel coordinates (" + sheetRowIndex + "," + sheetColumnIndex + ") is of type " + cellType);
    }

    try {
      return Casts.cast(value, clazz);
    } catch (CastException e) {
      throw new SQLException(e);
    }
  }


  static <T> T getCellValueSafe(Cell cell, Class<T> clazz) {
    try {
      return getCellValue(cell, clazz);
    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
  }
}
