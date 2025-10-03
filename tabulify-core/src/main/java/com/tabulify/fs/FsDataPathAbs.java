package com.tabulify.fs;

import com.tabulify.DbLoggers;
import com.tabulify.conf.Attribute;
import com.tabulify.fs.runtime.FsCommandMediaType;
import com.tabulify.spi.DataPath;
import com.tabulify.spi.DataPathAbs;
import com.tabulify.spi.DataPathAttribute;
import net.bytle.crypto.Digest;
import net.bytle.exception.InternalException;
import net.bytle.exception.NoParentException;
import net.bytle.fs.Fs;
import net.bytle.type.MediaType;
import net.bytle.type.MediaTypes;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.sql.Timestamp;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public abstract class FsDataPathAbs extends DataPathAbs implements FsDataPath {

  private BasicFileAttributes pathAttrs;
  /**
   * A relative path use {@link #getAbsoluteNioPath()} to get the absolute path
   */
  private Path relativePath;

  public FsDataPathAbs(FsConnection fsConnection, Path relativepath, MediaType mediaType) {

    super(fsConnection,
      isRelative(relativepath).toString(),
      null, mediaType
    );

    this.relativePath = relativepath;

    /**
     * Default values
     */
    this.getOrCreateVariable(DataPathAttribute.LOGICAL_NAME).setValueProvider(this::getLogicalNameDefault);
    this.addVariablesFromEnumAttributeClass(FsDataPathAttribute.class);
    this.getOrCreateVariable(FsDataPathAttribute.URI).setValueProvider(() -> this.getAbsoluteNioPath().toUri());

  }

  private static Path isRelative(Path relativepath) {
    if (relativepath.isAbsolute()) {
      throw new InternalException("The path (" + relativepath + ") is not relative");
    }
    return relativepath;
  }


  private String getLogicalNameDefault() {

    if (this.executableDataPath != null) {
      return this.executableDataPath.getLogicalName();
    }
    return Fs.getFileNameWithoutExtension(relativePath);


  }

  @Override
  public DataPath addAttribute(Attribute attribute) {
    try {

      /**
       * If the value is not derived and this is not a runtime
       */
      if (!attribute.isValueProviderValue() && this.relativePath != null) {

        String key = attribute.getAttributeMetadata().toString();
        Object valueOrDefaultOrNull = attribute.getValueOrDefault();
        String value;
        if (valueOrDefaultOrNull != null) {
          value = valueOrDefaultOrNull.toString();
        } else {
          value = ""; // null does not work
        }
        Fs.setUserAttribute(this.relativePath, key, value);

      }

    } catch (IOException e) {

      DbLoggers.LOGGER_DB_ENGINE.warning("Problem while adding a attribute on the file (" + relativePath + "). The file system does not support adding user attributes. Error:" + e.getMessage());

    }
    return super.addAttribute(attribute);
  }

  public FsDataPathAbs(FsConnection fsConnection, DataPath executableDataPath) {
    super(fsConnection, null, executableDataPath, FsCommandMediaType.COMMAND_MEDIA_TYPE);
  }

  @Override
  public FsDataPath getSibling(String name) {
    Path relativeSiblingPath = relativePath.resolveSibling(name);
    MediaType mediaType = this.getConnection().getDataSystem().determineMediaType(relativeSiblingPath);
    return (FsDataPath) this.getConnection().getDataSystem().getFileManager(mediaType).createDataPath(getConnection(), relativeSiblingPath, mediaType);
  }

  @Override
  public FsDataPath getParent() throws NoParentException {
    if (relativePath == null) {
      // script case
      throw new NoParentException();
    }
    Path parent = relativePath.getParent();
    if (parent == null) {
      // the parent of a relative path is null
      throw new NoParentException();
    }
    return (FsDataPath) this
      .getConnection().getDataSystem().getFileManager(MediaTypes.DIR)
      .createDataPath(getConnection(), parent, MediaTypes.DIR);
  }

  @Override
  public FsDataPath resolve(String name) {
    Path resolvedRelativePath = relativePath.resolve(name);
    MediaType mediaType = this.getConnection().getDataSystem().determineMediaType(resolvedRelativePath);
    return this.resolve(name, mediaType);
  }


  @Override
  public FsDataPath resolve(String name, MediaType mediaType) {
    Path resolvedRelativePath = relativePath.resolve(name);
    if (mediaType == null) {
      mediaType = this.getConnection().getDataSystem().determineMediaType(resolvedRelativePath);
    }
    return (FsDataPath) this.getConnection().getDataSystem().getFileManager(mediaType).createDataPath(getConnection(), resolvedRelativePath, mediaType);
  }

  @Override
  public String getCompactPath() {

    /**
     * Script data path case
     */
    if (this.relativePath == null) {

      return null;

    }

    if (!this.relativePath.isAbsolute()) {
      return this.relativePath.toString();
    }

    Path relativePath = this.getConnection().getNioPath().relativize(this.relativePath);
    String relativePathString = relativePath.toString();
    relativePathString = FsConnectionResourcePath.toTabliPath(relativePathString);
    return relativePathString;

  }


  /**
   * @return th relative data path
   * Warning use {@link #getAbsoluteNioPath()} if you want to test if the file exists
   */
  @Override
  public Path getNioPath() {
    /**
     * Script Data Path case
     */
    if (this.relativePath == null) {
      return null;
    }
    return this.relativePath;

  }

  @Override
  public Path getAbsoluteNioPath() {

    // case of a static path
    if (this.relativePath != null) {
      return this.getConnection().getDataSystem().toAbsolutePath(this.relativePath);
    }

    // the generated resource is not a file
    // we check the executable
    DataPath executableDataPath = getExecutableDataPath();
    if (executableDataPath instanceof FsDataPath) {
      return ((FsDataPath) executableDataPath).getAbsoluteNioPath();
    }

    // case of a runtime path
    DataPath dataPath = execute();
    if (dataPath instanceof FsDataPath) {
      return ((FsDataPath) dataPath).getAbsoluteNioPath();
    }

    // the only path that we know
    return this.getConnection().getNioPath();

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
    if (this.executableDataPath != null) {
      return this.executableDataPath.getName();
    }
    return this.relativePath.getFileName().toString();
  }


  @Override
  public List<String> getNames() {
    return IntStream.range(0, relativePath.getNameCount())
      .mapToObj(i -> relativePath.getName(i).toString())
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

    return Digest.createFromPath(Digest.Algorithm.createFrom(algorithm), getAbsoluteNioPath()).getHashBytes();

  }

  @Override
  public Long getSize() {
    BasicFileAttributes fileAttributes = getFileAttributes();
    return fileAttributes.size();
  }

  @Override
  public Timestamp getCreationTime() {
    BasicFileAttributes fileAttributes = getFileAttributes();
    FileTime fileTime = fileAttributes.creationTime();
    if (fileTime == null) {
      return null;
    }
    return Timestamp.from(fileTime.toInstant());
  }

  @Override
  public Timestamp getUpdateTime() {
    BasicFileAttributes fileAttributes = getFileAttributes();
    FileTime fileTime = fileAttributes.lastModifiedTime();
    if (fileTime == null) {
      return null;
    }
    return Timestamp.from(fileTime.toInstant());
  }

  @Override
  public Timestamp getAccessTime() {
    BasicFileAttributes fileAttributes = getFileAttributes();
    FileTime fileTime = fileAttributes.lastAccessTime();
    if (fileTime == null) {
      return null;
    }
    return Timestamp.from(fileTime.toInstant());
  }


  /**
   * We read the meta lazily
   */
  private BasicFileAttributes getFileAttributes() {
    if (this.pathAttrs != null) {
      return this.pathAttrs;
    }
    Path absoluteNioPath = getAbsoluteNioPath();
    try {
      this.pathAttrs = Files.readAttributes(absoluteNioPath, BasicFileAttributes.class);
    } catch (IOException e) {
      throw new RuntimeException("Error while reading the file attribute of the path " + absoluteNioPath + ". Error: " + e.getMessage(), e);
    }
    return this.pathAttrs;
  }

}
