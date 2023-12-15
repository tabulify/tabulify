package net.bytle.fs;


import com.ibm.icu.text.CharsetDetector;
import com.ibm.icu.text.CharsetMatch;
import net.bytle.crypto.Digest;
import net.bytle.exception.NotAbsoluteException;
import net.bytle.os.Oss;
import net.bytle.type.MediaType;
import net.bytle.type.MediaTypes;

import java.io.BufferedInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.UserDefinedFileAttributeView;
import java.util.*;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static net.bytle.os.Oss.WIN;


public class Fs {


  /**
   * A safe method even if the string is not a path
   *
   * @param s - a string to test if s is a path to a valid file
   * @return true if this a regular file
   */
  public static boolean isFile(String s) {
    try {
      return Files.isRegularFile(Paths.get(s));
    } catch (java.nio.file.InvalidPathException e) {
      return false;
    }
  }


  /**
   * Safe is directory method even if the string is not a path
   *
   * @param s - a string path
   * @return true if the string path represents a directory
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
   * @param path           - a directory or file path where to start from
   * @param pathsCollector - the object that collects the path
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
   * @param content - the file content
   * @return a path to a txt file with the string as content
   */
  public static Path createTempFileWithContent(String content) {
    return createTempFileWithContent(content, ".txt");
  }

  /**
   * @param content -  the content of the file
   * @param suffix  - the file suffix. Example: ".txt"
   * @return a temp file in the temp directory
   */
  public static Path createTempFileWithContent(String content, String suffix) {

    try {

      Path tempFile = createTempFile(suffix);
      Files.write(tempFile, content.getBytes());

      return tempFile;

    } catch (IOException e) {

      throw new RuntimeException(e);

    }

  }

