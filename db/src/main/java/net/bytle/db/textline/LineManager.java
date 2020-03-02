package net.bytle.db.textline;

import net.bytle.db.fs.*;
import net.bytle.db.stream.InsertStream;
import net.bytle.db.stream.SelectStream;

import java.nio.file.Path;

public class LineManager extends FsBinaryFileManager implements FsFileManager {


  private static LineManager lineManager;

  public static FsBinaryFileManager getSingeleton() {
    if (lineManager == null){
      lineManager = new LineManager();
    }
    return lineManager;
  }

  @Override
  public LineDataPath createDataPath(FsDataStore fsDataStore, Path path) {

    return new LineDataPath(fsDataStore, path);

  }

  @Override
  public long getSize(FsDataPath fsDataPath) {
    long i = 0;
    try (SelectStream selectStream = getSelectStream(fsDataPath)) {
      while (selectStream.next()) {
        i++;
      }
    }
    return i;
  }


  @Override
  public SelectStream getSelectStream(FsDataPath fsDataPath) {
    return LineSelectStream.of((LineDataPath) fsDataPath);
  }

  @Override
  public InsertStream getInsertStream(FsDataPath fsDataPath) {

    return LineInsertStream.of((LineDataPath) fsDataPath);

  }

}
