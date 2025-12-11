package com.tabulify.niofs.http;

import java.io.IOException;
import java.nio.file.FileStore;
import java.nio.file.attribute.FileAttributeView;
import java.nio.file.attribute.FileStoreAttributeView;

public class HttpFileStore extends FileStore {

  @Override
  public String name() {
    return "HttpFileStore";
  }

  @Override
  public String type() {
    return "http";
  }

  @Override
  public boolean isReadOnly() {
    return false;
  }

  @Override
  public long getTotalSpace() {
    return 0;
  }

  @Override
  public long getUsableSpace() throws IOException {
    return 0;
  }

  @Override
  public long getUnallocatedSpace() throws IOException {
    return 0;
  }

  @Override
  public boolean supportsFileAttributeView(Class<? extends FileAttributeView> type) {
    return false;
  }

  @Override
  public boolean supportsFileAttributeView(String name) {
    return false;
  }

  @Override
  public <V extends FileStoreAttributeView> V getFileStoreAttributeView(Class<V> type) {
    return null;
  }

  @Override
  public Object getAttribute(String attribute) throws IOException {
    return null;
  }

}
