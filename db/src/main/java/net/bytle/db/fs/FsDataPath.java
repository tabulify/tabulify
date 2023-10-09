package net.bytle.db.fs;

import net.bytle.db.spi.DataPath;
import net.bytle.type.MediaType;

import java.nio.file.Path;

/**
 * A wrapper around a {@link Path} that adds the data def
 */
public interface FsDataPath extends DataPath {


  /**
   *
   * @return the absolute path
   * the path used mostly in file operations
   */
  Path getAbsoluteNioPath();


  /**
   *
   * @return the relative path from the fs datastore path (when possible)
   * This path is mostly used in report
   *
   * This is not possible to get all path relative to its datastore path.
   * This is the case when a path is a local temporary one. For instance:
   * C:/Users/user1/AppData/Local/Temp/README8185053018394765872.md
   *
   *
   */
  Path getNioPath();

  FsFileManager getFileManager();

  @Override
  FsConnection getConnection();

}
