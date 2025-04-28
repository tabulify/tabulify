package com.tabulify.fs;

import com.tabulify.DbLoggers;
import com.tabulify.TabularAttributes;
import com.tabulify.spi.DataPath;
import com.tabulify.spi.DataPathAbs;
import com.tabulify.spi.DataPathAttribute;
import net.bytle.crypto.Digest;
import net.bytle.exception.NoParentException;
import net.bytle.exception.NoValueException;
import net.bytle.exception.NoVariableException;
import net.bytle.fs.Fs;
import net.bytle.type.MediaType;
import net.bytle.type.MediaTypes;
import net.bytle.type.Variable;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public abstract class FsDataPathAbs extends DataPathAbs implements FsDataPath {

  private Path path;

  public FsDataPathAbs(FsConnection fsConnection, Path path, MediaType mediaType) {

    super(fsConnection, path.toString(), mediaType);

    this.path = path;

    /**
     * Default values
     */
    this.getOrCreateVariable(DataPathAttribute.LOGICAL_NAME).setValueProvider(this::getLogicalNameDefault);
    this.addVariablesFromEnumAttributeClass(FsDataPathAttribute.class);
    this.getOrCreateVariable(FsDataPathAttribute.URI).setValueProvider(this::getUri);


  }

  private URI getUri() {
    return this.getNioPath().toUri();
  }

  private String getLogicalNameDefault() {

    if (this.isScript()) {
      return this.scriptDataPath.getLogicalName();
    }

    /**
     * A directory does not have any extension
     */
    if (!Files.isDirectory(path)) {
      int endIndex = getName().lastIndexOf(".");
      if (endIndex != -1) {
        return getName().substring(0, endIndex);
      } else {
        return getName();
      }
    } else {
      return getName();
    }

  }

  @Override
  public DataPath addVariable(Variable variable) {
    try {

      String key = variable.getAttribute().toString();
      Object valueOrDefaultOrNull = variable.getValueOrDefaultOrNull();
      String value;
      if (valueOrDefaultOrNull != null) {
        value = valueOrDefaultOrNull.toString();
      } else {
        value = ""; // null does not work
      }

      /**
       * Case of a script or memory path
       */
      if (this.path != null) {
        Fs.setUserAttribute(this.path, key, value);
      }

    } catch (IOException e) {

      DbLoggers.LOGGER_DB_ENGINE.warning("Problem while adding a attribute on the file (" + path + "). The file system does not support adding user attributes. Error:" + e.getMessage());

    }
    return super.addVariable(variable);
  }

  public FsDataPathAbs(FsConnection fsConnection, DataPath dataPath) {
    super(fsConnection, dataPath);
  }

  @Override
  public FsDataPath getSibling(String name) {
    Path siblingPath = path.resolveSibling(name);
    return this.getConnection().getDataSystem().getFileManager(siblingPath, null).createDataPath(getConnection(), siblingPath);
  }

  @Override
  public FsDataPath getParent() throws NoParentException {
    if (path == null) {
      // script case
      throw new NoParentException();
    }
    Path parent = path.getParent();
    if (parent == null) {
      // the parent of a relative path is null
      throw new NoParentException();
    }
    return this
      .getConnection().getDataSystem().getFileManager(parent, MediaTypes.DIR)
      .createDataPath(getConnection(), parent);
  }

  @Override
  public FsDataPath resolve(String stringPath) {
    Path resolvedPath = path.resolve(stringPath);
    return this.getConnection().getDataSystem().getFileManager(resolvedPath, null).createDataPath(getConnection(), resolvedPath);
  }

  @Override
  public FsDataPath getChildAsTabular(String name) {
    String extension;
    try {
      extension = (String) getConnection().getTabular().getVariable(TabularAttributes.DEFAULT_FILE_SYSTEM_TABULAR_TYPE).getValueOrDefault();
    } catch (NoVariableException | NoValueException e) {
      extension = "csv";
    }
    Path siblingPath = path.resolve(name + "." + extension);
    return this.getConnection().getDataSystem().getFileManager(siblingPath, MediaTypes.TEXT_CSV).createDataPath(getConnection(), siblingPath);
  }

  @Override
  public FsDataPath getChild(String name) {
    return resolve(name);
  }

  @Override
  public String getRelativePath() {

    /**
     * Script data path case
     */
    if (this.path == null) {

      return null;

    } else {

      if (!this.path.isAbsolute()) {
        return this.path.toString();
      }

      Path relativePath = this.getConnection().getNioPath().relativize(this.path);
      String relativePathString = relativePath.toString();
      relativePathString = FsConnectionResourcePath.toTabliPath(relativePathString);
      return relativePathString;

    }

  }


  /**
   *
   * @return th relative data path
   * Warning use {@link #getAbsoluteNioPath()} if you want to test if the file exists
   */
  @Override
  public Path getNioPath() {
    /**
     * Script Data Path case
     */
    if (this.path == null) {
      return null;
    }
    return this.path;

  }

  @Override
  public Path getAbsoluteNioPath() {
    if (this.path == null) {
      // case of a script
      return null;
    }
    if (!this.path.isAbsolute()) {
      Path nioPath = this.getConnection().getNioPath().toAbsolutePath();
      return nioPath.resolve(this.path);
    } else {
      return this.path;
    }
  }

  @Override
  public String getAbsolutePath() {

    Path absoluteNioPath = getAbsoluteNioPath();
    if (absoluteNioPath == null) {
      return "/";
    }
    /**
     * We don't {@link FsConnectionResourcePath#toTabliPath(String)}
     * because the user may take it and copy it
     * on its system
     * It's not really used by Tabulify
     */
    return absoluteNioPath.normalize().toString();

  }


  @Override
  public String getName() {
    if (this.isScript()) {
      return this.scriptDataPath.getName();
    } else {
      return this.path.getFileName().toString();
    }
  }


  @Override
  public List<String> getNames() {
    return IntStream.range(0, path.getNameCount())
      .mapToObj(i -> path.getName(i).toString())
      .collect(Collectors.toList());
  }

  @Override
  public FsConnection getConnection() {

    return (FsConnection) super.getConnection();

  }


  @Override
  public String getLogicalName() {

    return this.getOrCreateVariable(DataPathAttribute.LOGICAL_NAME).getValueOrDefaultAsStringNotNull();

  }

  @Override
  public DataPath getSelectStreamDependency() {
    // There is no stream dependency
    return null;
  }

  @Override
  public byte[] getByteDigest(String algorithm) throws NoSuchFileException {

    return Digest.createFromPath(Digest.Algorithm.createFrom(algorithm), path).getHashBytes();

  }


}
