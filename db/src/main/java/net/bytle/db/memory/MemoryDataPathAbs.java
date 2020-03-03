package net.bytle.db.memory;

import net.bytle.db.spi.DataPath;
import net.bytle.db.spi.DataPathAbs;
import net.bytle.db.uri.DataUri;

import java.util.Arrays;
import java.util.List;

public abstract class MemoryDataPathAbs extends DataPathAbs implements MemoryDataPath {


  private final MemoryDataStore memoryDataStore;

  private String path;

  public MemoryDataPathAbs(MemoryDataStore memoryDataStore, String path) {
    this.memoryDataStore = memoryDataStore;
    this.path = path;
  }


  @Override
  public MemoryDataStore getDataStore() {
    return memoryDataStore;
  }


  @Override
  public String getName() {
    return getNames().get(getNames().size()-1);
  }

  @Override
  public List<String> getNames() {

    return Arrays.asList(this.path.split(PATH_SEPARATOR));
  }

  @Override
  public String getPath() {

    return path;

  }

  @Override
  public DataUri getDataUri() {
    return DataUri.of().setDataStore(this.memoryDataStore.getName()).setPath(path);
  }

  @Override
  public MemoryDataPath getSibling(String name) {

    int i = this.path.lastIndexOf(PATH_SEPARATOR);
    String calculatedPath;
    if (i==-1){
      calculatedPath = name;
    } else {
      calculatedPath = this.path.substring(0,i) + PATH_SEPARATOR + name;
    }
    return this.memoryDataStore.getTypedDataPath(getType(),calculatedPath);

  }

  @Override
  public MemoryDataPath getChild(String name) {

    if (this.path.equals(MemoryDataStore.WORKING_PATH)) {
      return this.memoryDataStore.getDefaultDataPath(name);
    } else {
      return this.memoryDataStore.getTypedDataPath(getType(),this.path + PATH_SEPARATOR + name);
    }

  }

  @Override
  public MemoryDataPath resolve(String... names) {
    if (names.length==1) {
      return getChild(names[0]);
    } else {
      throw new RuntimeException("The memory data path system does not have any tree system.");
    }
  }

  @Override
  public MemoryDataPath getDataPath(String... names) {
    return this.resolve(names);
  }

  @Override
  public DataPath getChildAsTabular(String name) {
    return getChild(name);
  }



}
