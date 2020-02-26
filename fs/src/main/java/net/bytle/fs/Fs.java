package net.bytle.fs;


import net.bytle.os.Oss;
import net.bytle.regexp.Globs;
import net.bytle.type.Arrayss;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.FileAttribute;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static net.bytle.os.Oss.LINUX;
import static net.bytle.os.Oss.WIN;
import static net.bytle.type.Bytes.printHexBinary;


public class Fs {


  public static final String GLOB_SEPARATOR = "/";
  private static final Logger LOGGER = LoggerFactory.getLogger(Fs.class);

  /**
   * A safe method even if the string is not a path
   *
   * @param s
   * @return
   */
  public static boolean isFile(String s) {
    try {
      return Files.isRegularFile(Paths.get(s));
    } catch (java.nio.file.InvalidPathException e) {
      return false;
    }
  }

  /**
   * @param path
   * @return a string for the file content with the os line separator
   * <p>
   * One liner without getting the OS line separator:
   * new String(Files.readAllBytes(jsonFile))
   */
  public static String getFileContent(Path path) {
    try {

      return Files.lines(path)
        .collect(Collectors.joining(System.getProperty("line.separator")));

    } catch (FileNotFoundException e) {
      throw new RuntimeException("Unable to find the file (" + path.toAbsolutePath().normalize().toString() + ")", e);
    } catch (Exception e) {
      throw new RuntimeException("Error on the path ("+path.toString()+")",e);
    }
  }

  /**
   * Safe is directory method even if the string is not a path
   *
   * @param s
   * @return
   */
  public static boolean isDirectory(String s) {
    try {
      return Files.isDirectory(Paths.get(s));
    } catch (java.nio.file.InvalidPathException e) {
      return false;
    }
  }

