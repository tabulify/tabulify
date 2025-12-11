package com.tabulify.fs;

import com.tabulify.DbLoggers;
import com.tabulify.fs.binary.FsBinaryFileManager;
import com.tabulify.fs.textfile.FsTextManager;
import com.tabulify.model.Constraint;
import com.tabulify.model.ForeignKeyDef;
import com.tabulify.spi.*;
import com.tabulify.stream.SelectStream;
import com.tabulify.transfer.*;
import com.tabulify.exception.InternalException;
import com.tabulify.exception.NotAbsoluteException;
import com.tabulify.fs.Fs;
import com.tabulify.fs.FsShortFileName;
import com.tabulify.glob.Glob;
import com.tabulify.type.KeyNormalizer;
import com.tabulify.type.MediaType;
import com.tabulify.type.MediaTypes;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;


/**
 * A wrapper around a {@link java.nio.file.FileSystem}
 */
public class FsDataSystem extends DataSystemAbs {


  private final FsConnection fsConnection;

  public FsDataSystem(FsConnection fsConnection) {
    super(fsConnection);
    this.fsConnection = fsConnection;
  }

  /**
   * An utility to get resources files with a glob
   *
   * @param clazz                 the class context from where the resources is searched
   * @param rootResourceDirectory - the resource directory to search from (giving only the root, send you to the class directory, not where you think)
   * @param globPathOrNames       - the glob path or name from the rootResourceDirectory
   * @param names                 - the extra names to create a glob path if any
   * @return the list of resources
   */
  public static List<Path> getResourcesByGlob(Class<?> clazz, String rootResourceDirectory, String globPathOrNames, String... names) {

    try {
      if (!(rootResourceDirectory.startsWith("/"))) {
        rootResourceDirectory = "/" + rootResourceDirectory;
      }
      URL resource = clazz.getResource(rootResourceDirectory);
      if (resource == null) {
        throw new RuntimeException("The resource " + rootResourceDirectory + " was not found");
      }
      URI rootResource = resource.toURI();
      Path path = Paths.get(rootResource);
      return getFilesByGlob(path, globPathOrNames, names);
    } catch (URISyntaxException e) {
      throw new RuntimeException("Bad Uri", e);
    }

  }


  @Override
  public FsConnection getConnection() {
    return fsConnection;
  }

  @Override
  public Boolean exists(DataPath dataPath) {

    final FsDataPath fsDataPath = (FsDataPath) dataPath;

    Path nioPath = fsDataPath.getAbsoluteNioPath();
    /**
     * A runtime existence is checked against the exectuable
     */
    if (dataPath.isRuntime()) {
      DataPath executableDataPath = dataPath.getExecutableDataPath();
      if (!(executableDataPath instanceof FsDataPath)) {
        return true;
      }
      nioPath = ((FsDataPath) executableDataPath).getAbsoluteNioPath();
    }

    if (nioPath == null) {
      return false;
    }
    return Files.exists(nioPath);

  }


  /**
   * The service provider
   *
   * @param mediaType the media type
   * @return the file manager
   */
  public FsFileManager getFileManager(MediaType mediaType) {

    /**
     * If the media type is given, get the
     * file manager by media type
     */
    FsFileManager fileManager = getFsFileManagerWithMediaType(mediaType);
    if (fileManager != null) {
      return fileManager;
    }

    /**
     * Mime Text?
     */
    if (mediaType.isText()) {
      return FsTextManager.getSingeleton();
    }


    /**
     * Raw binary file
     */
    return FsBinaryFileManager.getSingleton();


  }

  /**
   * Resolve a path against the connection path
   *
   * @param path the path to resolve
   * @return the path if already absolute or the path resolved to the connection path
   */
  public Path toAbsolutePath(Path path) {
    if (path.isAbsolute()) {
      return path;
    }
    return this.getConnection().getNioPath().resolve(path);
  }