  public static Path createTempFile(String suffix) {
    Path temp = createTempDirectory(null);

    try {
      return Files.createTempFile(temp, null, suffix);
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
   * @param path - the file path
   * @return See also: <a href="https://github.com/google/guava/wiki/HashingExplained">...</a>
   */
  public static String getMd5(Path path) {
    if (Files.isDirectory(path)) {
      throw new RuntimeException("Md5 calculation for directory is not implemented. No md5 for (" + path.toAbsolutePath());
    }
    try {
      byte[] bytes = Files.readAllBytes(path);
      return Digest.createFromBytes(Digest.Algorithm.MD5, bytes).getHashHex();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }


  /**
   * Return the user directory for an application
   * known as the AppData Directory.
   * <p>
   * This directory contains data for one user.
   *
   * @param appName - the application name
   * @return the path for user data for this app
   */
  public static Path getUserAppData(String appName) {
    Path appData;
    //noinspection SwitchStatementWithTooFewBranches
    switch (Oss.getType()) {
      case WIN:
        // ie %LOCALAPPDATA% env variable
        // C:\Users\gerardnico\AppData\Local
        // this location does not roam
        // while %APPDATA% does C:\Users\gerardnico\AppData\roaming
        appData = getUserHome().resolve("AppData").resolve("Local").resolve(appName);
        break;
      default:
        // Linux ...
        appData = getUserHome().resolve("." + appName);
    }

    try {
      Files.createDirectories(appData);
      return appData;
    } catch (IOException e) {
      throw new RuntimeException("Unable to create the user app data directory (" + appData.toAbsolutePath() + ")", e);
    }

  }


  public static Path getUserHome() {
    final String home = System.getProperty("user.home");
    return Paths.get(home);
  }

  @SuppressWarnings("unused")
  private static String getPathSeparator() {
    return System.getProperty("file.separator");
  }

  /**
   * @return the system (process) encoding
   * ie the value of the system property file.encoding
   */
  @SuppressWarnings("unused")
  private static String getSystemEncoding() {
    return System.getProperty("file.encoding");
  }

  @SuppressWarnings("unused")
  private static Path getTempDir() {
    return Paths.get(System.getProperty("java.io.tmpdir"));
  }


  /**
   * Create a file and all sub-directories if needed
   *
   * @param path the file to create
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
   * to write a string to a file in UTF8
   * without exception handling
   *
   * @param path - the path
   * @param s    - the content to add to the path
   */
  public static void write(Path path, String s) {
    try {
      Files.write(path, s.getBytes(StandardCharsets.UTF_8));
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * A wrapper around path.relativize(base)
   *
   * @param path - the path to relativize
   * @param base - the base path
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
   * @param path the path
   * @return true if the path has a root (is absolute)
   */
  public static boolean isAbsolute(Path path) {
    return path.getRoot() != null;
  }


  /**
   * @param path - the directory path
   * @return the children path of a directory
   */
  public static List<Path> getChildrenFiles(Path path) {

    try (DirectoryStream<Path> paths = Files.newDirectoryStream(path)) {
      List<Path> childrenPaths = new ArrayList<>();
      for (Path childPath : paths) {
        childrenPaths.add(childPath);
      }
      return childrenPaths;
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * @param parts - the parts of the path name
   * @return a local pah from an array string
   */
  public static Path getPath(String[] parts) {
    String[] more = Arrays.copyOfRange(parts, 1, parts.length);
    return Paths.get(parts[0], more);
  }

  /**
   * @param path the path
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
   *
   * @param path the path to delete
   * @return all deleted path
   */
  private static List<Path> delete(Path path) {
    try {

      List<Path> deletedPaths = new ArrayList<>();

      if (Files.isDirectory(path)) {
        try (Stream<Path> walk = Files.walk(path)) {
          walk
            .map(Path::toFile)
            .sorted(Comparator.reverseOrder())
            .forEach(file -> {
              boolean result = file.delete();
              if (result) {
                deletedPaths.add(file.toPath());
              } else {
                throw new RuntimeException("Unable to delete the file (" + file + ")");
              }
            });
          return deletedPaths;
        }
      }

      Files.delete(path);
      deletedPaths.add(path);
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
    } catch (AccessDeniedException e) {
      // the temp directory may be C:\windows
      // when there is no variable set
      // that is not accessible
      throw new RuntimeException("The access to the temporary directory was denied with the following message (" + e.getMessage() + "). \n The root cause may be that your environment does not have any TEMP / TMP variables. \n As a workaround, you can add the following option `-Djava.io.tmpdir=/temp/path` to a writable directory in your script.");
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

  /**
   * @return the temporary directory
   * <p>
   * Environment variable that have an influence:
   * * TEMP for windows by default: "C:\\Users\\user\\AppData\\Local\\Temp"
   */
  public static Path getTempDirectory() {
    return Paths.get(System.getProperty("java.io.tmpdir"));
  }


  public static String getExtension(Path path) {
    return getExtension(path.getFileName().toString());
  }

  public static String getExtension(String fullFileName) {
    int i = fullFileName.lastIndexOf('.');
    if (i == -1) {
      return null;
    } else {
      return fullFileName.substring(i + 1);
    }
  }

  /**
   * Return the part of the file without its extension
   *
   * @param fullFileName a full file name string
   * @return the extension if any
   */
  public static String getFileNameWithoutExtension(String fullFileName) {
    return fullFileName.substring(0, fullFileName.lastIndexOf('.'));
  }

  /**
   * Create the directory and all its children if not exist
   *
   * @param path a directory path
   */
  public static void createDirectoryIfNotExists(Path path) {
    try {
      if (Files.notExists(path)) {
        Files.createDirectories(path);
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }


  /**
   * @return the first root of the current file system
   * This is a utility function mostly for test purpose
   * in order to create a string with a root in it
   */
  public static String getFirstRoot() {

    return StreamSupport
      .stream(Paths.get(".").getFileSystem().getRootDirectories().spliterator(), false)
      .map(Path::toString)
      .findFirst()
      .orElse(null);

  }

  public static String getSeparator() {
    return Paths.get(".").getFileSystem().getSeparator();
  }


  /**
   * A wrapper around {@link Files#move(Path, Path, CopyOption...) move} that makes sure
   * that the target directory is already created
   *
   * @param source the source to move
   * @param target the location of the target
   */
  public static void move(Path source, Path target, CopyOption... copyOptions) {
    try {
      Fs.createDirectoryIfNotExists(target.getParent());
      Files.move(source, target, copyOptions);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * @param path - an absolute or relative path
   * @param name - a name
   * @return the path until a certain name was found (name included)
   */
  public static Path getPathUntilName(Path path, String name) {

    Path pathUntil = null;
    boolean found = false;
    for (int i = 0; i < path.getNameCount(); i++) {
      Path subName = path.getName(i);
      if (pathUntil == null) {
        if (path.isAbsolute()) {
          pathUntil = path.getRoot().resolve(subName);
        } else {
          pathUntil = subName;
        }
      } else {
        pathUntil = pathUntil.resolve(subName);
      }
      if (subName.getFileName().toString().equals(name)) {
        found = true;
        break;
      }
    }
    if (found) {
      return pathUntil;
    } else {
      return null;
    }
  }

  /**
   * <a href="http://userguide.icu-project.org/conversion/detection">...</a>
   *
   * @param path - the path
   * @return a encoding value or null if this is not possible
   * See possible values at
   * <a href="http://userguide.icu-project.org/conversion/detection#TOC-Detected-Encodings">...</a>
   */
  public static String detectCharacterSet(Path path) {
    /**
     * Buffered reader is important because
     * the detector make us of the mark/reset
     */
    try (InputStream bis = new BufferedInputStream(Files.newInputStream(path))) {
      CharsetDetector charsetDetector = new CharsetDetector();
      charsetDetector.setText(bis);
      CharsetMatch match = charsetDetector.detect();
      if (match == null) {
        return null;
      } else {
        return match.getName();
      }
    } catch (Exception e) {
      /**
       * If the file is used, we can get a java.nio.file.FileSystemException exception
       * such as `The process cannot access the file`
       * Example on windows with `C:/Users/userName/NTUSER.DAT`
       * <p>
       * We can also get a problem when basic authentication is mandatory
       * for http path
       */
      FsLog.LOGGER.fine("Error while reading the file (" + path + ")" + e.getMessage());
      return null;
    }

  }


  /**
   * @param path - the path
   * @return the string of a text file
   * <p>
   */
  public static String getFileContent(Path path, Charset charset) throws NoSuchFileException {

    try {
      return Files.readString(path, charset);
    } catch (NoSuchFileException e) {
      throw e;
    } catch (IOException e) {
      throw new RuntimeException(e);
    }

  }

  public static String getFileContent(Path path) throws NoSuchFileException {
    return getFileContent(path, Charset.defaultCharset());
  }

  public static MediaType detectMediaType(Path path) throws NotAbsoluteException {

    return MediaTypes.createFromPath(path);

  }

  public static boolean isRoot(Path path) {
    return path.getRoot().equals(path);
  }

  /**
   * @param name - the name of the closest path
   * @return the closest path
   */
  public static Path closest(Path path, String name) throws FileNotFoundException {

    Path resolved;
    Path actual = path;
    if (!Files.isDirectory(path)) {
      actual = path.getParent();
    }
    // toAbsolute is needed otherwise the loop
    // will not stop at the root of the file system
    // but at the root of the relative path
    actual = actual.toAbsolutePath();
    while (actual != null) {
      resolved = actual.resolve(name);
      if (Files.exists(resolved)) {
        return resolved;
      }
      actual = actual.getParent();
    }
    throw new FileNotFoundException("No closest file was found");

  }

  public static Path getUserDesktop() {
    return Fs.getUserHome()
      .resolve("Desktop");
  }

  public static String getInputStreamContent(InputStream inputStream) {
    Scanner s = new Scanner(inputStream).useDelimiter("\\A");
    return s.hasNext() ? s.next() : "";
  }

  public static void setUserAttribute(Path path, String key, String value) throws IOException {


    if (!Files.exists(path)) {
      return;
    }

    FileStore store = Files.getFileStore(path);
    if (store.supportsFileAttributeView(UserDefinedFileAttributeView.class)) {
      UserDefinedFileAttributeView view = Files
        .getFileAttributeView(path, UserDefinedFileAttributeView.class);
      ByteBuffer valueByteBuffer = Charset.defaultCharset().encode(value);
      view.write(key, valueByteBuffer);
    }


  }

}
