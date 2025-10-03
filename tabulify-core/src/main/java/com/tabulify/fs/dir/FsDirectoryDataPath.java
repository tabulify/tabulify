package com.tabulify.fs.dir;

import com.tabulify.fs.FsConnection;
import com.tabulify.fs.FsDataPath;
import com.tabulify.fs.FsDataPathAbs;
import com.tabulify.spi.DataPath;
import com.tabulify.spi.SchemaType;
import com.tabulify.stream.InsertStream;
import com.tabulify.stream.SelectStream;
import com.tabulify.transfer.TransferPropertiesSystem;
import net.bytle.type.MediaTypes;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * A directory
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
  public InsertStream getInsertStream(DataPath source, TransferPropertiesSystem transferProperties) {
    throw new IllegalArgumentException("You can't insert into a directory");
  }

  @Override
  public SelectStream getSelectStream() {
    throw new UnsupportedOperationException("The select of directory is not yet implemented");
  }

  @Override
  public boolean hasHeaderInContent() {
    return false;
  }

  @Override
  public SchemaType getSchemaType() {
    return SchemaType.STRICT;
  }

}
