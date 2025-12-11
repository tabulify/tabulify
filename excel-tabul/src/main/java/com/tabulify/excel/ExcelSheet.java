package com.tabulify.excel;

import com.tabulify.model.RelationDef;
import com.tabulify.model.SqlDataType;
import com.tabulify.exception.CastException;
import com.tabulify.exception.InternalException;
import com.tabulify.fs.Fs;
import com.tabulify.type.Casts;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.openxml4j.opc.OPCPackage;
import org.apache.poi.openxml4j.opc.PackageAccess;
import org.apache.poi.poifs.filesystem.POIFSFileSystem;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Types;
import java.util.Date;

/**
 * An Excel sheet wrapper that is only Nio Path dependent
 */
public class ExcelSheet {


  // Use in a local sourceResultSet in order to read the Excel File in Stream
  // not completely in memory
  private final OPCPackage pkg;

  // The Workbook and the sheet containing the data
  private final Workbook wb;
  private final Sheet sheet;

  private final POIFSFileSystem poifsFileSystem;
  private final ExcelSheetConfig excelSheetConfig;
  private final CreationHelper createHelper;
  private final CellStyle timestampCellStyle;
  private final CellStyle dateCellStyle;


  public ExcelSheet(ExcelSheetConfig excelSheetConfig) {
    this.excelSheetConfig = excelSheetConfig;
    Path path = excelSheetConfig.path;

    // A workbook, either a .xls HSSFWorkbook, or a .xlsx XSSFWorkbook,

    // Files vs InputStreams
    // The Workbook can be loaded from either a File or an InputStream.
    // Using a File object allows for lower memory consumption, while an InputStream requires more memory as it has to buffer the whole file.
    // https://poi.apache.org/components/spreadsheet/quick-guide.html#FileInputStream

    boolean existingFile;
    try {
      existingFile = Files.exists(path) && Files.size(path) != 0;
    } catch (IOException e) {
      throw new RuntimeException("Error while reading the size of " + path.toAbsolutePath(), e);
    }
    try {
      String extension = Fs.getExtension(path);
      boolean localFile = path.getFileSystem().provider().getScheme().equals("file");
      switch (extension) {
        case ExcelManagerProvider.XLSX:
          if (localFile) {
            // doesn't need to hold the whole zip file in memory, and can take advantage of native methods
            if (existingFile) {
              this.pkg = OPCPackage.open(path.toString(), excelSheetConfig.access);
              this.wb = new XSSFWorkbook(pkg);
            } else {
              this.wb = new XSSFWorkbook();
              this.pkg = null;
            }
          } else {
            // need to hold the whole zip sourceResultSet in memory, and can not take advantage of native methods
            if (Files.exists(path)) {
              this.wb = new XSSFWorkbook(Files.newInputStream(path));
            } else {
              this.wb = new XSSFWorkbook();
            }
            this.pkg = null;
          }
          this.poifsFileSystem = null;
          break;
        case ExcelManagerProvider.XLS:
          if (localFile) {
            if (Files.exists(path)) {
              this.poifsFileSystem = new POIFSFileSystem(path.toFile());
              this.wb = new HSSFWorkbook(poifsFileSystem.getRoot(), true);
            } else {
              this.wb = new HSSFWorkbook();
              this.poifsFileSystem = null;
            }

          } else {
            // need to hold the whole zip sourceResultSet in memory, and can not take advantage of native methods
            if (Files.exists(path)) {
              this.wb = new HSSFWorkbook(Files.newInputStream(path));
            } else {
              this.wb = new HSSFWorkbook();
            }
            this.poifsFileSystem = null;
          }
          this.pkg = null;
        default:
          throw new RuntimeException("Internal error: extension (" + extension + ") not taken into account");

      }

    } catch (Exception e) {
      throw new RuntimeException(e);
    }

    /**
     * Helper to create cell values
     */
    createHelper = wb.getCreationHelper();
    timestampCellStyle = wb.createCellStyle();
    String timestampFormat = this.excelSheetConfig.getTimestampFormat();
    timestampCellStyle.setDataFormat(createHelper.createDataFormat().getFormat(timestampFormat));
    dateCellStyle = wb.createCellStyle();
    String dateFormat = this.excelSheetConfig.getDateFormat();
    dateCellStyle.setDataFormat(createHelper.createDataFormat().getFormat(dateFormat));

    /**
     * Get the sheet
     */
    String sheetName = excelSheetConfig.getSheetName();
    if (existingFile) {
      if (sheetName == null) {
        this.sheet = wb.getSheetAt(0);
      } else {
        this.sheet = wb.getSheet(sheetName);
      }
    } else {
      if (sheetName == null) {
        this.sheet = wb.createSheet();
      } else {
        this.sheet = wb.createSheet(sheetName);
      }
    }
  }


  public static ExcelSheetConfig config(Path pathObj, PackageAccess packageAccess) {
    return new ExcelSheetConfig(pathObj, packageAccess);
  }

  public int getHeaderRowId() {
    return this.excelSheetConfig.headerId;
  }

  public Sheet getSheet() {
    return this.sheet;
  }

