package com.tabulify.fs.binary;

import com.tabulify.fs.FsConnection;
import com.tabulify.fs.FsConnectionAttribute;
import com.tabulify.fs.FsDataPath;
import com.tabulify.fs.FsDataPathAbs;
import com.tabulify.spi.DataPath;
import com.tabulify.spi.SchemaType;
import com.tabulify.stream.InsertStream;
import com.tabulify.stream.SelectStream;
import com.tabulify.transfer.TransferPropertiesSystem;
import com.tabulify.type.MediaType;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * The base data path -
 * - A file without any structure (a binary)
 */
public class FsBinaryDataPath extends FsDataPathAbs implements FsDataPath {


  public FsBinaryDataPath(FsConnection fsConnection, Path path, MediaType mediaType) {
    super(fsConnection, path, mediaType);
  }


  @Override
  public FsBinaryFileManager getFileManager() {
    return FsBinaryFileManager.getSingleton();
  }

  @Override
  public Long getSize() {
    try {
      return Files.size(getAbsoluteNioPath());
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public Long getCount() {

    if (this.getClass().equals(FsBinaryDataPath.class)) {
      // binary does not implement any select stream
      // 1 represents the file
      return 1L;
    }

    long i = 0;
    try (SelectStream selectStream = getSelectStreamSafe()) {
      while (selectStream.next()) {
        i++;
      }
    }
    return i;
  }

  @Override
  public InsertStream getInsertStream(DataPath source, TransferPropertiesSystem transferProperties) {
    /**
     * Note that when we download a memory or relation database,
     * the file calculated should use the {@link com.tabulify.fs.FsConnectionAttribute#TABULAR_FILE_TYPE}
     * and we should not come here
     */
    MediaType tabularMediaType = (MediaType) getConnection().getAttribute(FsConnectionAttribute.TABULAR_FILE_TYPE).getValueOrDefault();
    throw new UnsupportedOperationException("No data insertion can be done in a binary file. The resource (" + this + ") has been detected as a binary file (" + mediaType + "). If this file is the target of a sql or memory resource, Tabulify should have created a file with the media type (" + tabularMediaType + ") defined in the connection parameter (" + this.getConnection() + FsConnectionAttribute.TABULAR_FILE_TYPE + ")");
  }


  @Override
  public boolean hasHeaderInContent() {
    return false;
  }

  @Override
  public SchemaType getSchemaType() {
    return SchemaType.LOOSE;
  }

}
