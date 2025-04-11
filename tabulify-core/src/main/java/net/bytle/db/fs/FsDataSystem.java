package net.bytle.db.fs;

import net.bytle.db.DbLoggers;
import net.bytle.db.fs.binary.FsBinaryFileManager;
import net.bytle.db.fs.textfile.FsTextManager;
import net.bytle.db.model.Constraint;
import net.bytle.db.model.ForeignKeyDef;
import net.bytle.db.spi.DataPath;
import net.bytle.db.spi.DataSystemAbs;
import net.bytle.db.spi.SelectException;
import net.bytle.db.spi.Tabulars;
import net.bytle.db.stream.SelectStream;
import net.bytle.db.transfer.*;
import net.bytle.exception.InternalException;
import net.bytle.exception.NotAbsoluteException;
import net.bytle.fs.Fs;
import net.bytle.fs.FsShortFileName;
import net.bytle.regexp.Glob;
import net.bytle.type.MediaType;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * A wrapper around a {@link java.nio.file.FileSystem}
 */
public class FsDataSystem extends DataSystemAbs {


  private final FsConnection dataStore;

  public FsDataSystem(FsConnection fsConnection) {
    super(fsConnection);
    dataStore = fsConnection;
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
    return dataStore;
  }

  @Override
  public Boolean exists(DataPath dataPath) {

    final FsDataPath fsDataPath = (FsDataPath) dataPath;
    Path nioPath = fsDataPath.getAbsoluteNioPath();
    if (nioPath == null) {
      return false;
    } else {
      return Files.exists(nioPath);
    }

  }


  /**
   * The service provider
   *
   * @param path      the path
   * @param mediaType the media type
   * @return the file manager
   */
  public FsFileManager getFileManager(Path path, MediaType mediaType) {

    /**
     * If the media type is given, get the
     * file manager by media type
     */
    if (mediaType != null) {
      FsFileManager fileManager = getFsFileManagerWithMediaType(mediaType);
      if (fileManager != null) {
        return fileManager;
      }
    }

    /**
     * Media Type determination
     */
    Path absolutePath = this.toAbsolutePath(path);
    try {
      mediaType = Fs.detectMediaType(absolutePath);
    } catch (NotAbsoluteException e) {
      throw new InternalException("It should not happen as the path passed is absolute. Path: "+absolutePath,e);
    }

    FsFileManager fileManager = getFsFileManagerWithMediaType(mediaType);
    if (fileManager != null) {
      return fileManager;
    }

    /**
     * Try to get a file manager by path (file name)
     * In case of complex subtype such as `--datagen.yml`
     */
    fileManager = getFsFileManagerWithPath(path);
    if (fileManager != null) {
      return fileManager;
    }

    /**
     * Try to return a text (Charset to verify that this is a text file)
     */
    String charset = Fs.detectCharacterSet(path);
    if (charset != null) {
      return FsTextManager.getSingeleton();
    }

    /**
     * Raw binary file
     */
    return FsBinaryFileManager.getSingleton();


  }

