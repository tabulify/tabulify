package net.bytle.niofs.http;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.*;
import java.util.Iterator;

public class HttpPath implements Path {

  /**
   * When creating a name for the fonction {@link #getFileName()}
   */
  private String fileName;

  HttpFileSystem httpFileSystem;
  URL url;

  public HttpPath(HttpFileSystem httpFileSystem, URL url) {
    this.httpFileSystem = httpFileSystem;
    this.url = url;
  }

  public HttpPath(HttpFileSystem httpFileSystem, String fileName) {
    this.httpFileSystem = httpFileSystem;
    this.url = null;
    this.fileName = fileName;
  }

  @Override
  public HttpFileSystem getFileSystem() {
    return this.httpFileSystem;
  }

  @Override
  public boolean isAbsolute() {
    throw new UnsupportedOperationException("Not implemented");
  }

  @Override
  public Path getRoot() {
    throw new UnsupportedOperationException("Not implemented");
  }

  @Override
  public Path getFileName() {
    String path = url.getPath();
    int i = path.lastIndexOf("/");
    return new HttpPath(this.getFileSystem(),path.substring(i+1));
  }

  @Override
  public Path getParent() {
    throw new UnsupportedOperationException("Not implemented");
  }

  @Override
  public int getNameCount() {
    throw new UnsupportedOperationException("Not implemented");
  }

  @Override
  public Path getName(int index) {
    throw new UnsupportedOperationException("Not implemented");
  }

  @Override
  public Path subpath(int beginIndex, int endIndex) {
    throw new UnsupportedOperationException("Not implemented");
  }

  @Override
  public boolean startsWith(Path other) {
    throw new UnsupportedOperationException("Not implemented");
  }

  @Override
  public boolean startsWith(String other) {
    throw new UnsupportedOperationException("Not implemented");
  }

  @Override
  public boolean endsWith(Path other) {
    throw new UnsupportedOperationException("Not implemented");
  }

  @Override
  public boolean endsWith(String other) {
    throw new UnsupportedOperationException("Not implemented");
  }

  @Override
  public Path normalize() {
    throw new UnsupportedOperationException("Not implemented");
  }

  @Override
  public Path resolve(Path other) {
    throw new UnsupportedOperationException("Not implemented");
  }

  @Override
  public Path resolve(String other) {
    try {
      int port = url.getPort();
      if (port == -1){
        port = url.getDefaultPort();
      }
      URI uri = url.toURI();
      String path = uri.getPath()+"/"+other;
      String file = path + "?" + uri.getQuery();
      URL url = new URL(this.url.getProtocol(), this.url.getHost(), port, file);
      return new HttpPath(httpFileSystem, url);
    } catch (MalformedURLException e) {
      throw new RuntimeException(e);
    } catch (URISyntaxException e) {
      throw new RuntimeException(e);
    }

  }

  @Override
  public Path resolveSibling(Path other) {
    throw new UnsupportedOperationException("Not implemented");
  }

  @Override
  public Path resolveSibling(String other) {
    throw new UnsupportedOperationException("Not implemented");
  }

  @Override
  public Path relativize(Path other) {
    throw new UnsupportedOperationException("Not implemented");
  }

  @Override
  public URI toUri() {
    try {
      return url.toURI();
    } catch (URISyntaxException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public Path toAbsolutePath() {
    return this;
  }

  /**
   * We are not taking redirection into account
   * @param options
   * @return
   */
  @Override
  public Path toRealPath(LinkOption... options) {
    return this;
  }

  @Override
  public File toFile() {
    throw new UnsupportedOperationException("Not implemented");
  }

  @Override
  public WatchKey register(WatchService watcher, WatchEvent.Kind<?>[] events, WatchEvent.Modifier... modifiers) throws IOException {
    throw new UnsupportedOperationException("Not implemented");
  }

  @Override
  public WatchKey register(WatchService watcher, WatchEvent.Kind<?>... events) throws IOException {
    throw new UnsupportedOperationException("Not implemented");
  }

  @Override
  public Iterator<Path> iterator() {
    throw new UnsupportedOperationException("Not implemented");
  }

  @Override
  public int compareTo(Path other) {
    return this.url.toString().compareTo(((HttpPath) other).url.toString());
  }

  public URL getUrl() {
    return this.url;
  }

  @Override
  public String toString() {
    if (url !=null) {
      return url.toString();
    } else {
      return fileName;
    }

  }
}
