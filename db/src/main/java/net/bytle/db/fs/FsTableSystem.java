package net.bytle.db.fs;

import net.bytle.db.DbLoggers;
import net.bytle.db.database.DataStore;
import net.bytle.db.model.SqlDataType;
import net.bytle.db.spi.DataPath;
import net.bytle.db.spi.TableSystem;
import net.bytle.db.stream.InsertStream;
import net.bytle.db.stream.SelectStream;
import net.bytle.db.transfer.TransferListener;
import net.bytle.db.transfer.TransferProperties;
import net.bytle.db.transfer.TransferSourceTarget;
import net.bytle.db.uri.DataUri;
import net.bytle.fs.Fs;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

/**
 * A wrapper around a {@link java.nio.file.FileSystem}
 */
public class FsTableSystem extends TableSystem {

  // Static file system (Don't store state)
  private static FsTableSystem fsTableSystem;


  // private FsDataStore fsDataStore;
  // private final FsTableSystemProvider fsTableSystemProvider;
  // private FileSystem fileSystem;


  public static FsTableSystem of() {
    if (fsTableSystem == null) {
      fsTableSystem = new FsTableSystem();
    }
    return fsTableSystem;
  }


  /**
   * @param dataUri - a data Uri
   * @return a list of file that matches the uri segments
   * <p>
   * ex: the following file uri
   * /tmp/*.md
   * will return all md file in the tmp directory
   */
  public List<DataPath> getDataPaths(DataUri dataUri) {
    throw new RuntimeException("Never used ?");
    //String[] pathSegments = dataUri.getPath().split(this.fileSystem.getSeparator());

    // Start
//    Path startPath = Paths.get(".");
//    List<Path> currentMatchesPaths = new ArrayList<>();
//    currentMatchesPaths.add(startPath);
//
//    for (String s : pathSegments) {
//
//      // Glob to regex Pattern
//      String pattern = Globs.toRegexPattern(s);
//
//      // The list where the actual matches path will be stored
//      List<Path> matchesPath = new ArrayList<>();
//      for (Path currentPath : currentMatchesPaths) {
//        List<Path> paths = Fs.getChildrenFiles(currentPath);
//        for (Path childrenPath : paths) {
//          if (childrenPath.getFileName().toString().matches(pattern)) {
//            matchesPath.add(childrenPath);
//          }
//        }
//      }
//
//      if (matchesPath.size() == 0) {
//        break;
//      } else {
//        // Recursion
//        currentMatchesPaths = matchesPath;
//      }
//
//    }
//
//    return currentMatchesPaths.stream()
//      .map(path -> getFileManager(path).createDataPath(this,path))
//      .collect(Collectors.toList());
  }


  @Override
  public Boolean exists(DataPath dataPath) {

    final FsDataPath fsDataPath = (FsDataPath) dataPath;
    Path nioPath = fsDataPath.getNioPath();
    if (nioPath==null){
      return false;
    } else {
      return Files.exists(nioPath);
    }

  }

  @Override
  public SelectStream getSelectStream(DataPath dataPath) {

    FsRawDataPath fsDataPath = (FsRawDataPath) dataPath;
    return getFileManager(fsDataPath).getSelectStream(fsDataPath);

  }


  /**
   * The service provider
   *
   * @param path
   * @return
   */
  public FsFileManager getFileManager(Path path) {
    FsFileManager fileManager = null;
    List<FsFileManagerProvider> installedProviders = FsFileManagerProvider.installedProviders();
    for (FsFileManagerProvider structProvider : installedProviders) {
      if (structProvider.accept(path)) {
        fileManager = structProvider.getFsFileManager();
        if (fileManager == null) {
          String message = "The returned file manager is null for the provider (" + structProvider.getClass().toString() + ")";
          DbLoggers.LOGGER_DB_ENGINE.severe(message);
          throw new RuntimeException(message);
        }
      }
    }
    if (fileManager == null) {
      if (Files.isRegularFile(path)) {
        DbLoggers.LOGGER_DB_ENGINE.warning("No file structure was found for the file (" + path + "). It got therefore the default file manager.");
        fileManager = FsFileManager.getSingeleton();
      } else {
        fileManager = FsDirectoryManager.getSingeleton();
      }
    }
    return fileManager;
  }


  @Override
  public boolean isContainer(DataPath dataPath) {
    return Files.isDirectory(((FsDataPath) dataPath).getNioPath());
  }

  @Override
  public void create(DataPath dataPath) {
    FsDataPath fsDataPath = (FsDataPath) dataPath;
    if (!exists(dataPath)) {
      Path path = fsDataPath.getNioPath();
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
      throw new RuntimeException("The data path (" + fsDataPath.getNioPath() + ") already exists");
    }
  }


