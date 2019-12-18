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
import java.util.HashMap;
import java.util.Map;
import java.util.Set;


public class HttpFileSystemProvider extends FileSystemProvider {


  private Map<URI, HttpFileSystem> httpFileSystems = new HashMap<>();

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
  public final HttpPath getPath(final URI uri) {
    /**
     * This function may be called directly via {@link java.nio.file.Paths#get(URI)}
     * the file system was then may be not created
      */

    HttpFileSystem fileSystem = getFileSystem(uri);
    if (fileSystem==null){
      fileSystem = newFileSystem(uri, null);
    }
    return fileSystem.getPath(uri);


  }

  @Override
  public final SeekableByteChannel newByteChannel(final Path path,
                                                  final Set<? extends OpenOption> options, final FileAttribute<?>... attrs) {


    if (options.isEmpty() ||
      (options.size() == 1 && options.contains(StandardOpenOption.READ))) {
      return new HttpSeekableByteChannel((HttpPath) path);
    } else {
      throw new UnsupportedOperationException(
        String.format("Only %s is supported for %s, but %s options(s) are provided",
          StandardOpenOption.READ, this, options));
    }
  }

  @Override
  public final DirectoryStream<Path> newDirectoryStream(final Path dir,
                                                        final DirectoryStream.Filter<? super Path> filter) throws IOException {
    throw new UnsupportedOperationException("Not implemented");
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
    throw new UnsupportedOperationException("Not implemented");
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
    throw new UnsupportedOperationException("Not implemented");
  }

  @Override
  public final void checkAccess(final Path path, final AccessMode... modes) {

    if (modes.length==0){
      try {
        // check the existence of the file
        HttpPath httpPath = (HttpPath) path;
        HttpURLConnection connection = HttpStatic.getConnection(httpPath.getUrl());
        connection.setRequestMethod("HEAD");
        connection.connect();
        int responseCode = connection.getResponseCode();
        if (responseCode !=HttpURLConnection.HTTP_OK){
          if (responseCode==HttpURLConnection.HTTP_NOT_FOUND) {
            throw new NoSuchFileException(httpPath.toString());
          } else {
            throw new RuntimeException("The http request was not successful, we got the following response code "+responseCode);
          }
        }
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    } else {
      throw new UnsupportedOperationException("Modes access check is not yet implemented");
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
    return (A) new HttpBasicFileAttributes((HttpPath) path);
  }

  @Override
  public final Map<String, Object> readAttributes(final Path path, final String attributes,
                                                  final LinkOption... options) throws IOException {
    throw new UnsupportedOperationException("Not implemented");
  }

  @Override
  public final void setAttribute(final Path path, final String attribute, final Object value,
                                 final LinkOption... options) throws IOException {
    throw new UnsupportedOperationException(this.getClass().getName() +
      " is read-only: cannot set attributes to paths");
  }

  @Override
  public String toString() {
    return this.getClass().getSimpleName();
  }
}
