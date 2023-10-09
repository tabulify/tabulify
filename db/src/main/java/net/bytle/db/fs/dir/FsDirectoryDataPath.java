package net.bytle.db.fs.dir;

import net.bytle.db.fs.FsConnection;
import net.bytle.db.fs.FsDataPath;
import net.bytle.db.fs.FsDataPathAbs;
import net.bytle.db.spi.DataPath;
import net.bytle.db.stream.InsertStream;
import net.bytle.db.stream.SelectStream;
import net.bytle.db.transfer.TransferProperties;
import net.bytle.type.MediaTypes;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * A basic directory manager
 */
public class FsDirectoryDataPath extends FsDataPathAbs implements FsDataPath {


  public FsDirectoryDataPath(FsConnection fsConnection, Path path) {
    super(fsConnection, path, MediaTypes.DIR);
  }



  @Override
  public FsDirectoryManager getFileManager() {
    return FsDirectoryManager.getSingleton();
  }


  @Override
  public DataPath getSelectStreamDependency() {

    return null;

  }

  @Override
  public Long getSize() {

    return null;

  }

  @Override
  public Long getCount() {
    Path nioPath = this.getAbsoluteNioPath();
    Long counter = null;
    if (Files.exists(nioPath)) {
      counter = 0L;
      try {
        for (Path ignored : Files.newDirectoryStream(nioPath)) {
          counter++;
        }
      } catch (UnsupportedOperationException e) {
        return -1L;
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }
    return counter;
  }

  @Override
  public InsertStream getInsertStream(DataPath source, TransferProperties transferProperties) {
    throw new IllegalArgumentException("You can't insert into a directory");
  }

  @Override
  public SelectStream getSelectStream() {
    throw new UnsupportedOperationException("The select of directory is not yet implemented");
  }
}
