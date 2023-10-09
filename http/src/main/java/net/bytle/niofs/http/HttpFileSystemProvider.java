package net.bytle.niofs.http;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.FileAttributeView;
import java.nio.file.spi.FileSystemProvider;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;


public class HttpFileSystemProvider extends FileSystemProvider {


  private static final Map<URI, HttpFileSystem> httpFileSystems = new HashMap<>();


  @Override
  public String getScheme() {
    return "http";
  }


  @Override
  public final HttpFileSystem newFileSystem(final URI uri, final Map<String, ?> env) {

    try {
      HttpFileSystem httpFileSystem = httpFileSystems.get(uri);
      if (httpFileSystem == null) {
        httpFileSystem = new HttpFileSystem(this, uri.toURL(), env);
        httpFileSystems.put(uri, httpFileSystem);
      }
      return httpFileSystem;
    } catch (MalformedURLException e) {
      throw new RuntimeException(e);
    }

  }

  @Override
  public final HttpFileSystem getFileSystem(final URI uri) {
    return httpFileSystems.get(uri);
  }


  @Override
  public final HttpRequestPath getPath(final URI uri) {

    /**
     * This function may be called directly via {@link java.nio.file.Paths}
     * the file system was then may be not created
     */
    HttpFileSystem fileSystem = getFileSystem(uri);
    if (fileSystem == null) {
      fileSystem = newFileSystem(uri, null);
    }

    return fileSystem.getPath(uri.getPath());


  }

  @Override
  public final SeekableByteChannel newByteChannel(final Path path,
                                                  final Set<? extends OpenOption> options, final FileAttribute<?>... attrs) {


    if (options.isEmpty() ||
      (options.size() == 1 && options.contains(StandardOpenOption.READ))) {
      return new HttpSeekableByteChannel((HttpRequestPath) path);
    } else {
      throw new UnsupportedOperationException(
        String.format("Only %s is supported for %s, but %s options(s) are provided",
          StandardOpenOption.READ, this, options));
    }
  }

  @Override
  public final DirectoryStream<Path> newDirectoryStream(final Path dir,
                                                        final DirectoryStream.Filter<? super Path> filter) throws IOException {
    throw new UnsupportedOperationException("HTTP does not support the notion of directory");
  }

  /**
   * Unsupported method.
   */
  @Override
  public final void createDirectory(final Path dir, final FileAttribute<?>... attrs) {
    throw new UnsupportedOperationException("Not implemented");
  }

  /**
   * Unsupported method.
   */
  @Override
  public final void delete(final Path path) {
    throw new UnsupportedOperationException("You can't delete a HTTP file. If you are using a `move` transfer, use the `copy` one instead.");
  }

  @Override
  public final void copy(final Path source, final Path target, CopyOption... options) {
    throw new UnsupportedOperationException("Not implemented");
  }

  /**
   * Unsupported method.
   */
  @Override
  public final void move(final Path source, final Path target, final CopyOption... options) {
    throw new UnsupportedOperationException("Not implemented");
  }

  @Override
  public final boolean isSameFile(final Path path, final Path path2) throws IOException {
    throw new UnsupportedOperationException("Not implemented");
  }

  @Override
  public final boolean isHidden(final Path path) {
    throw new UnsupportedOperationException("Not implemented");
  }

  @Override
  public final FileStore getFileStore(final Path path) {
    return new HttpFileStore();
  }

  /**
   * check access is meaning less in HTTP.
   * <p>
   * Why ?
   * A `HEAD` method may be not authorized (405)
   * while a `GET` is
   *
   * @param path  - the path to check
   * @param modes - the access modes
   * @throws IOException - if the file is not accessible or does not exists
   */
  @Override
  public final void checkAccess(final Path path, final AccessMode... modes) throws IOException {

    if (!((HttpRequestPath) path).getFileSystem().shouldCheckAccess()) {
      return;
    }

    if (modes.length == 0) {

      // check the existence of the file
      HttpRequestPath httpPath = (HttpRequestPath) path;
      HttpURLConnection fetch = HttpStatic.getHttpFetchObject(httpPath);
      fetch.setRequestMethod("HEAD");
      fetch.connect();
      int responseCode = fetch.getResponseCode();
      if (responseCode != HttpURLConnection.HTTP_OK) {
        // if (Arrays.asList(HttpURLConnection.HTTP_NOT_FOUND, HttpURLConnection.HTTP_UNAUTHORIZED).contains(responseCode)) {
        /**
         * For the {@link Files#exists(Path, LinkOption...)}
         * you need to throw a IO exception
         *
         * For the {@link Files#notExists(Path, LinkOption...)}
         * you need to throw a IO exception (not
         */
        throw new NoSuchFileException(httpPath + ". The http request was not successful, we got the following response code: " + responseCode);

      }

    } else {
      // Read
      if (modes.length == 1 && modes[0] == AccessMode.READ) {

        // check the existence of the file
        HttpRequestPath httpPath = (HttpRequestPath) path;
        HttpURLConnection connection = HttpStatic.getHttpFetchObject(httpPath);
        connection.setRequestMethod("HEAD");
        connection.connect();
        int responseCode = connection.getResponseCode();
        if (responseCode != HttpURLConnection.HTTP_OK) {
          /**
           * IO Exception is needed for {@link Files#isAccessible(Path, AccessMode...)}
           */
          throw new IOException("No read permission. The http request was not successful, we got the following response code " + responseCode);
        }

      } else {
        // A IOException is caught by the isAccessible function
        throw new IOException("Http cannot handle the following modes " + Arrays.stream(modes).map(Enum::toString).collect(Collectors.joining(",")));
      }
    }

  }

  @Override
  public final <V extends FileAttributeView> V getFileAttributeView(final Path path,
                                                                    final Class<V> type, final LinkOption... options) {
    throw new UnsupportedOperationException("Not implemented");
  }

  @Override
  public final <A extends BasicFileAttributes> A readAttributes(final Path path,
                                                                final Class<A> type, final LinkOption... options) throws IOException {
    //noinspection unchecked
    return (A) new HttpBasicFileAttributes((HttpRequestPath) path);
  }

  @Override
  public final Map<String, Object> readAttributes(final Path path, final String attributes,
                                                  final LinkOption... options) throws IOException {
    if (options.length != 0) {
      throw new UnsupportedOperationException("Link options are not implemented");
    }
    Map<String, Object> values = new HashMap<>();
    for (String attribute : attributes.split(",")) {
      Object value = ((HttpRequestPath) path).readAttribute(attribute);
      /**
       * {@link Files#getAttribute(Path, String, LinkOption...)} }
       * is checking only the attribute without the namespace strange
       */
      String attributeWithoutNamespace = this.getAttributeWithoutNamespace(attribute);
      values.put(attributeWithoutNamespace, value);
    }
    return values;
  }

  /**
   * @param attribute - the attribute to get
   * @return the attribute without the namespace
   */
  private String getAttributeWithoutNamespace(String attribute) {
    int pos = attribute.indexOf(':');
    String name;
    if (pos == -1) {
      name = attribute;
    } else {
      name = (pos == attribute.length()) ? "" : attribute.substring(pos + 1);
    }
    return name;
  }

  @Override
  public final void setAttribute(final Path path, final String attribute, final Object value,
                                 final LinkOption... options) throws IOException {
    if (options == null) {
      throw new UnsupportedOperationException(this.getClass().getName() +
        " is read-only: cannot set attributes to paths");
    }
    ((HttpRequestPath) path).setAttribute(attribute, value);
  }

  @Override
  public String toString() {
    return this.getClass().getSimpleName();
  }
}
