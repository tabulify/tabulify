package net.bytle.niofs.http;

import net.bytle.type.Base64Utility;
import net.bytle.type.Casts;

import java.io.IOException;
import java.net.URL;
import java.nio.file.*;
import java.nio.file.attribute.UserPrincipalLookupService;
import java.nio.file.spi.FileSystemProvider;
import java.util.*;

final class HttpFileSystem extends FileSystem {


  static final String USER_AGENT = "Bytle NioFs Http";
  private final HttpFileSystemProvider provider;
  private final URL url;
  @SuppressWarnings({"FieldCanBeLocal", "MismatchedQueryAndUpdateOfCollection"})
  private final Map<String, Object> envs = new HashMap<>();
  private Object user;

  private Object password;

  public HttpFileSystem(HttpFileSystemProvider provider, URL url, final Map<String, ?> envs) {

    this.provider = provider;
    this.url = url;

    if (envs != null) {
      for (Map.Entry<String, ?> env : envs.entrySet()) {

        String attribute = env.getKey();
        Object value = env.getValue();

        HttpRequestAttribute attributeObject;
        try {
          attributeObject = Casts.cast(attribute, HttpRequestAttribute.class);
          switch (attributeObject) {
            case USER:
              this.setUser(value);
              break;
            case PASSWORD:
              this.setPassword(value);
              break;
          }
        } catch (Exception e) {
          // not known
          this.envs.put(attribute, value);
        }

      }
    }


  }

  HttpFileSystem setUser(Object user) {
    this.user = user;
    return this;
  }

  public boolean hasPassword() {
    return this.password != null;
  }

  public String getBasicAuthenticationString() {
    Objects.requireNonNull(this.user, "User should be provided for basic authentication string");
    Objects.requireNonNull(this.password, "Password should be provided for basic authentication string");
    return Base64Utility.stringToBase64UrlString(this.user + ":" + this.password);
  }


  HttpFileSystem setPassword(Object password) {
    this.password = password;
    return this;
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

    HttpRequestPath rootPath = getPath(this.getSeparator());
    return Collections.singletonList(rootPath);

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
  public HttpRequestPath getPath(final String first, final String... more) {

    String path = first;
    if (more.length > 0) {
      path += getSeparator() + String.join(getSeparator(), more);
    }
    return new HttpRequestPath(this, path);

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
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    HttpFileSystem that = (HttpFileSystem) o;
    return url.equals(that.url);
  }

  @Override
  public int hashCode() {
    return Objects.hash(url);
  }

  /**
   * @return return if we check the access (readability, existence) with the `HEAD` method
   */
  public boolean shouldCheckAccess() {
    /**
     * No, by default because `HEAD` has a side effect in the fact that it may be
     * just not authorized (405) while a `GET` will
     *
     */
    return false;
  }

  public String getWorkingStringPath() {
    String path = this.url.getPath();
    if (path.equals("")) {
      return this.getRootCharacter();
    }
    return path;
  }

  public URL getConnectionUrl() {
    return this.url;
  }

  public String getRootCharacter() {
    return this.getSeparator();
  }

}
