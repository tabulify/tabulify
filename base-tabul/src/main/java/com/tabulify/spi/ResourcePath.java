package com.tabulify.spi;

import com.tabulify.glob.Glob;

import java.nio.file.PathMatcher;
import java.util.List;

/**
 * The function below are the one that needs to be implemented
 * for the path system
 * <p>
 * To create a string path, you just needs to extends {@link ConnectionResourcePathAbs}
 * <p>
 * There is only three function that returns a StringPath and they needs to be implemented
 * by the path system ie
 * {@link #toAbsolute()}
 * {@link #isAbsolute()}
 * {@link #normalize()}
 * <p>
 * A string path is needed because a {@link java.nio.file.Path Path} cannot contains {@link com.tabulify.glob.Glob}
 *
 * TODO: We can delete this class with:
 *   * all path operation in {@link DataPath}
 *   * all match glob / operation in Path Matcher to support glob, regexp (ie the same NIO notion of {@link PathMatcher}
 *   build with {@link java.nio.file.FileSystem#getPathMatcher(String)}}
 */
public interface ResourcePath {

  /**
   * @return a absolute glob. One that is unique.
   */
  ResourcePath toAbsolute();


  /**
   * @return true if the glob is absolute
   */
  Boolean isAbsolute();


  /**
   * Delete the relative characters
   *
   * @return a string path without relative characters
   */
  ResourcePath normalize();


  /**
   * @return the path names without the root
   */
  List<String> getNames();

  /**
   * @return The working char (ie `.` for a file system)
   */
  String getCurrentPathName();

  /**
   * @return the parent working char (ie `..` for a file system)
   */
  String getParentPathName();

  /**
   * @param parentWorkingChar -  the parent working char (ie `..` for a file system)
   */
  ResourcePath setParentPathName(String parentWorkingChar);

  /**
   * @param workingChar - the working char (ie `.` for a file system)
   * @return the glob path object
   */
  ResourcePath setCurrentPathName(String workingChar);

  /**
   * @param separator - the path separator (windows: \, Linux: /, Sql: ., ...)
   */
  ResourcePath setPathSeparator(String separator);

  /**
   * @return the path separator (windows: \, Linux: /, Sql: ., ...)
   */
  String getPathSeparator();

  /**
   * If the path is an absolute path and therefore has a root, it's important
   * to set it with this function in order
   * to return names without the root
   */
  ResourcePath setRootPath(String root);

  /**
   * @return a glob expression aware of the path character
   * <p>
   * Because on Windows, the path `\` is path separator but
   * this is also the escape regexp character
   * This function should escape it (ie every `\` would become `\\`)
   */
  Glob toGlobExpression();

  /**
   * A replace that is aware of the fact that the target path can have
   * path separator that must be escaped
   * <p>
   * ie on Windows, the path `\` is path separator but
   * * this is also the escape regexp character
   * This function should escape it (ie every `\` would become `\\`)
   * before every replacement
   */
  String replace(String sourcePath, String targetPath);

  /**
   * Make the path the same on all datastore
   * Used formerly to transform a file path separator to /
   * on Windows and Linux
   *
   */
  ResourcePath standardize();


}
