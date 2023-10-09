package net.bytle.niofs.http;

import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;

public class HttpBasicFileAttributes implements BasicFileAttributes {
  private final HttpRequestPath path;

  public HttpBasicFileAttributes(HttpRequestPath httpPath) {
    this.path = httpPath;
  }

  @Override
  public FileTime lastModifiedTime() {
    return null;
  }

  @Override
  public FileTime lastAccessTime() {
    return null;
  }

  @Override
  public FileTime creationTime() {
    return null;
  }

  /**
   * HTTP does not have the notion of directory/file
   *
   * @return always true
   */
  @Override
  public boolean isRegularFile() {
    return true;
  }

  /**
   * HTTP does not have the notion of directory
   * This attribute determine if the result of Fs.copy will be a directory or a file
   * If true, it will create a directory and not a file.
   *
   * @return always false
   */
  @Override
  public boolean isDirectory() {
    return false;
  }

  @Override
  public boolean isSymbolicLink() {
    return false;
  }

  @Override
  public boolean isOther() {
    return false;
  }

  @Override
  public long size() {

    Long size = HttpStatic.getSizeWithHeadRequest(path);
    if (size==null){
      size = -1L;
    }
    return size;

  }

  @Override
  public Object fileKey() {
    return null;
  }
}
