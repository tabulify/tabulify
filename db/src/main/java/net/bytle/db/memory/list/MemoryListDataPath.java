package net.bytle.db.memory.list;


import net.bytle.db.memory.MemoryDataPathAbs;
import net.bytle.db.memory.MemoryDataStore;
import net.bytle.db.stream.InsertStream;
import net.bytle.db.stream.SelectStream;

import java.util.ArrayList;
import java.util.List;

public class MemoryListDataPath extends MemoryDataPathAbs {


  /**
   * Type
   */
  public static final String TYPE = "LIST";
  private List<List<Object>> values = new ArrayList<>();

  public MemoryListDataPath(MemoryDataStore memoryDataStore, String path) {
    super(memoryDataStore, path);
  }

  public static MemoryListDataPath of(MemoryDataStore memoryDataStore, String path) {
    return new MemoryListDataPath(memoryDataStore, path);
  }

  public String getType() {
    return TYPE;
  }


  @Override
  public void truncate() {
    this.values = new ArrayList<>();
  }

  @Override
  public long size() {
    return values.size();
  }

  @Override
  public void create() {
    this.values = new ArrayList<>();
  }

  public List<List<Object>> getValues() {
    return values;
  }

  @Override
  public InsertStream getInsertStream(){
    return new MemoryListInsertStream(this);
  }



  @Override
  public SelectStream getSelectStream() {
    return new MemoryListSelectStream(this);
  }

  @Override
  public void drop() {
    this.values = null;
  }

  @Override
  public Boolean exists(){
    return this.values!=null;
  }

}