  private FsFileManager getFsFileManagerWithMediaType(MediaType mediaType) {
    List<FsFileManagerProvider> installedProviders = FsFileManagerProvider.installedProviders();
    for (FsFileManagerProvider structProvider : installedProviders) {
      if (structProvider.accept(mediaType)) {
        FsFileManager fileManager = structProvider.getFsFileManager();
        if (fileManager == null) {
          String message = "The returned file manager is null for the provider (" + structProvider.getClass() + ")";
          DbLoggers.LOGGER_DB_ENGINE.severe(message);
          throw new InternalException(message);
        }
        return fileManager;
      }
    }
    return null;
  }


  @Override
  public void create(DataPath dataPath, DataPath sourceDataPath, Map<DataPath, DataPath> sourceTargets) {
    FsDataPath fsDataPath = (FsDataPath) dataPath;
    if (exists(dataPath)) {
      throw new RuntimeException("The data path (" + fsDataPath.getAbsoluteNioPath() + ") already exists");
    }
    Path path = fsDataPath.getAbsoluteNioPath();
    try {
      if (Files.isDirectory(path)) {
        Files.createDirectory(path);
        return;
      }
      /**
       * Merge the definition
       */
      if (sourceDataPath != null) {
        dataPath
          .getOrCreateRelationDef()
          .mergeDataDef(sourceDataPath, sourceTargets);
      }
      /**
       * Create it
       */
      getFileManager(dataPath.getMediaType()).create(fsDataPath);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }

  }

  private FsFileManager getFileManager(Path path) {
    MediaType mediaType = this.determineMediaType(path);
    return getFileManager(mediaType);
  }


  @Override
  public void drop(List<DataPath> dataPaths, Set<DropTruncateAttribute> dropAttributes) {
    boolean cascade = dropAttributes.contains(DropTruncateAttribute.FORCE) || dropAttributes.contains(DropTruncateAttribute.CASCADE);
    for (DataPath dataPath : dataPaths) {
      FsDataPath fsDataPath = (FsDataPath) dataPath;
      if (dropAttributes.contains(DropTruncateAttribute.IF_EXISTS)) {
        if (!exists(dataPath)) {
          continue;
        }
      }
      Fs.delete(fsDataPath.getAbsoluteNioPath(), cascade);
    }
  }

  @Override
  public void truncate(List<DataPath> dataPaths, Set<DropTruncateAttribute> truncateAttributes) {

    boolean cascade = truncateAttributes.contains(DropTruncateAttribute.FORCE) || truncateAttributes.contains(DropTruncateAttribute.CASCADE);

    for (DataPath dataPath : dataPaths) {
      if (!(dataPaths instanceof FsDataPath)) {
        throw new InternalException("The data resource (" + dataPaths + ") is not a file  but a " + dataPath.getMediaType());
      }
      FsDataPath fsDataPath = (FsDataPath) dataPath;
      Fs.truncate(fsDataPath.getAbsoluteNioPath(), 0, cascade);
    }


  }


  @SuppressWarnings("unused")
  private FsFileManager getFileManager(FsDataPath fsDataPath) {
    FsFileManager fileManager = fsDataPath.getFileManager();
    if (fileManager == null) {
      fileManager = getFileManager(fsDataPath.getNioPath());
    }
    return fileManager;
  }

  @Override
  public <D extends DataPath> List<D> getChildrenDataPath(DataPath dataPath) {

    FsDataPath fsDataPath = (FsDataPath) dataPath;
    Path path = fsDataPath.getAbsoluteNioPath();
    if (!Files.isDirectory(path)) {
      throw new RuntimeException("The data path (" + dataPath + ") is not a directory and therefore has no child");
    }

    try (DirectoryStream<Path> paths = Files.newDirectoryStream(path)) {

      List<DataPath> children = new ArrayList<>();
      for (Path childPath : paths) {
        MediaType mediaType = MediaTypes.detectMediaTypeSafe(childPath.toAbsolutePath());
        children.add(dataPath.resolve(childPath.getFileName().toString(), mediaType));
      }

      //noinspection unchecked
      return (List<D>) children;

    } catch (IOException e) {
      throw new RuntimeException(e);
    }

  }


