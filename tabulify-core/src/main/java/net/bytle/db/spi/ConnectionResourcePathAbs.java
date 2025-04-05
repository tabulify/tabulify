package net.bytle.db.spi;

import net.bytle.regexp.Glob;
import net.bytle.type.Strings;

import java.util.ArrayList;
import java.util.List;

/**
 * Represent an {@link ResourcePath} that is aware of a path structure
 * where the separator may be parameterized
 *
 * Note: A file system path cannot have special character such as a star
 */
public abstract class ConnectionResourcePathAbs implements ResourcePath {


  protected final String resourcePath;

  /**
   * Not every path system has a root
   * but if this is the case, it needs to be deleted from the
   * path to get the names. That's why it's here
   *
   * The root name of the glob path if any
   * <p>
   * If this value is not null, this is an absolute glob path
   */
  private String pathRoot;


  /**
   * The special characters
   */
  private String systemSeparator = "/";
  private String currentWorkingDirCharacter = ".";
  private String parentPathName = "..";


  public ConnectionResourcePathAbs(String stringPath) {
    this.resourcePath = stringPath;
  }


  /**
   * The path names does not include the root
   *
   */
  @Override
  public List<String> getNames() {

    if (resourcePath ==null){
      return new ArrayList<>();
    }

    String processedGlob = resourcePath;

    if (this.pathRoot != null) {
      processedGlob = processedGlob.substring(this.pathRoot.length());
    }

    return Strings.createFromString(processedGlob).split(this.getPathSeparator());
  }


  @Override
  public String getCurrentPathName() {
    return this.currentWorkingDirCharacter;
  }

  public String getParentPathName() {
    return this.parentPathName;
  }
  @Override
  public ConnectionResourcePathAbs setParentPathName(String parentWorkingChar) {
    this.parentPathName = parentWorkingChar;
    return this;
  }

  @Override
  public ConnectionResourcePathAbs setCurrentPathName(String workingChar) {
    this.currentWorkingDirCharacter = workingChar;
    return this;
  }

  @Override
  public ConnectionResourcePathAbs setPathSeparator(String separator) {
    this.systemSeparator = separator;
    return this;
  }

  @Override
  public String getPathSeparator() {
    return this.systemSeparator;
  }

  @Override
  public ConnectionResourcePathAbs setRootPath(String root) {
    this.pathRoot = root;
    return this;
  }

  @Override
  public String toString() {
    return resourcePath;
  }

  @Override
  public Glob toGlobExpression() {
    return Glob.createOf(this.resourcePath);
  }

  @Override
  public String replace(String sourcePath, String targetPath) {
    return toGlobExpression().replace(sourcePath,targetPath);
  }

  @Override
  public ResourcePath standardize() {
    return this;
  }
}
