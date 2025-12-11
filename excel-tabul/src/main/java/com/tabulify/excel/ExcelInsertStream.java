package com.tabulify.excel;

import com.tabulify.stream.InsertStream;
import com.tabulify.stream.InsertStreamAbs;
import com.tabulify.transfer.TransferPropertiesCross;
import com.tabulify.transfer.TransferPropertiesSystem;
import com.tabulify.exception.CastException;
import org.apache.poi.openxml4j.opc.PackageAccess;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;

import java.util.List;

public class ExcelInsertStream extends InsertStreamAbs implements InsertStream {


  private final ExcelSheet excelSheet;

  public ExcelInsertStream(ExcelDataPath fsDataPath, TransferPropertiesSystem transferPropertiesSystem) {

    super(fsDataPath);
    this.excelSheet = fsDataPath.getExcelSheet(PackageAccess.READ_WRITE);


  }


  /**
   * @param values - The values to insert
   * @return the {@link InsertStream} for insert chaining
   */
  @Override
  public ExcelInsertStream insert(List<Object> values) {

    // https://poi.apache.org/components/spreadsheet/quick-guide.html#CreateCells
    this.insertStreamListener.addRows(1);
    int lastRowNum = excelSheet.getSheet().getLastRowNum();
    Row row = this.excelSheet.getSheet().createRow(lastRowNum + 1);
    for (int i = 0; i < values.size(); i++) {
      // Create a cell and put a value in it.
      Cell cell = row.createCell(i);
      Object value = values.get(i);
      try {
        excelSheet.setCellValue(cell, value);
      } catch (CastException e) {
        throw new RuntimeException("Could not cast the value of the cell (" + row.getRowNum() + 1 + "+" + i + 1 + "), Value: " + value + ", Message:" + e.getMessage(), e);
      }

    }

    return this;
  }

  /**
   * Close the stream
   * ie
   * * commit the last rows
   * * close the connection
   * * ...
   */
  @Override
  public void close() {

    this.excelSheet.close();

  }

  @Override
  public void flush() {
    throw new UnsupportedOperationException();
  }


  @Override
  public ExcelDataPath getDataPath() {
    return (ExcelDataPath) super.getDataPath();
  }

}