  @Override
  public Boolean isEmpty(DataPath dataPath) {
    /**
     * We don't check the file size because it may
     * have metadata
     */
    try (SelectStream selectStream = dataPath.getSelectStream()) {
      return !selectStream.next();
    } catch (SelectException e) {
      throw new RuntimeException("Error while trying to read the file resource (" + dataPath + ")");
    }
  }


  @Override
  public boolean isDocument(DataPath dataPath) {
    Path path = ((FsDataPath) dataPath).getAbsoluteNioPath();
    return !Files.isDirectory(path);
  }


  @Override
  public String getContentAsString(DataPath dataPath) {
    FsDataPath fsDataPath = (FsDataPath) dataPath;
    try {
      return Fs.getFileContent(fsDataPath.getAbsoluteNioPath());
    } catch (NoSuchFileException e) {
      throw new RuntimeException("The resource (" + dataPath + ") does not exist, we can't read its content");
    }
  }

  @Override
  public TransferListener transfer(TransferSourceTargetOrder transferOrder) {


    FsDataPath fsSource = (FsDataPath) transferOrder.getSourceDataPath();

    if (!this.isReadable(fsSource)) {
      throw new RuntimeException("The source file (" + fsSource + ") is not readable. You don't have the permission.");
    }
    if (!exists(fsSource)) {
      throw new RuntimeException("The source file (" + fsSource + ") does not exists.");
    }
    FsDataPath fsTarget = (FsDataPath) transferOrder.getTargetDataPath();
    TransferListener transferListenerStream = new TransferListenerAtomic(transferOrder)
      .startTimer();
    try {

      /*
       * Load Operation check
       */
      TransferPropertiesSystem transferProperties = transferOrder.getTransferProperties();
      TransferOperation loadOperation = transferProperties.getOperation();
      if (loadOperation == null) {
        loadOperation = TransferOperation.COPY;
      }
      FsLog.LOGGER_DB_FS.info(loadOperation + " data operation on the file (" + fsSource.getNioPath() + ") to the file(" + fsTarget.getNioPath() + ")");


      Path targetAbsoluteNioPath = fsTarget.getAbsoluteNioPath();

      // Create directory if not exits
      // It's need in all operations
      // otherwise you get a java.nio.file.NoSuchFileException
      Fs.createDirectoryIfNotExists(targetAbsoluteNioPath.getParent());

      /*
       * Target check
       */
      if (!Files.exists(targetAbsoluteNioPath)) {
        // Fs.Copy, Fs.Write and Fs.Move create the file
        if (!Set.of(TransferOperation.COPY, TransferOperation.INSERT).contains(loadOperation) && !transferProperties.isMoveOperation()) {
          if (
            transferProperties.getTargetOperations().contains(TransferResourceOperations.CREATE)
          ) {
            if (fsTarget.getOrCreateRelationDef().getColumnDefs().isEmpty() && !fsSource.getOrCreateRelationDef().getColumnDefs().isEmpty()) {
              fsTarget.getOrCreateRelationDef()
                .mergeColumns(fsSource.getRelationDef());
            }
            Tabulars.create(fsTarget);
          } else {
            throw new RuntimeException("The target data resource file (" + targetAbsoluteNioPath + ") does not exist and the target data operation (" + TransferResourceOperations.CREATE + ") was not present");
          }
        }
      } else {
        if (
          transferProperties.getTargetOperations().contains(TransferResourceOperations.DROP)
        ) {
          Files.delete(targetAbsoluteNioPath);
        }
      }

      /*
       * Copy Options
       */
      List<StandardCopyOption> copyOptions = new ArrayList<>();
      if (transferProperties.getTargetOperations().contains(TransferResourceOperations.DROP)) {
        copyOptions.add(StandardCopyOption.REPLACE_EXISTING);
      }

      // Atomic
      ArrayList<StandardCopyOption> atomicOption = new ArrayList<>(copyOptions);
      atomicOption.add(StandardCopyOption.ATOMIC_MOVE);


      switch (loadOperation) {
        case COPY:
          /**
           * ie copy and source delete (surely implemented as rename)
           */
          if (transferProperties.isMoveOperation()) {

            try {
              /*
               * We try atomic
               */
              Files.move(fsSource.getAbsoluteNioPath(), targetAbsoluteNioPath, atomicOption.toArray(new StandardCopyOption[0]));
            } catch (AtomicMoveNotSupportedException | UnsupportedOperationException e) {
              /*
               * Non-atomic then
               */
              Files.move(fsSource.getAbsoluteNioPath(), targetAbsoluteNioPath, copyOptions.toArray(new StandardCopyOption[0]));
            }
          } else {
            try {
              /*
               * We try atomic
               */
              Files.copy(fsSource.getAbsoluteNioPath(), targetAbsoluteNioPath, atomicOption.toArray(new StandardCopyOption[0]));
            } catch (AtomicMoveNotSupportedException | UnsupportedOperationException e) {
              /*
               * Non-atomic then
               */
              Files.copy(fsSource.getAbsoluteNioPath(), targetAbsoluteNioPath, copyOptions.toArray(new StandardCopyOption[0]));
            }
          }
          break;
        case INSERT:
          /**
           * Existing file may have headers in the content at creation
           * Example: csv
           * This case is taken into account in the {@link TransferManagerOrder}
           */
          List<StandardOpenOption> options = new ArrayList<>();
          options.add(StandardOpenOption.APPEND);
          if (transferProperties.getTargetOperations().contains(TransferResourceOperations.CREATE) && !Files.exists(targetAbsoluteNioPath)) {
            options.add(StandardOpenOption.CREATE_NEW);
          }
          Files.write(
            targetAbsoluteNioPath,
            Files.readAllBytes(fsSource.getAbsoluteNioPath()),
            options.toArray(new StandardOpenOption[0]));
          break;
      }

    } catch (IOException e) {
      transferListenerStream.addException(e);
      throw new RuntimeException(e);
    }
    return transferListenerStream.stopTimer();
  }

