package net.bytle.db.fs;

import net.bytle.db.DbLoggers;
import net.bytle.db.spi.DataPath;
import net.bytle.db.spi.DataSystem;
import net.bytle.db.stream.InsertStream;
import net.bytle.db.stream.SelectStream;
import net.bytle.db.transfer.TransferListenerStream;
import net.bytle.db.transfer.TransferListener;
import net.bytle.db.transfer.TransferSourceTarget;
import net.bytle.fs.Fs;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * A wrapper around a {@link java.nio.file.FileSystem}
 *
 */
public class FsDataSystem implements DataSystem {


  private FsDataStore dataStore;

  public FsDataSystem(FsDataStore fsDataStore) {
    dataStore = fsDataStore;
  }


  @Override
  public FsDataStore getDataStore() {
    return dataStore;
  }

  @Override
  public Boolean exists(DataPath dataPath) {

    final FsDataPath fsDataPath = (FsDataPath) dataPath;
    Path nioPath = fsDataPath.getNioPath();
    if (nioPath == null) {
      return false;
    } else {
      return Files.exists(nioPath);
    }

  }

  @Override
  public SelectStream getSelectStream(DataPath dataPath) {

    FsDataPath fsDataPath = (FsDataPath) dataPath;
    return getFileManager(fsDataPath).getSelectStream(fsDataPath);

  }


  /**
   * The service provider
   *
   * @param path
   * @return
   */
  public FsFileManager getFileManager(Path path) {
    FsFileManager fileManager;
    List<FsFileManagerProvider> installedProviders = FsFileManagerProvider.installedProviders();
    for (FsFileManagerProvider structProvider : installedProviders) {
      if (structProvider.accept(path)) {
        fileManager = structProvider.getFsFileManager();
        if (fileManager == null) {
          String message = "The returned file manager is null for the provider (" + structProvider.getClass().toString() + ")";
          DbLoggers.LOGGER_DB_ENGINE.severe(message);
          throw new RuntimeException(message);
        }
        return fileManager;
      }
    }
    // No file manager found
    if (Files.isRegularFile(path)) {
      DbLoggers.LOGGER_DB_ENGINE.warning("No file manager was found for the file (" + path + "). It got therefore the default file manager.");
      DbLoggers.LOGGER_DB_ENGINE.warning("Did you add the manager for the file in the class path ?");
      fileManager = FsBinaryFileManager.getSingeleton();
    } else {
      fileManager = FsDirectoryManager.getSingeleton();
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
    if (fileManager == null) {
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
   */
  @Override
  public void move(DataPath source, DataPath target) {
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
    return getFileManager((FsDataPath) dataPath).getSize((FsDataPath) dataPath);
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
  public TransferListener copy(DataPath source, DataPath target) {
    FsDataPath fsSource = (FsDataPath) source;
    if (!exists(fsSource)) {
      throw new RuntimeException("The source file (" + source + ") does not exists");
    }
    FsDataPath fsTarget = (FsDataPath) target;
    TransferListenerStream transferListenerStream = (TransferListenerStream) new TransferListenerStream(new TransferSourceTarget(fsSource, fsTarget))
      .startTimer();
    try {
      Files.copy(fsSource.getNioPath(), fsTarget.getNioPath());
    } catch (IOException e) {
      transferListenerStream.addException(e);
      throw new RuntimeException(e);
    }
    return transferListenerStream.stopTimer();
  }

  @Override
  public TransferListener insert(DataPath source, DataPath target) {
    throw new RuntimeException("Not yet implemented");
  }


  @Override
  public List<DataPath> getDescendants(DataPath dataPath) {
    throw new RuntimeException("Not yet implemented");
  }

  @Override
  public List<DataPath> getDescendants(DataPath dataPath, String glob) {
    FsDataPath fsDataPath = (FsDataPath) dataPath;
    Path path = fsDataPath.getNioPath().toAbsolutePath().normalize();
    String separator = path.getFileSystem().getSeparator();
    if (!separator.equals(Fs.GLOB_SEPARATOR)) {
      glob = glob.replace(Fs.GLOB_SEPARATOR, separator);
    }
    String finalGlob = path + separator + glob;
    List<Path> paths = Fs.getFilesByGlob(finalGlob);
    return paths.stream()
      .map(p -> getFileManager(p).createDataPath(fsDataPath.getDataStore(), p))
      .collect(Collectors.toList());
  }

  @Override
  public List<DataPath> getReferences(DataPath dataPath) {
    throw new RuntimeException("Not yet implemented");
  }


}
