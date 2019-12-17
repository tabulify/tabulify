package net.bytle.niofs.http;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.nio.file.*;
import java.nio.file.attribute.UserPrincipalLookupService;
import java.nio.file.spi.FileSystemProvider;
import java.util.Map;
import java.util.Set;

final class HttpFileSystem extends FileSystem {


  static final String USER_AGENT = "Bytle NioFs Http";
  private final HttpFileSystemProvider provider;
  private final URL url;
  private final Map<String, ?> env;

  public HttpFileSystem(HttpFileSystemProvider provider, URL url, final Map<String, ?> env) {
    this.provider = provider;
    this.url = url;
    this.env = env;
  }

  @Override
  public FileSystemProvider provider() {
    return provider;
  }


  @Override
  public void close() {

  }


  @Override
  public boolean isOpen() {
    return true;
  }


  @Override
  public boolean isReadOnly() {
    return true;
  }


  @Override
  public String getSeparator() {
    return "/";
  }

  @Override
  public Iterable<Path> getRootDirectories() {
    throw new UnsupportedOperationException("Not implemented");
  }

  @Override
  public Iterable<FileStore> getFileStores() {
    throw new UnsupportedOperationException("Not implemented");
  }

  @Override
  public Set<String> supportedFileAttributeViews() {
    throw new UnsupportedOperationException("Not implemented");
  }

  @Override
  public HttpPath getPath(final String first, final String... more) {
    throw new UnsupportedOperationException("Not implemented");
  }



  HttpPath getPath(final URI uri) {
    try {
      return new HttpPath(this, uri.toURL());
    } catch (MalformedURLException e) {
      throw new RuntimeException();
    }
  }

  @Override
  public PathMatcher getPathMatcher(final String syntaxAndPattern) {
    throw new UnsupportedOperationException("Not implemented");
  }

  @Override
  public UserPrincipalLookupService getUserPrincipalLookupService() {
    throw new UnsupportedOperationException("Not implemented");
  }

  @Override
  public WatchService newWatchService() throws IOException {
    throw new UnsupportedOperationException("Not implemented");
  }

  @Override
  public String toString() {
    return String.format("%s[%s]@%s", this.getClass().getSimpleName(), provider, hashCode());
  }

  @Override
  public boolean equals(Object other) {
    throw new UnsupportedOperationException("Not implemented");
  }

  @Override
  public int hashCode() {
    throw new UnsupportedOperationException("Not implemented");
  }
}
