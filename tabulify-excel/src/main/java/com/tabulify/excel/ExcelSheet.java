package com.tabulify.excel;

import net.bytle.fs.Fs;
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

  public ExcelSheet(ExcelSheetConfig excelSheetConfig) {
    this.excelSheetConfig = excelSheetConfig;
    Path path = excelSheetConfig.path;
    // A workbook, either a .xls HSSFWorkbook, or a .xlsx XSSFWorkbook,

    // Files vs InputStreams
    // The Workbook can be loaded from either a File or an InputStream.
    // Using a File object allows for lower memory consumption, while an InputStream requires more memory as it has to buffer the whole file.
    // https://poi.apache.org/components/spreadsheet/quick-guide.html#FileInputStream

    try {
      String extension = Fs.getExtension(path);
      boolean localFile = path.getFileSystem().provider().getScheme().equals("file");
      switch (extension) {
        case ExcelManagerProvider.XLSX:
          if (localFile) {
            // doesn't need to hold the whole zip file in memory, and can take advantage of native methods
            if (Files.exists(path)) {
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
          throw new RuntimeException("Internal error: extension " + extension + " not taken into account");

      }

    } catch (Exception e) {
      throw new RuntimeException(e);
    }

    String sheetName = excelSheetConfig.getSheetName();
    if (Files.exists(path)) {
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



  public static class ExcelSheetConfig {
    private final Path path;
    private final PackageAccess access;
    private int headerId = 0;
    private String sheetName = null;

    public ExcelSheetConfig(Path pathObj, PackageAccess packageAccess) {
      this.path = pathObj;
      this.access = packageAccess;
    }

    public ExcelSheetConfig setHeaderId(int i) {
      this.headerId = i;
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