  @Override
  public SqlDataType getDataType(Integer typeCode) {

    throw new RuntimeException("not yet implemented");

  }

  @Override
  public void drop(DataPath dataPath) {
    delete(dataPath);
  }

  @Override
  public void delete(DataPath dataPath) {
    FsDataPath fsDataPath = (FsDataPath) dataPath;
    try {
      Files.delete(fsDataPath.getNioPath());
    } catch (IOException e) {
      throw new RuntimeException("Unable to delete the file (" + fsDataPath.toString() + ")", e);
    }
  }

  @Override
  public void truncate(DataPath dataPath) {
    delete(dataPath);
  }


  @Override
  public InsertStream getInsertStream(DataPath dataPath) {

    FsDataPath fsDataPath = (FsDataPath) dataPath;
    return getFileManager(fsDataPath).getInsertStream(fsDataPath);

  }

  private FsFileManager getFileManager(FsDataPath fsDataPath) {
    FsFileManager fileManager = fsDataPath.getFileManager();
    if (fileManager==null){
      fileManager = getFileManager(fsDataPath.getNioPath());
    }
    return fileManager;
  }

  @Override
  public List<DataPath> getChildrenDataPath(DataPath dataPath) {

    FsDataPath fsDataPath = (FsDataPath) dataPath;
    Path path = fsDataPath.getNioPath();
    if (!Files.isDirectory(path)) {
      throw new RuntimeException("The data path (" + dataPath + ") is not a directory and therefore has no child");
    }

    try {

      List<DataPath> children = new ArrayList<>();
      Files.newDirectoryStream(path).forEach(p -> children.add(dataPath.getChild(p.getFileName().toString())));
      return children;

    } catch (IOException e) {
      throw new RuntimeException(e);
    }

  }

  /**
   * Move (for now, just a append data move, the source file is not deleted)
   *
   * @param source             - the source data path
   * @param target             - the target data path
   * @param transferProperties - the properties of the transfer
   */
  @Override
  public void move(DataPath source, DataPath target, TransferProperties transferProperties) {
    FsDataPath fsSource = (FsDataPath) source;
    FsDataPath fsTarget = (FsDataPath) target;
    try {

      // The below statement will delete the source file
      Files.move(fsSource.getNioPath(), fsTarget.getNioPath(), StandardCopyOption.REPLACE_EXISTING);

      // This statement append
      //      Files.write(
      //        fsTarget.getNioPath(),
      //        Files.readAllBytes(fsSource.getNioPath()),
      //        StandardOpenOption.APPEND);

    } catch (IOException e) {
      throw new RuntimeException("Unable to move the file", e);
    }

  }


  @Override
  public Boolean isEmpty(DataPath queue) {
    throw new RuntimeException("not yet implemented");
  }

  @Override
  public long size(DataPath dataPath) {
    long i = 0;
    try (SelectStream selectStream = getSelectStream(dataPath)) {
      while (selectStream.next()) {
        i++;
      }
    }
    return i;
  }

  @Override
  public boolean isDocument(DataPath dataPath) {
    Path path = ((FsDataPath) dataPath).getNioPath();
    return !Files.isDirectory(path);
  }


  @Override
  public String getString(DataPath dataPath) {
    FsDataPath fsDataPath = (FsDataPath) dataPath;
    return Fs.getFileContent(fsDataPath.getNioPath());
  }

  @Override
  public TransferListener copy(DataPath source, DataPath target, TransferProperties transferProperties) {
    FsRawDataPath fsSource = (FsRawDataPath) source;
    if (!exists(fsSource)){
      throw new RuntimeException("The source file ("+source+") does not exists");
    }
    FsRawDataPath fsTarget = (FsRawDataPath) target;
    TransferListener transferListener = TransferListener.of(TransferSourceTarget.of(fsSource, fsTarget))
      .startTimer();
    try {
      Files.copy(fsSource.getNioPath(), fsTarget.getNioPath());
    } catch (IOException e) {
      transferListener.addException(e);
      throw new RuntimeException(e);
    }
    return transferListener.stopTimer();
  }

  @Override
  public TransferProperties insert(DataPath source, DataPath target, TransferProperties transferProperties) {
    throw new RuntimeException("Not yet implemented");
  }

  @Override
  public List<DataPath> getDescendants(DataPath dataPath) {
    throw new RuntimeException("Not yet implemented");
  }

  @Override
  public List<DataPath> getDescendants(DataPath dataPath, String glob) {
    throw new RuntimeException("Not yet implemented");
  }

  @Override
  public List<DataPath> getReferences(DataPath dataPath) {
    throw new RuntimeException("Not yet implemented");
  }

  @Override
  public DataStore createDataStore(String name, String url) {

    return new FsDataStore(name, url);

  }


}
