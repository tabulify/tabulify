package com.tabulify.fs.binary;

import com.tabulify.fs.FsConnection;
import com.tabulify.fs.FsDataPath;
import com.tabulify.fs.FsDataPathAbs;
import com.tabulify.fs.textfile.FsTextDataPath;
import com.tabulify.fs.textfile.FsTextLogger;
import com.tabulify.spi.DataPath;
import com.tabulify.stream.InsertStream;
import com.tabulify.stream.SelectStream;
import com.tabulify.transfer.TransferProperties;
import net.bytle.type.MediaType;

import java.io.IOException;
import java.nio.charset.MalformedInputException;
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

  public FsBinaryDataPath(FsConnection fsConnection, DataPath dataPath) {
    super(fsConnection, dataPath);
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
    try (SelectStream selectStream = getSelectStream()) {
      while (selectStream.next()) {
        i++;
      }
    }
    return i;
  }

  @Override
  public InsertStream getInsertStream(DataPath source, TransferProperties transferProperties) {
    throw new UnsupportedOperationException("The insertion in " + this.getClass().getSimpleName() + " file is not yet supported. File (" + getAbsoluteNioPath() + ")");
  }

  @Override
  public SelectStream getSelectStream() {
    throw new UnsupportedOperationException("The loop of this " + this.getClass().getSimpleName() + " file is not implemented. File (" + getAbsoluteNioPath() + ")");
  }

}