  private boolean isReadable(DataPath dataPath) {
    final FsDataPath fsDataPath = (FsDataPath) dataPath;
    Path nioPath = fsDataPath.getAbsoluteNioPath();
    if (nioPath == null) {
      return false;
    } else {
      return Files.isReadable(nioPath);
    }
  }


  @Override
  public <D extends DataPath> List<D> getDescendants(DataPath dataPath) {
    throw new UnsupportedOperationException("Not yet implemented");
  }

  @SuppressWarnings("unchecked")
  @Override
  public List<DataPath> select(DataPath baseDataPath, String patternOrPath, MediaType mediaType) {

    FsDataPath baseFsDataPath = (FsDataPath) baseDataPath;
    Path basePath = baseFsDataPath.getAbsoluteNioPath();

    List<Path> paths = getFilesByGlob(basePath, patternOrPath);

    return paths.stream()
      .map(p -> getConnection().getFsDataPath(p, mediaType))
      .collect(Collectors.toList());
  }

  @Override
  public List<ForeignKeyDef> getForeignKeysThatReference(DataPath dataPath) {
    FsLog.LOGGER_DB_FS.fine("The foreign keys that reference are not yet implemented for a file system");
    return new ArrayList<>();
  }

  @Override
  public void dropConstraint(Constraint constraint) {
    throw new RuntimeException("A file system does not have the notion of constraint yet");
  }

