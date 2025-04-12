package com.tabulify.memory.list;


import com.tabulify.memory.MemoryConnection;
import com.tabulify.memory.MemoryDataPathAbs;
import com.tabulify.memory.MemoryDataPathType;
import com.tabulify.spi.DataPath;
import com.tabulify.stream.InsertStream;
import com.tabulify.stream.SelectStream;
import com.tabulify.transfer.TransferProperties;

import java.util.ArrayList;
import java.util.List;

public class MemoryListDataPath extends MemoryDataPathAbs {



  private List<List<Object>> values = new ArrayList<>();

  public MemoryListDataPath(MemoryConnection memoryConnection, String path) {

    super(memoryConnection, path, MemoryDataPathType.LIST);

  }

  public static MemoryListDataPath of(MemoryConnection memoryConnection, String path) {
    return new MemoryListDataPath(memoryConnection, path);
  }


  @Override
  public void truncate() {
    this.values = new ArrayList<>();
  }

  @Override
  public Long getCount() {
    return (long) values.size();
  }

  @Override
  public InsertStream getInsertStream(DataPath source, TransferProperties transferProperties) {
    return new MemoryListInsertStream(this);
  }

  @Override
  public void create() {
    this.values = new ArrayList<>();
  }

  public List<List<Object>> getValues() {
    return values;
  }



  @Override
  public SelectStream getSelectStream() {
    return new MemoryListSelectStream(this);
  }

  @Override
  public DataPath getParent() {
    return this.getConnection().getCurrentDataPath();
  }

  @Override
  public Long getSize() {
    return (long) values.size();
  }

}
