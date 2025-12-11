package com.tabulify.excel;

import com.tabulify.Tabular;
import com.tabulify.fs.FsDataPath;
import com.tabulify.model.SqlDataTypeAnsi;
import com.tabulify.exception.CastException;
import org.apache.poi.openxml4j.opc.PackageAccess;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.util.Date;

public class ExcelSheetTest {


  @Test
  void dateTest() throws CastException, SQLException {

    try (Tabular tabular = Tabular.tabularWithCleanEnvironment()) {

      FsDataPath tempFile = tabular.getTempFile("excelDateTest", ".xlsx");
      ExcelDataPath excelDataPath = (ExcelDataPath) new ExcelDataPath(tempFile)
        .getOrCreateRelationDef()
        .addColumn("date", SqlDataTypeAnsi.DATE)
        .getDataPath();
      ExcelSheet excelsheet = excelDataPath.getExcelSheet(PackageAccess.WRITE);
      Sheet sheet = excelsheet.getSheet();
      Row row = sheet.createRow(0);
      Cell cell = row.createCell(0);
      excelsheet.setCellValue(cell, new Date());
      Assertions.assertTrue(DateUtil.isCellDateFormatted(cell), "Cell is date formatted");
      Object value = ExcelSheets.getCellValue(cell, Object.class);
      Assertions.assertNotNull(value);
      Assertions.assertEquals(java.sql.Date.class, value.getClass());
      excelsheet.close();
    }
  }
}
