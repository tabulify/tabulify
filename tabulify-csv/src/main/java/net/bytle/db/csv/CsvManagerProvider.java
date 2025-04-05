package net.bytle.db.csv;

import net.bytle.db.fs.binary.FsBinaryFileManager;
import net.bytle.db.fs.FsFileManagerProvider;
import net.bytle.type.MediaType;
import net.bytle.type.MediaTypes;

public class CsvManagerProvider extends FsFileManagerProvider {

  private CsvManagerFs csvManager;

  @Override
  public Boolean accept(MediaType mimeType) {

    // you may also have application/csv, text/csv ...
    return mimeType.getSubType().equals(MediaTypes.TEXT_CSV.getSubType());
  }

  @Override
  public FsBinaryFileManager getFsFileManager() {
    if (csvManager == null){
      csvManager = new CsvManagerFs();
    }
    return csvManager;
  }
}