  public boolean isClosed() {
    return sheet == null;
  }

  /**
   * Create the headers
   */
  ExcelSheet createHeaders() {

    if (this.excelSheetConfig.excelDataPath == null) {
      throw new IllegalStateException("excel data path should not be null");
    }

    Row headerRow = this.sheet.createRow(this.getHeaderRowId() - 1);
    RelationDef relationDef = this.excelSheetConfig.excelDataPath.getRelationDef();
    for (int i = 1; i <= relationDef.getColumnsSize(); i++) {

      String headerName = relationDef.getColumnDef(i).getColumnName();

      // Construct the cell
      Cell headerCell = headerRow.createCell(i - 1);
      headerCell.setCellValue(headerName);
      this.sheet.autoSizeColumn(i);

    }
    return this;

  }

  public Workbook getWorkbook() {
    return this.wb;
  }

  /**
   * See also the counterpart {@link ExcelSheets#getCellValue(Cell, Class)}
   */
  public void setCellValue(Cell cell, Object value) throws CastException {

    // https://poi.apache.org/components/spreadsheet/quick-guide.html#CreateCells

    if (value == null) {
      // no cell value is null?
      return;
    }

    if (this.excelSheetConfig.excelDataPath == null) {
      throw new InternalException("Internal: The excel data path is empty");
    }

    // Relation def is mandatory for setting or reading a cell
    RelationDef relationDef = this.excelSheetConfig.excelDataPath.getRelationDef();
    if (relationDef == null) {
      throw new InternalException("Internal: The relation def is empty");
    }

    SqlDataType dataType = relationDef.getColumnDef(cell.getColumnIndex() + 1).getDataType();
    if (dataType.isNumber()) {
      cell.setCellValue(Casts.cast(value, Float.class));
      return;
    }

    int typeCode = dataType.getVendorTypeNumber();
    switch (typeCode) {
      case Types.BOOLEAN:
        cell.setCellValue(Casts.cast(value, Boolean.class));
        return;
      case Types.TIMESTAMP:
        cell.setCellStyle(timestampCellStyle);
        cell.setCellValue(Casts.cast(value, Date.class));
        return;
      case Types.DATE:
        cell.setCellStyle(dateCellStyle);
        cell.setCellValue(Casts.cast(value, Date.class));
        return;
      case Types.DOUBLE:
      case Types.NUMERIC:
      case Types.FLOAT:
      case Types.INTEGER:
        cell.setCellValue(Casts.cast(value, Double.class));
        return;
      case Types.VARCHAR:
      case Types.NVARCHAR:
      case Types.CHAR:
      case Types.NCHAR:
      default:
        // All other format as string
        // There is also a setCellValue("a string");
        cell.setCellValue(createHelper.createRichTextString(value.toString()));

    }

  }


  public static class ExcelSheetConfig {
    private final Path path;
    private final PackageAccess access;
    private int headerId = 0;
    private String sheetName = null;

    private String timestampFormat = ExcelDataPathAttribute.Constants.DEFAULT_TIMESTAMP_FORMAT;
    private ExcelDataPath excelDataPath;
    private String dateFormat = ExcelDataPathAttribute.Constants.DEFAULT_DATE_FORMAT;

    public ExcelSheetConfig(Path pathObj, PackageAccess packageAccess) {
      this.path = pathObj;
      this.access = packageAccess;
    }

    public ExcelSheetConfig setTimestampFormat(String timestampFormat) {
      this.timestampFormat = timestampFormat;
      return this;
    }

    public ExcelSheetConfig setDateFormat(String dateFormat) {
      this.dateFormat = dateFormat;
      return this;
    }

    public ExcelSheetConfig setHeaderId(int i) {
      this.headerId = i;
      return this;
    }

    // relation def may be `build` at read time or at def time, ie later
    // ie after ExcelSheet creation
    // we inject it to get the column data type
    public ExcelSheetConfig setDataPath(ExcelDataPath excelDataPath) {
      this.excelDataPath = excelDataPath;
      return this;
    }


    public ExcelSheet build() {
      return new ExcelSheet(this);
    }

    public String getSheetName() {
      return this.sheetName;
    }

    public ExcelSheetConfig setSheetName(String sheetName) {
      this.sheetName = sheetName;
      return this;
    }

    public String getTimestampFormat() {
      return this.timestampFormat;
    }

    public String getDateFormat() {
      return this.dateFormat;
    }
  }

  void close() {
    // https://poi.apache.org/components/spreadsheet/quick-guide.html#ReadWriteWorkbook
    try {
      if (this.poifsFileSystem != null) {
        this.poifsFileSystem.close();
      }
      if (this.pkg != null) {
        // close may change the package
        this.pkg.close();
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    if (this.wb != null && (this.excelSheetConfig.access == PackageAccess.READ_WRITE || this.excelSheetConfig.access == PackageAccess.WRITE)) {

      try (OutputStream fileOut = new FileOutputStream(this.excelSheetConfig.path.toFile())) {
        wb.write(fileOut);
      } catch (IOException e) {
        throw new RuntimeException(e);
      }

    }
  }
}
