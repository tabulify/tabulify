package com.tabulify.niofs.http;

import com.tabulify.exception.IllegalArgumentExceptions;
import com.tabulify.exception.IllegalStructure;
import com.tabulify.type.Casts;
import com.tabulify.type.UriEnhanced;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.*;
import java.util.*;

/**
 * The path (ie a URL wrapper) that has all request information
 * in the attributes
 */
public class HttpPath implements Path {

  /**
   * When creating a name for the fonction {@link #getFileName()}
   * A relative path (otherwise url is not empty)
   */

  private final HttpFileSystem httpFileSystem;

  private final Map<String, String> attributes = new HashMap<>();
  private final String pathString;
  private final List<String> names;
  private final String query;


  public HttpPath(HttpFileSystem httpFileSystem, String pathString, String query) {
    this.httpFileSystem = httpFileSystem;
    this.pathString = pathString;
    this.query = query;
    String pathStringToSplit = pathString;

    if (this.isAbsolute()) {
      pathStringToSplit = pathString.substring(1);
    }
    this.names = Arrays.asList(pathStringToSplit.split(httpFileSystem.getSeparator()));

  }

  @Override
  public HttpFileSystem getFileSystem() {
    return this.httpFileSystem;
  }

  @Override
  public boolean isAbsolute() {
    return this.pathString.startsWith(this.httpFileSystem.getSeparator());
  }

  @Override
  public Path getRoot() {

    if (!this.isAbsolute()) {
      return null;
    }

    return new HttpPath(httpFileSystem, httpFileSystem.getRootCharacter(), this.query);

  }

  @Override
  public Path getFileName() {

    // root ?
    if (this.pathString.equals(httpFileSystem.getRootCharacter())) {
      return null;
    }

    int i = this.pathString.lastIndexOf(httpFileSystem.getSeparator());
    if (i == -1) {
      // not an absolute, nor path names
      // this is a file name
      return this;
    }

    // directory (separator is the last character)
    if (i == this.pathString.length()) {
      String stringWithoutSeparatorAtTheEnd = this.pathString.substring(0, i);
      int j = stringWithoutSeparatorAtTheEnd.lastIndexOf(httpFileSystem.getSeparator());
      if (j == -1) {
        return new HttpPath(this.getFileSystem(), stringWithoutSeparatorAtTheEnd, this.query);
      }
      String fileName = stringWithoutSeparatorAtTheEnd.substring(j + 1);
      return new HttpPath(this.getFileSystem(), fileName, this.query);
    }

    // file
    String fileName = this.pathString.substring(i + 1);
    return new HttpPath(this.getFileSystem(), fileName, this.query);
  }

  @Override
  public Path getParent() {

    // root
    if (
      this.pathString.equals(httpFileSystem.getRootCharacter())
        || this.pathString.isEmpty()
    ) {
      return null;
    }

    // Get the parent names
    List<String> parentNames = this.getParentNames();

    // get the names without the last one
    String parentString = String.join(httpFileSystem.getSeparator(), parentNames);

    // add the root absolute character (if the path is an absolute path)
    if (this.isAbsolute()) {
      parentString = httpFileSystem.getRootCharacter() + parentString;
    }

    return new HttpPath(this.getFileSystem(), parentString, this.query);

  }


  @Override
  public int getNameCount() {
    return this.names.size();
  }

  @Override
  public Path getName(int index) {
    return new HttpPath(httpFileSystem, this.names.get(index), this.query);
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
    return this;
  }

  @Override
  public Path resolve(Path other) {

    // as defined in the description of the resolve function
    if (other.isAbsolute()) {
      return other;
    }

    // as defined in the description of the resolve function
    if (other.toString().isEmpty()) {
      return this;
    }

    return resolve(other.toString());
  }

  @Override
  public Path resolve(String other) {

    // absolute path
    if (other.startsWith(httpFileSystem.getRootCharacter())) {
      return new HttpPath(httpFileSystem, other, this.query);
    }

    // the actual object is considered a directory path
    String newPath = this.pathString;
    if (!this.pathString.isEmpty() && !newPath.endsWith(httpFileSystem.getSeparator())) {
      newPath += httpFileSystem.getSeparator();
    }
    newPath += other; // other is relative

    return new HttpPath(httpFileSystem, newPath, this.query);

  }

  @Override
  public Path resolveSibling(Path other) {
    return resolveSibling(other.getFileName().toString());
  }

  @Override
  public Path resolveSibling(String other) {

    // Get the correct sibling names
    List<String> parentNames = getParentNames();
    parentNames.add(other);
    String siblingPath = String.join(httpFileSystem.getSeparator(), parentNames);

    // absolute path ?
    if (this.isAbsolute()) {
      siblingPath = httpFileSystem.getRootCharacter() + siblingPath;
    }
    return new HttpPath(this.getFileSystem(), siblingPath, this.query);
  }

