package com.tabulify.fs;

import com.tabulify.spi.ConnectionResourcePathAbs;
import net.bytle.regexp.Glob;
import net.bytle.type.Arrayss;

import java.nio.file.FileSystem;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.StreamSupport;

/**
 * A File system glob path utility
 * <p>
 * It standardizes also the path separator
 */
public class FsConnectionResourcePath extends ConnectionResourcePathAbs {


  /**
   * The path separator of the file system
   */
  private static String pathSeparator;

  /**
   * Root path
   * In a nio format
   */
  private Path nioPathRoot;

  /**
   * The working path
   */
  private Path workingPath;

  /**
   * Don't use this constructor but {@link #createOf(Path, String, String...) the create function}
   */
  private FsConnectionResourcePath(Path workingPath, String stringPath) {
    super(stringPath);
    this.workingPath = workingPath;
    this.setPathSeparator(workingPath.getFileSystem().getSeparator());
    this.setCurrentPathName(".");
    updatePathRoot();
  }


  public static FsConnectionResourcePath createOf(Path path, String name, String... names) {

    String stringPath = name;
    if (names.length != 0) {
      String[] pathNames = Arrayss.concat(name, names);
      String separator = path.getFileSystem().getSeparator();
      stringPath = String.join(separator, pathNames);
    } else {
      stringPath = FsConnectionResourcePath.toSystemPath(path.getFileSystem(), stringPath);
    }
    return new FsConnectionResourcePath(path, stringPath);
  }

  public static String toSystemPath(FileSystem fileSystem, String stringPath) {
    /**
     * Make the path file system independent. ie the path separator:
     *   * / becomes \ on windows
     *   * \ becomes / on linux
     */
    pathSeparator = fileSystem.getSeparator();
    switch (pathSeparator) {
      case "/":
        stringPath = stringPath.replace("\\", "/");
        break;
      case "\\":
        stringPath = stringPath.replace("/", "\\");
        break;
      default:
        throw new RuntimeException("The file system (" + fileSystem + ") has an unknown separator");
    }
    return stringPath;
  }

  public static FsConnectionResourcePath createOf(String name, String... names) {
    return createOf(Paths.get("."), name, names);
  }

  protected static String toTabliPath(String resourcePath) {
    return resourcePath.replace("\\", "/");
  }


  /**
   * Calculate the root of the glob path
   */
  public FsConnectionResourcePath updatePathRoot() {

    this.nioPathRoot = StreamSupport
      .stream(workingPath.getFileSystem().getRootDirectories().spliterator(), false)
      .filter(rootDir -> this.resourcePath.startsWith(rootDir.toString()))
      .findFirst()
      .orElse(null);
    if (this.nioPathRoot != null) {
      this.setRootPath(this.nioPathRoot.toString());
    }
    return this;
  }


  /**
   * Return an absolute glob path  from the current path
   * <p>
   * The path is normalized (ie without . or ..)
   */
  public FsConnectionResourcePath toAbsolute() {


    if (!isAbsolute()) {

      /**
       * Make the glob expression absolute and normalize
       */

      String processedStringPath = this.resourcePath;
      workingPath = workingPath.toAbsolutePath().normalize();

      while (processedStringPath.startsWith(this.getParentPathName()) || processedStringPath.startsWith(this.getCurrentPathName())) {
        // Working dir should be first because it has generally two times the current working dir character
        if (processedStringPath.startsWith(this.getParentPathName())) {
          processedStringPath = processedStringPath.substring(this.getParentPathName().length());
          workingPath = workingPath.getParent();
        }
        if (processedStringPath.startsWith(this.getCurrentPathName())) {
          processedStringPath = processedStringPath.substring(this.getCurrentPathName().length());
        }
        /**
         * In case of ./ or ../, the / stay
         */
        if (processedStringPath.startsWith(this.getPathSeparator())) {
          processedStringPath = processedStringPath.substring(this.getPathSeparator().length());
        }
      }
      processedStringPath = workingPath.toString() + this.getPathSeparator() + processedStringPath;
      return FsConnectionResourcePath.createOf(workingPath, processedStringPath);
    } else {
      return this;
    }


  }


  /**
   * The path root (used in the select processing of files)
   */
  public Path getPathRoot() {
    return this.nioPathRoot;
  }

  /**
   * @return if the path is absolute
   */
  public Boolean isAbsolute() {

    return this.getPathRoot() != null;

  }

  @Override
  public FsConnectionResourcePath normalize() {
    String prefixWorkingDir = this.getCurrentPathName() + this.getPathSeparator();
    if (this.resourcePath.startsWith(prefixWorkingDir)) {
      return FsConnectionResourcePath.createOf(this.workingPath, this.resourcePath.substring(prefixWorkingDir.length()));
    } else {
      return this;
    }
  }

  @Override
  public Glob toGlobExpression() {

    return Glob.createOf(escapeStringPath(resourcePath));

  }

  @Override
  public String replace(String sourcePath, String targetPath) {
    String backReference = escapeStringPath(targetPath);
    return super.replace(sourcePath, backReference);
  }

  /**
   * \ is an escape character for glob,
   * but we want it in the case of a path
   * We escape it then
   */
  private String escapeStringPath(String targetPath) {
    String escapedPath = targetPath;
    if (pathSeparator.equals("\\")) {
      escapedPath = escapedPath.replace("\\", "\\\\");
    }
    return escapedPath;
  }

  /**
   * The entry of file path in tabular is always with a /
   */
  @Override
  public FsConnectionResourcePath standardize() {
    return FsConnectionResourcePath.createOf(workingPath, toTabliPath(this.resourcePath));
  }

}
