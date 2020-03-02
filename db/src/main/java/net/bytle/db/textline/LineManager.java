package net.bytle.db.textline;

import net.bytle.db.fs.FsDataPath;
import net.bytle.db.fs.FsDataStore;
import net.bytle.db.fs.FsBinaryFileManager;
import net.bytle.db.stream.InsertStream;
import net.bytle.db.stream.SelectStream;

import java.nio.file.Path;

public class LineManager extends FsBinaryFileManager {


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
  public SelectStream getSelectStream(FsDataPath fsDataPath) {
    return LineSelectStream.of((LineDataPath) fsDataPath);
  }

  @Override
  public InsertStream getInsertStream(FsDataPath fsDataPath) {

    return LineInsertStream.of((LineDataPath) fsDataPath);

  }

}
