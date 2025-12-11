package com.tabulify.noop;

import com.tabulify.Tabular;
import com.tabulify.conf.Attribute;
import com.tabulify.connection.Connection;
import com.tabulify.fs.FsConnectionResourcePath;
import com.tabulify.spi.DataPath;
import com.tabulify.spi.DataSystem;
import com.tabulify.spi.ProcessingEngine;
import com.tabulify.spi.ResourcePath;
import com.tabulify.exception.CastException;
import com.tabulify.type.Casts;
import com.tabulify.type.MediaType;

public class NoOpConnection extends Connection {

  public NoOpConnection(Tabular tabular, Attribute name, Attribute uri) {

    super(tabular, name, uri);

  }

  @Override
  public DataSystem getDataSystem() {
    return new NoOpDataSystem(this);
  }

  @Override
  public DataPath getDataPath(String pathOrName, MediaType mediaType) {
    throw new UnsupportedOperationException("No provider was found for connection (" + getName() + ") and the the url (" + getUriAsVariable() + ")");
  }

  @Override
  public DataPath getDataPath(String pathOrName) {
    return getDataPath(pathOrName, (MediaType) null);
  }

  @Override
  public String getCurrentPathCharacters() {
    throw new UnsupportedOperationException("No provider was found for connection (" + getName() + ") and the the url (" + getUriAsVariable() + ")");
  }

  @Override
  public String getParentPathCharacters() {
    throw new UnsupportedOperationException("No provider was found for connection (" + getName() + ") and the the url (" + getUriAsVariable() + ")");
  }

  @Override
  public String getSeparator() {
    throw new UnsupportedOperationException("No provider was found for connection (" + getName() + ") and the the url (" + getUriAsVariable() + ")");
  }


  @Override
  public DataPath getCurrentDataPath() {
    return null;
  }


  @Override
  public DataPath getRuntimeDataPath(DataPath dataPath, MediaType mediaType) {
    throw new UnsupportedOperationException("No provider was found for connection (" + getName() + ") and the the url (" + getUriAsVariable() + ")");
  }


  @Override
  public ProcessingEngine getProcessingEngine() {
    throw new UnsupportedOperationException("No provider was found for connection (" + getName() + ") and the the url (" + getUriAsVariable() + ")");
  }


  @Override
  public <T> T getObject(Object valueObject, Class<T> clazz) {
    try {
      return Casts.cast(valueObject, clazz);
    } catch (CastException e) {
      throw new RuntimeException(e.getMessage() + ". We were unable to cast the object value (" + valueObject + ") to the class (" + clazz + ") for the connection (" + this + ")", e);
    }
  }

  @Override
  public ResourcePath createStringPath(String pathOrName, String... names) {

    /**
     * We use string path to set the attribute of data resource
     * with the group captured by a {@link com.tabulify.glob.Glob}
     * If it's not implemented, we could throw an error, but
     * we prefer to set the string path to the fs path
     * because it should work most of the time
     */
    NoOpLog.LOGGER.warning("Glob matching may not work fully (because StringPath is not implemented by the connection (" + getName() + ") and the the url (" + getUriAsVariable() + ").)");
    return FsConnectionResourcePath.createOf(pathOrName, names);


  }

  @Override
  public Boolean ping() {
    return true;
  }


}