  /**
   * Resolve a path against the connection path
   * @param path the path to resolve
   * @return the path if already absolute or the path resolved to the connection path
   */
  public Path toAbsolutePath(Path path) {
    if(path.isAbsolute()){
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
          String message = "The returned file manager is null for the provider (" + structProvider.getClass().toString() + ")";
          DbLoggers.LOGGER_DB_ENGINE.severe(message);
          throw new RuntimeException(message);
        }
        return fileManager;
      }
    }
    return null;
  }

  private FsFileManager getFsFileManagerWithPath(Path path) {
    List<FsFileManagerProvider> installedProviders = FsFileManagerProvider.installedProviders();
    for (FsFileManagerProvider structProvider : installedProviders) {
      if (structProvider.accept(path)) {
        FsFileManager fileManager = structProvider.getFsFileManager();
        if (fileManager == null) {
          String message = "The returned file manager is null for the provider (" + structProvider.getClass().toString() + ")";
          DbLoggers.LOGGER_DB_ENGINE.severe(message);
          throw new RuntimeException(message);
        }
        return fileManager;
      }
    }
    return null;
  }


  @Override
  public boolean isContainer(DataPath dataPath) {
    return Files.isDirectory(((FsDataPath) dataPath).getAbsoluteNioPath());
  }

  @Override
  public void create(DataPath dataPath) {
    FsDataPath fsDataPath = (FsDataPath) dataPath;
    if (!exists(dataPath)) {
      Path path = fsDataPath.getAbsoluteNioPath();
      try {
        if (Files.isDirectory(path)) {
          Files.createDirectory(path);
        } else {
          getFileManager(path).create(fsDataPath);
        }
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    } else {
      throw new RuntimeException("The data path (" + fsDataPath.getAbsoluteNioPath() + ") already exists");
    }
  }

  private FsFileManager getFileManager(Path path) {
    return getFileManager(path, null);
  }


  @Override
  public void drop(DataPath dataPath) {
    delete(dataPath);
  }

  @Override
  public void delete(DataPath dataPath) {
    FsDataPath fsDataPath = (FsDataPath) dataPath;
    try {
      Files.delete(fsDataPath.getAbsoluteNioPath());
    } catch (IOException e) {
      throw new RuntimeException("Unable to delete the file (" + fsDataPath + ")", e);
    }
  }

  @Override
  public void truncate(List<DataPath> dataPaths) {
    dataPaths.forEach(this::delete);
  }


  /**
   * See also how Github handles shell
   * https://docs.github.com/en/free-pro-team@latest/actions/reference/workflow-syntax-for-github-actions#jobsjob_idstepsrun
   * <p>
   * https://www.nextflow.io/docs/latest/process.html
   * The shebangdeclaration for a Perl script, for example, would look like: #!/usr/bin/env perl
   *
   * @param dataPath - a script data path
   */
  @Override
  public void execute(DataPath dataPath) {
    throw new UnsupportedOperationException("Not yet implemented");
  }


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

    try {

      List<DataPath> children = new ArrayList<>();
      Files.newDirectoryStream(path).forEach(p -> children.add(dataPath.getChild(p.getFileName().toString())));
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
  public String getString(DataPath dataPath) {
    FsDataPath fsDataPath = (FsDataPath) dataPath;
    try {
      return Fs.getFileContent(fsDataPath.getAbsoluteNioPath());
    } catch (NoSuchFileException e) {
      throw new RuntimeException("The resource (" + dataPath + ") does not exist, we can't read its content");
    }
  }

  @Override
  public TransferListener transfer(DataPath source, DataPath target, TransferProperties transferProperties) {
    FsDataPath fsSource = (FsDataPath) source;

    if (!this.isReadable(fsSource)) {
      throw new RuntimeException("The source file (" + source + ") is not readable. You don't have the permission.");
    }
    if (!exists(fsSource)) {
      throw new RuntimeException("The source file (" + source + ") does not exists.");
    }
    FsDataPath fsTarget = (FsDataPath) target;
    TransferListener transferListenerStream = new TransferListenerAtomic(new TransferSourceTarget(fsSource, fsTarget, transferProperties))
      .startTimer();
    try {

      /*
       * Load Operation check
       */
      TransferOperation loadOperation = transferProperties.getOperation();
      if (loadOperation == null) {
        loadOperation = TransferOperation.COPY;
      }
      FsLog.LOGGER_DB_FS.info(loadOperation + " data operation on the file (" + fsSource.getNioPath() + ") to the file(" + fsTarget.getNioPath() + ")");

      /*
       * Target check
       */
      Path targetNioPath = fsTarget.getAbsoluteNioPath();
      Fs.createDirectoryIfNotExists(targetNioPath.getParent());
      if (!Files.exists(targetNioPath)) {
        // Copy create the file and does not them by default
        if (loadOperation != TransferOperation.COPY && loadOperation != TransferOperation.MOVE) {
          if (
            transferProperties.getTargetOperations().contains(TransferResourceOperations.CREATE)
          ) {
            if (target.getOrCreateRelationDef().getColumnDefs().isEmpty() && !source.getOrCreateRelationDef().getColumnDefs().isEmpty()) {
              target.getOrCreateRelationDef()
                .mergeStructWithoutConstraints(source);
            }
            Tabulars.create(target);
          } else {
            throw new RuntimeException("The target data resource file does not exist and the target data operation (" + TransferResourceOperations.CREATE + ") was not present");
          }
        }
      }

      /*
       * Copy Options
       */
      List<StandardCopyOption> copyOptions = new ArrayList<>();
      if (transferProperties.getTargetOperations().contains(TransferResourceOperations.REPLACE)) {
        copyOptions.add(StandardCopyOption.REPLACE_EXISTING);
      }

      // Atomic
      ArrayList<StandardCopyOption> atomicOption = new ArrayList<>(copyOptions);
      atomicOption.add(StandardCopyOption.ATOMIC_MOVE);

      switch (loadOperation) {
        case COPY:
          try {
            /*
             * We try atomic
             */
            Files.copy(fsSource.getAbsoluteNioPath(), targetNioPath, atomicOption.toArray(new StandardCopyOption[0]));
          } catch (AtomicMoveNotSupportedException | UnsupportedOperationException e) {
            /*
             * Non-atomic then
             */
            Files.copy(fsSource.getAbsoluteNioPath(), targetNioPath, copyOptions.toArray(new StandardCopyOption[0]));
          }
          break;
        case MOVE:
          try {
            /*
             * We try atomic
             */
            Files.move(fsSource.getAbsoluteNioPath(), fsTarget.getAbsoluteNioPath(), atomicOption.toArray(new StandardCopyOption[0]));
          } catch (AtomicMoveNotSupportedException | UnsupportedOperationException e) {
            /*
             * Non-atomic then
             */
            Files.move(fsSource.getAbsoluteNioPath(), fsTarget.getAbsoluteNioPath(), copyOptions.toArray(new StandardCopyOption[0]));
          }
          break;
        case INSERT:
          /*
           * Existing file may have headers in the content at creation
           * Example: csv
           */
          Files.write(
            fsTarget.getAbsoluteNioPath(),
            Files.readAllBytes(fsSource.getAbsoluteNioPath()),
            StandardOpenOption.APPEND);
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
  public List<FsDataPath> select(DataPath baseDataPath, String patternOrPath, MediaType mediaType) {

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
      startPath = currentPath;
      for (String name : stringPath.getNames()) {
        if (name.equals(stringPath.getParentPathName())) {
          startPath = startPath.getParent();
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
         * This is not, we just lookup this level
         */
        List<Path> matchesPath = new ArrayList<>();
        for (Path currentRecursivePath : currentMatchesPaths) {
          List<Path> recursiveMatchesPath = getMatchesPath(pattern, currentRecursivePath, false);
          matchesPath.addAll(recursiveMatchesPath);
        }
        // Recursion
        currentMatchesPaths = matchesPath;
        // Break if there is no match
        if (matchesPath.size() == 0) {
          break;
        }
      } else {
        List<Path> matchedFiles = new ArrayList<>();
        while (currentMatchesPaths.size() != 0) {
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


}
