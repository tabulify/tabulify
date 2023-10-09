package net.bytle.doctest;

import net.bytle.fs.Fs;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public class DocCache {


  private final Path cacheDirectory;

  private DocCache(String name) {

    cacheDirectory = Paths.get(Fs.getUserAppData(DocExecutor.APP_NAME).toString(), name);
    if (!Files.exists(cacheDirectory)) {
      try {
        Files.createDirectory(cacheDirectory);
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }


  }

  /**
   * The name is a namespace to be able to cache two different set of doc
   *
   * @param name
   * @return
   */
  public static DocCache get(String name) {

    return new DocCache(name);

  }

  /**
   * @param path
   * @return the MD5 of the path
   */
  public String getMd5(Path path) {
    Path cacheFilePath = getPathCacheFile(path);
    if (Files.exists(cacheFilePath)) {
      return Fs.getMd5(cacheFilePath);
    } else {
      return null;
    }
  }

  /**
   * @param path
   * @return the file that is cached for this path
   */
  protected Path getPathCacheFile(Path path) {
    Path relativeCachePath = path;
    if (relativeCachePath.isAbsolute()) {
      relativeCachePath = Fs.relativize(path, path.getRoot());
    }
    return Paths.get(cacheDirectory.toString(), relativeCachePath.toString()).normalize();
  }

  /**
   * Cache/store this path in the cache
   *
   * @param path
   */
  public void store(Path path) {
    try {
      Path cachePath = getPathCacheFile(path);
      Path parent = cachePath.getParent();
      if (!(Files.exists(parent))) {
        Files.createDirectories(parent);
      }
      Fs.overwrite(path, cachePath);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }

  }

  /**
   * @param path
   * @return {@link DocUnit} for this path, null if the path is not in the cache
   */
  public List<DocUnit> getDocTestUnits(Path path) {
    final Path pathCacheFile = getPathCacheFile(path);
    if (Files.exists(pathCacheFile)) {
      return DocParser.getDocTests(pathCacheFile);
    } else {
      return null;
    }
  }

  public List<Path> purgeAll() {
    List<Path> paths = Fs.getDescendantFiles(cacheDirectory);
    Fs.deleteIfExists(cacheDirectory);
    return paths;
  }

  public void purge(Path path) {
    Path cacheFilePath = getPathCacheFile(path);
    Fs.deleteIfExists(cacheFilePath);
  }
}
