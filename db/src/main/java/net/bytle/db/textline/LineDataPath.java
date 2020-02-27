package net.bytle.db.textline;

import net.bytle.db.fs.FsDataPath;
import net.bytle.db.fs.FsDataStore;
import net.bytle.db.fs.FsFileManager;
import net.bytle.db.fs.FsRawDataPath;

import java.nio.file.Path;

/**
 * A data path with a line tabular structure (one cell by row = one line)
 */
public class LineDataPath extends FsRawDataPath implements FsDataPath {

  public LineDataPath(FsDataStore fsDataStore, Path path) {
    super(fsDataStore, path);
  }


  public static LineDataPath of(FsDataStore fsDataStore, Path path) {

    return new LineDataPath(fsDataStore, path);

  }

  public static LineDataPath of(Path path) {

    return new LineDataPath(FsDataStore.of(path), path);

  }

  @Override
  public LineDataDef getDataDef() {
    if (this.csvDataDef == null) {
      this.csvDataDef = new LineDataDef(this);
    }
    return (LineDataDef) this.csvDataDef;
  }

  @Override
  public LineDataPath setDescription(String description) {
    super.setDescription(description);
    return this;
  }

  @Override
  public FsFileManager getFileManager() {
    return LineManager.getSingeleton();
  }
}