  /**
   * @param currentPath    - the current path
   * @param globPathOrName - a glob
   * @return a list of path that matches the glob
   * <p>
   * ex: the following glob
   * /tmp/*.md
   * will return all md file in the tmp directory
   * <p>
   * See also {@link PathMatcher}
   */
  static public List<Path> getFilesByGlob(Path currentPath, String globPathOrName, String... names) {

    /**
     * Glob Path File System Based
     */
    FsConnectionResourcePath stringPath = FsConnectionResourcePath.createOf(currentPath, globPathOrName, names);

    /**
     * What is the start path ?
     * If the glob is an absolute path, we start from the root
     * otherwise it's relative to the current path given as argument
     */
    Path startPath;


    // if not root in glob
    List<String> stringNames = new ArrayList<>();
    if (stringPath.isAbsolute()) {
      startPath = stringPath.getPathRoot();
      stringNames = stringPath.getNames();
    } else {
      /**
       * normalize because the parent of path1/path2/. is path1/path2, not path1
       * toAbsolutePath because the parent of `.` is `null`
       */
      startPath = currentPath.toAbsolutePath().normalize();
      for (String name : stringPath.getNames()) {
        if (name.equals(stringPath.getParentPathName())) {
          Path parent = startPath.getParent();
          if(parent == null) {
            throw new RuntimeException("You can't ask the parent of the root path ("+startPath+")");
          }
          startPath = parent;
        } else {
          if (!name.equals(stringPath.getCurrentPathName())) {
            stringNames.add(name);
          }
        }
      }
    }

    /**
     * Recursive processing
     * We go through all glob names and retain only
     * the matched paths in the currentMatchesPaths variable
     */
    List<Path> currentMatchesPaths = new ArrayList<>();
    currentMatchesPaths.add(startPath);
    boolean recursiveWildCardFound = false;
    for (String name : stringNames) {

      if (name.equals(Glob.DOUBLE_STAR)) {
        recursiveWildCardFound = true;
        continue;
      }

      FsShortFileName sfn = FsShortFileName.of(name);
      Pattern pattern;
      if (!sfn.isShortFileName()) {
        pattern = Pattern.compile(Glob.createOf(name).toRegexPattern());
      } else {
        pattern = Pattern.compile(Glob.createOf(sfn.getShortName() + "*").toRegexPattern(), Pattern.CASE_INSENSITIVE);
      }

      /**
       * Is it a recursive traversal, now ?
       */
      if (!recursiveWildCardFound) {
        /**
         * This is not, we just look up this level
         */
        List<Path> matchesPath = new ArrayList<>();
        for (Path currentRecursivePath : currentMatchesPaths) {
          List<Path> recursiveMatchesPath = getMatchesPath(pattern, currentRecursivePath, false);
          matchesPath.addAll(recursiveMatchesPath);
        }
        // Recursion
        currentMatchesPaths = matchesPath;
        // Break if there is no match
        if (matchesPath.isEmpty()) {
          break;
        }
      } else {
        List<Path> matchedFiles = new ArrayList<>();
        while (!currentMatchesPaths.isEmpty()) {
          List<Path> matchesPath = new ArrayList<>();
          for (Path currentRecursivePath : currentMatchesPaths) {
            List<Path> currentMatchesPath = getMatchesPath(pattern, currentRecursivePath, true);
            matchesPath.addAll(currentMatchesPath);
          }
          /**
           * Recursion only on directories
           */
          currentMatchesPaths = matchesPath
            .stream()
            .filter(Files::isDirectory)
            .collect(Collectors.toList());
          /**
           * Collect the files
           */
          matchedFiles.addAll(
            matchesPath
              .stream()
              .filter(Files::isRegularFile)
              .collect(Collectors.toList())
          );
        }
        /**
         * Update the returned variables
         */
        currentMatchesPaths = matchedFiles;
        break;
      }
    }

    return currentMatchesPaths;

  }