  /**
   * An alias to the function {@link Files#write(Path, byte[], OpenOption...)}
   * without any option.
   *
   * @param s    - the string to write
   * @param path - the path to the file to write
   */
  public static void toFile(String s, Path path) {
    try {
      Files.write(path, s.getBytes());
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Same as Files.walk(Paths.get(path))
   *
   * @param path - the start path to scan
   * @return - the children of a directory or the file if it's a file
   */
  public static List<Path> getDescendantFiles(Path path) {

    // Path to return
    List<Path> pathsCollector = new ArrayList<>();
    if (Files.isDirectory(path)) {
      try (DirectoryStream<Path> dirStream = Files.newDirectoryStream(path)) {

        for (Path childPath : dirStream) {
          if (Files.isRegularFile(childPath)) {
            pathsCollector.add(childPath);
          } else {
            addChildFiles(pathsCollector, childPath);
          }
        }

      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    } else {
      pathsCollector.add(path);
    }
    return pathsCollector;

  }

  /**
   * Recursive function usd by {@link #getDescendantFiles(Path)}
   *
   * @param path
   * @param pathsCollector
   */
  static void addChildFiles(List<Path> pathsCollector, Path path) {

    if (Files.isDirectory(path)) {

      try (DirectoryStream<Path> childrenFiles = Files.newDirectoryStream(path)) {

        for (Path childPath : childrenFiles) {
          if (Files.isRegularFile(childPath)) {
            pathsCollector.add(childPath);
          } else {
            addChildFiles(pathsCollector, childPath);
          }
        }

      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    } else {
      pathsCollector.add(path);
    }

  }

  /**
   * @param content
   * @return a path to a txt file with the string as content
   */
  public static Path createTempFile(String content) {
    return createTempFile(content, ".txt");
  }

  /**
   * @param content -  the content of the file
   * @param suffix  - the file suffix. Example: ".txt"
   * @return a temp file in the temp directory
   */
  public static Path createTempFile(String content, String suffix) {

    try {

      Path temp = createTempDirectory(null);

      Path tempFile = Files.createTempFile(temp, null, suffix);
      Files.write(tempFile, content.getBytes());

      return tempFile;

    } catch (IOException e) {

      throw new RuntimeException(e);

    }

  }


  /**
   * @param prefix - a prefix to generate the directory name (may be null)
   * @return a temp directory
   */
  public static Path createTempDirectory(String prefix) {

    try {

      return Files.createTempDirectory(prefix);

    } catch (IOException e) {

      throw new RuntimeException(e);

    }

  }

  /**
   * @param path
   * @return See also: http://code.google.com/p/guava-libraries/wiki/HashingExplained
   */
  public static String getMd5(Path path) {

    if (Files.isDirectory(path)) {
      throw new RuntimeException("Md5 calculation for directory is not implemented. No md5 for (" + path.toAbsolutePath().toString());
    }
    try {
      byte[] bytes = Files.readAllBytes(path);
      byte[] hash = MessageDigest.getInstance("MD5").digest(bytes);
      return printHexBinary(hash);
    } catch (NoSuchAlgorithmException | IOException e) {
      throw new RuntimeException(e);
    }
  }


  /**
   * Return the AppData Directory
   * This directory contains data for one user.
   *
   * @param appName
   * @return
   */
  public static Path getAppData(String appName) {
    Path appData;
    switch (Oss.getType()) {
      case WIN:
        appData = Paths.get(getUserHome().toString(), "AppData", "Local", appName);
        break;
      case LINUX:
        appData = Paths.get(getUserHome().toString(), "." + appName);
        break;
      default:
        throw new RuntimeException("AppData directory for OS " + Oss.getName() + " is not implemented");
    }

    try {
      Files.createDirectories(appData);
      return appData;
    } catch (IOException e) {
      throw new RuntimeException(e);
    }

  }


  private static Path getUserHome() {
    final String home = System.getProperty("user.home");
    return Paths.get(home);
  }

  private static String getPathSeparator() {
    return System.getProperty("file.separator");
  }

  /**
   * @return the system (process) encoding
   * ie the value of the system property file.encoding
   */
  private static String getSystemEncoding() {
    return System.getProperty("file.encoding");
  }

  private static Path getTempDir() {
    return Paths.get(System.getProperty("java.io.tmpdir"));
  }


  /**
   * Create a file and all sub-directories if needed
   *
   * @param path
   */
  public static void createFile(Path path) {
    try {
      Path parent = path.getParent();
      Files.createDirectories(parent);
      Files.createFile(path);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Wrapper around {@link Files#write(Path, byte[], OpenOption...)}
   * to write a string to a file
   * without exception handling
   *
   * @param path
   * @param s
   */
  public static void write(Path path, String s) {
    try {
      Files.write(path, s.getBytes());
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * A wrapper around path.relativize(base)
   *
   * @param path
   * @param base
   * @return a relative path
   */
  public static Path relativize(Path path, Path base) {
    return base.relativize(path);
  }

  public static void overwrite(Path source, Path target) {
    try {
      Files.write(target, Files.readAllBytes(source), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * @param path     - a regular file or a directory
   * @param basePath - a base directory
   * @return a list of name between the path and the base path
   * <p>
   * Example
   * * basePath = /tmp
   * * path = /tmp/foo/bar/blue
   * You will get { 'foo', 'bar'}
   */
  public static List<String> getDirectoryNamesInBetween(Path path, Path basePath) {

    if (basePath.getNameCount() > path.getNameCount()) {
      throw new RuntimeException("The base path should have less levels than the path");
    }
    List<String> names = new ArrayList<>();
    // -1 in the end limit because the last name if the file name, we don't return it
    for (int i = 0; i < path.getNameCount() - 1; i++) {
      String name = path.getName(i).toString();
      if (i <= basePath.getNameCount() - 1) {
        String baseName = basePath.getName(i).toString();
        if (!baseName.equals(name)) {
          throw new RuntimeException("The path doesn't share a common branch with the base path. At the level (" + (i + 1) + ", the name is different. We got (" + name + ") for the path and (" + baseName + ") for the base path");
        }
      } else {
        names.add(name);
      }
    }
    return names;
  }


  /**
   * path.isAbsolute just tell you that the object path is absolute, not the path
   * <p>
   * This method check if there is a root
   * If this is the case, the path is absolute otherwise not.
   *
   * @param path
   * @return true if the path has a root (is absolute)
   */
  public static boolean isAbsolute(Path path) {
    if (path.getRoot() != null) {
      return true;
    } else {
      return false;
    }
  }


  /**
   * @param path
   * @return the children path of a directory
   */
  public static List<Path> getChildrenFiles(Path path) {

    try {
      List<Path> childrenPaths = new ArrayList<>();
      for (Path childPath : Files.newDirectoryStream(path)) {
        childrenPaths.add(childPath);
      }
      return childrenPaths;
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * @param segments
   * @return a local pah from an array string
   */
  public static Path getPath(String[] segments) {
    String[] more = Arrays.copyOfRange(segments, 1, segments.length);
    return Paths.get(segments[0], more);
  }

  /**
   * @param path
   * @return the file name without the extension
   */
  public static String getFileNameWithoutExtension(Path path) {
    final String fileName = path.getFileName().toString();
    final int endIndex = fileName.indexOf(".");
    if (endIndex == -1) {
      return fileName;
    } else {
      return fileName.substring(0, endIndex);
    }
  }

  /**
   * @param path - a file or a directory
   */
  public static List<Path> deleteIfExists(Path path) {
    if (Files.exists(path)) {
      return Fs.delete(path);
    } else {
      return new ArrayList<>();
    }
  }

  /**
   * Delete a file or a directory (with all its content)
   * @param path
   * @return
   */
  private static List<Path> delete(Path path) {
    try {
      List<Path> deletedPaths = new ArrayList<>();
      if (Files.isDirectory(path)) {
        try (Stream<Path> walk = Files.walk(path)) {
          deletedPaths = walk.sorted(Comparator.reverseOrder())
            .filter(s -> !Files.isDirectory(s))
            .flatMap(s -> Fs.delete(s).stream())
            .collect(Collectors.toList());
        }
      } else {

        Files.delete(path);
        deletedPaths.add(path);

      }
      return deletedPaths;
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Return a temporary file path.
   * <p>
   * This function is a wrapper around the function {@link Files#createTempFile(String, String, FileAttribute[])}
   * but delete the file to return only a unique path
   * <p>
   * If you want to create it, use a create function such as {@link #createFile(Path)}
   *
   * @param prefix the beginning of the file name
   * @param suffix the extension (example .txt) when null '.tmp'
   * @return a temporary file path
   * <p>
   * Example:
   * Path path = Fs.getTempFilePath("test",".csv");
   */
  public static Path getTempFilePath(String prefix, String suffix) {

    try {
      Path path = Files.createTempFile(prefix, suffix);
      Files.deleteIfExists(path);
      return path;
    } catch (IOException e) {
      throw new RuntimeException(e);
    }

  }

  public static boolean isEmpty(Path path) {
    try {
      return Files.size(path) == 0;
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public static Path getTempDirectory() {
    try {
      return Files.createTempDirectory("tmp");
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }


  public static String getExtension(String fullFileName) {
    int i = fullFileName.lastIndexOf('.');
    if (i==-1){
      return "";
    } else {
      return fullFileName.substring(i + 1);
    }
  }

  /**
   * Return the part of the file without its extension
   * @param fullFileName
   * @return
   */
  public static String getFileNameWithoutExtension(String fullFileName) {
    return fullFileName.substring(0, fullFileName.lastIndexOf('.'));
  }

  public static void createDirectoryIfNotExists(Path path) {
    try {
      if (Files.notExists(path)) {
        Files.createDirectory(path);
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * @param glob - a glob
   * @return a list of file that matches the glob
   * <p>
   * ex: the following glob
   * /tmp/*.md
   * will return all md file in the tmp directory
   */
  static public List<Path>  getFilesByGlob(String glob) {

    // Get the file system
    FileSystem fileSystem = Paths.get(".").getFileSystem();

    // Absolute path
    String finalGlob = glob;
    Path matchedRootPath = StreamSupport
      .stream(fileSystem.getRootDirectories().spliterator(), false)
      .filter(s-> finalGlob.startsWith(s.toString()))
      .findFirst()
      .orElse(null);

    Path startPath;
    if (matchedRootPath!=null){
      startPath = matchedRootPath;
      glob = glob.replace(matchedRootPath.toString(),"");
    } else {
      startPath = Paths.get(".");
    }

    String separator = fileSystem.getSeparator();
    if (separator.equals("\\")){
      separator = "\\\\";
    }
    String[] globNames = glob.split(separator);

    // Init
    List<Path> currentMatchesPaths = new ArrayList<>();
    currentMatchesPaths.add(startPath);
    for (String globPattern: globNames) {

      FsShortFileName sfn = FsShortFileName.of(globPattern);
      Pattern pattern = null;
      if (!sfn.isShortFileName()) {
        pattern = Pattern.compile(Globs.toRegexPattern(globPattern));
      } else {
        pattern = Pattern.compile(Globs.toRegexPattern(sfn.getShortName()+"*"),Pattern.CASE_INSENSITIVE);
      }

      // The list where the actual matches path will be stored
      List<Path> matchesPath = new ArrayList<>();
      for (Path currentPath : currentMatchesPaths) {
        // There is also newDirectoryStream
        // https://docs.oracle.com/javase/8/docs/api/java/nio/file/Files.html#newDirectoryStream-java.nio.file.Path-java.lang.String-
        // but yeah ...
        if (Files.isDirectory(currentPath)) {
          try {
            List<Path> paths = Fs.getChildrenFiles(currentPath);
            for (Path childrenPath : paths) {
              if (pattern.matcher(childrenPath.getFileName().toString()).find()) {
                matchesPath.add(childrenPath);
              }
            }
          } catch (Exception e){
            if (e.getCause().getClass().equals(java.nio.file.AccessDeniedException.class)) {
              LOGGER.warn("The path (" + currentPath + ") was denied");
            } else {
              throw e;
            }
          }
        } else {
          if (pattern.matcher(currentPath.getFileName().toString()).find()) {
            matchesPath.add(currentPath);
          }
        }
      }
      // Recursion
      currentMatchesPaths = matchesPath;
      // Break if there is not match
      if (matchesPath.size() == 0) {
        break;
      }
    }

    return currentMatchesPaths;

  }

  /**
   * A path cannot have special character such as a star
   * This utility function creates a glob path pattern with the file separator of the local system
   * @param names - glob patterns
   * @return a glob path pattern for the local file system
   */
  public static String createGlobPath(String name, String... names) {
    names = Arrayss.concat(name, names);
    return String.join(System.getProperty("file.separator"), names);
  }

  public static List<Path> getFilesByGlob(String name, String... names) {
    return getFilesByGlob(createGlobPath(name,names));
  }
}
