package net.bytle.niofs.http;

import net.bytle.exception.IllegalArgumentExceptions;
import net.bytle.exception.IllegalStructure;
import net.bytle.type.Casts;
import net.bytle.type.UriEnhanced;

import java.io.File;
import java.net.URI;
import java.net.URL;
import java.nio.file.*;
import java.util.*;

/**
 * The path (ie a URL wrapper) that has all request information
 * in the attributes
 */
public class HttpRequestPath implements Path {

  /**
   * When creating a name for the fonction {@link #getFileName()}
   * A relative path (otherwise url is not empty)
   */

  private final HttpFileSystem httpFileSystem;

  private final Map<String, String> attributes = new HashMap<>();
  private final String pathString;
  private final List<String> names;


  public HttpRequestPath(HttpFileSystem httpFileSystem, String pathString) {
    this.httpFileSystem = httpFileSystem;
    this.pathString = pathString;
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

    return new HttpRequestPath(httpFileSystem, httpFileSystem.getRootCharacter());

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
        return new HttpRequestPath(this.getFileSystem(), stringWithoutSeparatorAtTheEnd);
      }
      String fileName = stringWithoutSeparatorAtTheEnd.substring(j + 1);
      return new HttpRequestPath(this.getFileSystem(), fileName);
    }

    // file
    String fileName = this.pathString.substring(i + 1);
    return new HttpRequestPath(this.getFileSystem(), fileName);
  }

  @Override
  public Path getParent() {

    // root
    if (
      this.pathString.equals(httpFileSystem.getRootCharacter())
      || this.pathString.equals("")
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

    return new HttpRequestPath(this.getFileSystem(), parentString);

  }


  @Override
  public int getNameCount() {
    return this.names.size();
  }

  @Override
  public Path getName(int index) {
    return new HttpRequestPath(httpFileSystem, this.names.get(index));
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
    if (other.toString().equals("")) {
      return other;
    }

    return resolve(other.toString());
  }

  @Override
  public Path resolve(String other) {

    // absolute path
    if (other.startsWith(httpFileSystem.getRootCharacter())) {
      return new HttpRequestPath(httpFileSystem, other);
    }

    // the actual object is considered a directory path
    String newPath = this.pathString;
    if (!this.pathString.equals("") && !newPath.endsWith(httpFileSystem.getSeparator())) {
      newPath += httpFileSystem.getSeparator();
    }
    newPath += other; // other is relative

    return new HttpRequestPath(httpFileSystem, newPath);

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
    return new HttpRequestPath(this.getFileSystem(), siblingPath);
  }

  private List<String> getParentNames() {
    List<String> strings = this.names.subList(0, this.names.size() - 1);
    // we wrap it to add the `add` operation
    return new ArrayList<>(strings);
  }

  @Override
  public Path relativize(Path other) {
    if (!other.isAbsolute()) {
      throw new IllegalArgumentException("The path argument (" + other + ") is not absolute and cannot be relativized");
    }
    if (!this.isAbsolute()) {
      throw new IllegalArgumentException("The path argument (" + this + ") is not absolute and cannot therefore be used to relativized");
    }
    if (this.equals(other)) {
      return new HttpRequestPath(this.getFileSystem(), "");
    }
    HttpRequestPath otherHttpPath = (HttpRequestPath) other;
    if (otherHttpPath.toString().startsWith(this.pathString)) {
      String relative = otherHttpPath.toString().substring(this.pathString.length());
      if (relative.startsWith("/")) {
        relative = relative.substring(1);
      }
      return new HttpRequestPath(this.getFileSystem(), relative);
    } else {
      throw new IllegalArgumentException("The other path argument (" + other + ") is not a subset of this path (" + this + ")");
    }
  }

  @Override
  public URI toUri() {

    // URI is absolute as defined in the description
    HttpRequestPath absoluteRequestPath = (HttpRequestPath) this.toAbsolutePath();

    // URL Twerks
    String absolutePath = absoluteRequestPath.toString();
    if (absolutePath.equals(this.httpFileSystem.getRootCharacter())) {
      absolutePath = "";
    }
    URL workingUrl = httpFileSystem.getConnectionUrl();

    try {
      return UriEnhanced.create()
        .setScheme(workingUrl.getProtocol())
        .setHost(workingUrl.getHost())
        .setPort(workingUrl.getPort())
        .setPath(absolutePath)
        .toUri();
    } catch (IllegalStructure e) {
      throw IllegalArgumentExceptions.createFromException(e);
    }

  }

  @Override
  public Path toAbsolutePath() {
    if (this.isAbsolute()) {
      return this;
    } else {
      String workingStringPath = this.httpFileSystem.getWorkingStringPath();
      String absolutePath;
      if (workingStringPath.endsWith(this.httpFileSystem.getSeparator())) {
        absolutePath = workingStringPath + this.pathString;
      } else {
        absolutePath = workingStringPath + this.httpFileSystem.getSeparator() + this.pathString;
      }
      return new HttpRequestPath(httpFileSystem, absolutePath);
    }
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


}
