package net.bytle.niofs.http;

import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;

public class HttpBasicFileAttributes implements BasicFileAttributes {
  private final HttpPath path;

  public HttpBasicFileAttributes(HttpPath httpPath) {
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

    @Override
    public boolean isRegularFile() {
        return false;
    }

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

        return HttpStatic.getSize(path.getUrl());

    }

    @Override
    public Object fileKey() {
        return null;
    }
}
