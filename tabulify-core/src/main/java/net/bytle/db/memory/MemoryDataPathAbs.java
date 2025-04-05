package net.bytle.db.memory;

import net.bytle.db.spi.DataPath;
import net.bytle.db.spi.DataPathAbs;
import net.bytle.db.stream.InsertStream;
import net.bytle.type.MediaType;

import java.util.Arrays;
import java.util.List;

public abstract class MemoryDataPathAbs extends DataPathAbs implements MemoryDataPath {


  private final String path;

  public MemoryDataPathAbs(MemoryConnection memoryConnection, String path, MediaType mediaType) {
    super(memoryConnection, path, mediaType);
    this.path = path;
  }


  @Override
  public MemoryConnection getConnection() {
    return (MemoryConnection) super.getConnection();
  }


  @Override
  public String getName() {
    return getNames().get(getNames().size() - 1);
  }


  @Override
  public List<String> getNames() {

    return Arrays.asList(this.path.split(PATH_SEPARATOR));
  }

  @Override
  public String getRelativePath() {

    return path;

  }

  @Override
  public String getAbsolutePath() {

    return this.getRelativePath();

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
  public MemoryDataPath getChild(String name) {

    if (this.path.equals(this.getConnection().getCurrentDataPath().getName())) {
      return this.getConnection().getDataPath(name);
    } else {
      return this.getConnection().getTypedDataPath(getMediaType(), this.path + PATH_SEPARATOR + name);
    }

  }

  @Override
  public MemoryDataPath resolve(String name) {

    return getChild(name);

  }


  @Override
  public DataPath getChildAsTabular(String name) {
    return getChild(name);
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

}
