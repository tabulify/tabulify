package com.tabulify.memory;

import com.tabulify.spi.DataPathAbs;
import com.tabulify.spi.SchemaType;
import com.tabulify.stream.InsertStream;
import net.bytle.type.MediaType;

import java.util.Arrays;
import java.util.List;

public abstract class MemoryDataPathAbs extends DataPathAbs implements MemoryDataPath {


  private final String path;

  public MemoryDataPathAbs(MemoryConnection memoryConnection, String path, MediaType mediaType) {
    super(memoryConnection, path, null, mediaType);
    this.path = path;
  }


  @Override
  public MemoryConnection getConnection() {
    return (MemoryConnection) super.getConnection();
  }


  @Override
  public String getName() {
    List<String> names = getNames();
    if (names.isEmpty() && this.path.equals("/")) {
      return "";
    }
    return names.get(names.size() - 1);
  }

  @Override
  public List<String> getNames() {

    return Arrays.asList(this.path.split(PATH_SEPARATOR));
  }

  @Override
  public String getCompactPath() {

    return path;

  }

  @Override
  public String getAbsolutePath() {

    return this.getCompactPath();

  }


  @Override
  public MemoryDataPath getSibling(String name) {

    int i = this.path.lastIndexOf(PATH_SEPARATOR);
    String calculatedPath;
    if (i == -1) {
      calculatedPath = name;
    } else {
      calculatedPath = this.path.substring(0, i) + PATH_SEPARATOR + name;
    }
    return this.getConnection().getTypedDataPath(getMediaType(), calculatedPath);

  }

  @Override
  public MemoryDataPath resolve(String name, MediaType mediaType) {

    MemoryDataPath currentDataPath = this.getConnection().getCurrentDataPath();
    if (this.path.equals(currentDataPath.getName())) {
      return this.getConnection().getDataPath(name);
    }

    return this.getConnection().getTypedDataPath(getMediaType(), this.path + PATH_SEPARATOR + name);

  }

  @Override
  public MemoryDataPath resolve(String name) {

    return resolve(name, mediaType);

  }



  @Override
  public MemoryDataPath setContent(String text) {
    this.getOrCreateRelationDef().addColumn("lines");
    try (InsertStream insertStream = this.getInsertStream()) {
      insertStream.insert(text);
    }
    return this;
  }

  @Override
  public MemoryDataPath setLogicalName(String logicalName) {
    return (MemoryDataPath) super.setLogicalName(logicalName);
  }

  @Override
  public SchemaType getSchemaType() {
    return SchemaType.STRICT;
  }
}
