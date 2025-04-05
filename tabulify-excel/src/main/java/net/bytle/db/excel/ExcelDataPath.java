package net.bytle.db.excel;

import net.bytle.db.fs.FsConnection;
import net.bytle.db.fs.binary.FsBinaryDataPath;
import net.bytle.type.MediaTypes;

import java.nio.file.Path;

public class ExcelDataPath extends FsBinaryDataPath {

  public ExcelDataPath(FsConnection fsConnection, Path path) {
    super(fsConnection, path, MediaTypes.EXCEL_FILE);
  }

  @Override
  public ExcelDataDef getOrCreateRelationDef() {
    return new ExcelDataDef(this);
  }

}
