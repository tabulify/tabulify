package com.tabulify.niofs.http;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.util.concurrent.TimeUnit;

public class HttpBasicFileAttributes implements BasicFileAttributes {
  private final HttpPath path;
  /**
   * -1 if not known
   */
  private long size = -1L;
  /**
   * 0 if not known
   */
  private long lastModifiedTime = 0L;

  public HttpBasicFileAttributes(HttpPath httpPath) {

    this.path = httpPath;
    this.updateAttributes(this.path);

  }

  @Override
  public FileTime lastModifiedTime() {

    if (lastModifiedTime == 0L) {
      return null;
    }
    return FileTime.from(this.lastModifiedTime, TimeUnit.MILLISECONDS);

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


    return size;

  }

  @Override
  public Object fileKey() {
    return null;
  }

  /**
   * Doc: https://www.w3.org/Protocols/rfc2616/rfc2616-sec4.html#sec4.4
   * <p>
   * The Content-Length header is not required.
   * Generated pages don't send it, they instead use things like Transfer-Encoding: chunked
   * Where the end of the response is signaled by a zero size chunk; or by forgoing persistent connections (via Connection: close) and simply closing the connection when the response is fully sent.
   * <p>
   * If the request is for a static file, there's a good chance that Content-Length will be present
   * and you can get the size without downloading the file.
   * <p>
   * But in the general case, the only fully viable way is by actually downloading the full response.
   * <p>
   * See also {@link HttpResponse#getSize()}
   *
   * @return the size
   */
  void updateAttributes(HttpPath path) {

    final HttpURLConnection connection;
    try {
      connection = HttpRequest.getHttpRequest(path);
      connection.setRequestMethod("HEAD");
      connection.setRequestProperty("Accept-Encoding", "identity"); // no compression, nor modification
      connection.connect();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    try {

      // The Content-Length entity-header field indicates the size of the entity-body,
      // in decimal number of OCTETs, sent to the recipient or, in the case of the HEAD
      // method, the size of the entity-body that would have been sent had the request
      // been a GET.
      // -1 if the content-length header is not present in the response.
      this.size = connection.getContentLengthLong();
      this.lastModifiedTime = connection.getLastModified();

    } finally {
      connection.disconnect();
    }

  }
}
