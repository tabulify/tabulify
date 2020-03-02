package net.bytle.db.textline;

import net.bytle.db.fs.FsBinaryFileManager;
import net.bytle.db.fs.FsDataPath;
import net.bytle.db.fs.FsDataPathAbs;
import net.bytle.db.fs.FsDataStore;

import java.nio.file.Path;

/**
 * A data path with a line tabular structure (one cell by row = one line)
 */
public class LineDataPath extends FsDataPathAbs implements FsDataPath {

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
  public LineDataDef getOrCreateDataDef() {
    return createDataDef();
  }

  @Override
  public LineDataPath setDescription(String description) {
    super.setDescription(description);
    return this;
  }

  @Override
  public String getType() {
    return "line";
  }

  @Override
  public LineDataDef createDataDef() {
    if (this.relationDef == null) {
      this.relationDef = new LineDataDef(this);
    }
    return (LineDataDef) this.relationDef;
  }

  @Override
  public FsBinaryFileManager getFileManager() {
    return LineManager.getSingeleton();
  }
}
