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

  HttpFileSystem httpFileSystem;
  URL url;

  public HttpPath(HttpFileSystem httpFileSystem, URL url) {
    this.httpFileSystem = httpFileSystem;
    this.url = url;
  }

  @Override
  public FileSystem getFileSystem() {
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
    throw new UnsupportedOperationException("Not implemented");
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

  @Override
  public Path toRealPath(LinkOption... options) throws IOException {
    throw new UnsupportedOperationException("Not implemented");
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
    return url.toString();
  }
}