  private static List<Path> getMatchesPath(Pattern pattern, Path currentRecursivePath, Boolean recursiveWildCardFound) {

    // The list where the actual matches path will be stored
    List<Path> matchesPath = new ArrayList<>();
    // There is also newDirectoryStream
    // https://docs.oracle.com/javase/8/docs/api/java/nio/file/Files.html#newDirectoryStream-java.nio.file.Path-java.lang.String-
    // but yeah ...
    if (Files.isDirectory(currentRecursivePath)) {
      try {
        List<Path> paths = Fs.getChildrenFiles(currentRecursivePath);
        for (Path childrenPath : paths) {
          if (pattern.matcher(childrenPath.getFileName().toString()).find()) {
            matchesPath.add(childrenPath);
          } else {
            if (recursiveWildCardFound) {
              if (Files.isDirectory(childrenPath)) {
                matchesPath.add(childrenPath);
              }
            }
          }
        }
      } catch (Exception e) {
        if (e.getCause().getClass().equals(AccessDeniedException.class)) {
          FsLog.LOGGER_DB_FS.warning("The path (" + currentRecursivePath + ") was denied");
        } else {
          throw e;
        }
      }
    } else {
      if (pattern.matcher(currentRecursivePath.getFileName().toString()).find()) {
        matchesPath.add(currentRecursivePath);
      }
    }
    return matchesPath;
  }


  /**
   * An utility to get the files by entering the glob path or glob names
   * Relative from the current path
   *
   * @param globPathOrName a glob or path name
   * @param names          the extra names
   * @return the files that match
   */
  public static List<Path> getFilesByGlob(String globPathOrName, String... names) {
    Path path = Paths.get(".");
    return getFilesByGlob(path, globPathOrName, names);
  }


  @Override
  public DataPath getTargetFromSource(DataPath sourceDataPath, MediaType mediaType, DataPath targetParentDataPath) {

    // For convenience
    FsConnection connection = this.getConnection();

    if (targetParentDataPath != null && !(targetParentDataPath instanceof FsDataPath)) {
      throw new InternalException("The targetParentDataPath must be a FsDataPath");
    }
    FsDataPath fsTargetParentDataPath = (FsDataPath) targetParentDataPath;
    if (fsTargetParentDataPath == null) {
      fsTargetParentDataPath = connection.getCurrentDataPath();
    }

    // Tabular (ie Sql data)
    if (
      !(sourceDataPath.getConnection() instanceof FsConnection)
        && (sourceDataPath.getOrCreateRelationDef().getColumnsSize() > 0 || sourceDataPath.isRuntime())
    ) {
      if (mediaType == null) {
        mediaType = (MediaType) getConnection().getAttribute(FsConnectionAttribute.TABULAR_FILE_TYPE).getValueOrDefault();
      }
      FsDataPath dataPath = (FsDataPath) fsTargetParentDataPath.resolve(sourceDataPath.getLogicalName() + "." + mediaType.getExtension(), mediaType);
      // hack
      if (MediaTypes.equals(mediaType, MediaTypes.TEXT_CSV)) {
        dataPath.addAttribute(KeyNormalizer.createSafe("header-row-id"), 1);
      }
      return dataPath;

    }

    // Script ?
    if (sourceDataPath.isRuntime()) {
      // a query is anonymous and does not have any name
      return fsTargetParentDataPath.resolve(sourceDataPath.getLogicalName(), mediaType);
    }

    /**
     * FsConnection takes the name and not the logical name as name
     * (ie when we move the file `foo.txt`, to a file system, the name
     * will be `foo.txt`
     */
    return fsTargetParentDataPath.resolve(sourceDataPath.getName(), mediaType);

  }

  @Override
  public MediaType getContainerMediaType() {
    return MediaTypes.DIR;
  }

  /**
   * Media Type determination
   */
  public MediaType determineMediaType(Path path) {
    Path absolutePath = path;
    if (!path.isAbsolute()) {
      absolutePath = this.toAbsolutePath(path);
    }
    try {
      return Fs.detectMediaType(absolutePath);
    } catch (NotAbsoluteException e) {
      throw new InternalException("It should not happen as the path passed is absolute. Path: " + absolutePath, e);
    }
  }
}