  private List<String> getParentNames() {
    List<String> strings = this.names.subList(0, this.names.size() - 1);
    // we wrap it to add the `add` operation
    return new ArrayList<>(strings);
  }

  /**
   *
   * @param other
   *          the path to relativize against this path
   */
  @Override
  public HttpPath relativize(Path other) {

    /**
     * ! The other path may be a unix path, not a HTTP path
     */
    if (!this.isAbsolute()) {
      throw new IllegalArgumentException("The actual path argument (" + this + ") is not absolute and cannot therefore be used to relativized");
    }
    if (this.equals(other)) {
      return (HttpPath) other;
    }

    String absoluteOther = other.toAbsolutePath().toString();
    if (!absoluteOther.startsWith(this.pathString)) {
      throw new IllegalArgumentException("The other path argument (" + absoluteOther + ") is not a subset of this path (" + this.pathString + ") and cannot be relativized");
    }
    String relative = absoluteOther.substring(pathString.length());
    if (relative.startsWith("/")) {
      relative = relative.substring(1);
    }
    return new HttpPath(this.getFileSystem(), relative, this.query);

  }

  @Override
  public URI toUri() {

    // URI is absolute as defined in the description
    HttpPath absoluteRequestPath = (HttpPath) this.toAbsolutePath();

    // URL Twerks
    String absolutePath = absoluteRequestPath.toString();
    if (absolutePath.equals(this.httpFileSystem.getRootCharacter())) {
      absolutePath = "";
    }
    URL workingUrl = httpFileSystem.getConnectionUrl();

    try {
      UriEnhanced uri = UriEnhanced.create()
        .setScheme(workingUrl.getProtocol())
        .setHost(workingUrl.getHost())
        .setPort(workingUrl.getPort())
        .setPath(absolutePath);
      if (this.query != null) {
        uri.setQueryString(this.query);
      }
      return uri.toUri();
    } catch (IllegalStructure e) {
      throw IllegalArgumentExceptions.createFromException(e);
    }

  }

  @Override
  public Path toAbsolutePath() {
    if (this.isAbsolute()) {
      return this;
    }

    String workingStringPath = this.httpFileSystem.getWorkingStringPath();
    String absolutePath;
    if (workingStringPath.endsWith(this.httpFileSystem.getSeparator())) {
      absolutePath = workingStringPath + this.pathString;
    } else {
      absolutePath = workingStringPath + this.httpFileSystem.getSeparator() + this.pathString;
    }
    return new HttpPath(httpFileSystem, absolutePath, this.query);

  }

  /**
   * We are not taking redirection into account
   *
   * @param options - the options
   * @return the path
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
  public WatchKey register(WatchService watcher, WatchEvent.Kind<?>[] events, WatchEvent.Modifier... modifiers) {
    throw new UnsupportedOperationException("Not implemented");
  }

  @Override
  public WatchKey register(WatchService watcher, WatchEvent.Kind<?>... events) {
    throw new UnsupportedOperationException("Not implemented");
  }

  @Override
  public Iterator<Path> iterator() {
    throw new UnsupportedOperationException("Not implemented");
  }

  @Override
  public int compareTo(Path other) {
    return this.toString().compareTo(other.toString());
  }

  @Override
  public String toString() {

    return this.pathString;

  }

  public void setAttribute(String attribute, Object value) {
    HttpRequestAttribute attributeObject;
    try {
      attributeObject = Casts.cast(attribute, HttpRequestAttribute.class);
      switch (attributeObject) {
        case USER:
          this.getFileSystem().setUser(value.toString());
          break;
        case PASSWORD:
          this.getFileSystem().setPassword(value.toString());
          break;
      }
    } catch (Exception e) {
      this.attributes.put(attribute, value.toString());
    }

  }


  public Object readAttribute(String attribute) {
    return this.attributes.get(attribute);
  }

  public Map<String, String> getAttributes() {
    return this.attributes;
  }


  public String getURLPath() {
    return this.pathString;
  }

  public static HttpPathBuilder builder() {
    return new HttpPathBuilder();
  }

  public String getQuery() {
    return this.query;
  }


  public static class HttpPathBuilder {
    private URI url;
    private String userAgent = HttpHeader.USER_AGENT.getDefaultValue();

    public HttpPathBuilder setUrl(String url) throws MalformedURLException, URISyntaxException {
      this.url = new URL(url).toURI();
      return this;
    }

    public HttpPathBuilder setUserAgent(String userAgent) {
      this.userAgent = userAgent;
      return this;
    }

    public HttpPath build() throws IOException {
      Path sourcePath = Paths.get(url);
      Files.setAttribute(sourcePath, HttpHeader.USER_AGENT.toKeyNormalizer().toHttpHeaderCase(), this.userAgent);
      return (HttpPath) sourcePath;
    }

  }
}
