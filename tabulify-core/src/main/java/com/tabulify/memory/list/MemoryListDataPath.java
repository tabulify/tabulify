package com.tabulify.memory.list;


import com.tabulify.memory.MemoryConnection;
import com.tabulify.memory.MemoryDataPathAbs;
import com.tabulify.memory.MemoryDataPathType;
import com.tabulify.spi.DataPath;
import com.tabulify.spi.SchemaType;
import com.tabulify.stream.InsertStream;
import com.tabulify.stream.SelectStream;
import com.tabulify.transfer.TransferPropertiesSystem;

import java.util.ArrayList;
import java.util.List;

public class MemoryListDataPath extends MemoryDataPathAbs {


  private List<List<?>> values = new ArrayList<>();

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
  public InsertStream getInsertStream(DataPath source, TransferPropertiesSystem transferProperties) {
    return new MemoryListInsertStream(this);
  }

  @Override
  public void create() {
    this.values = new ArrayList<>();
  }

  public List<List<?>> getValues() {
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
  public boolean hasHeaderInContent() {
    return false;
  }

  @SuppressWarnings("RedundantMethodOverride")
  @Override
  public SchemaType getSchemaType() {
    return SchemaType.STRICT;
  }

  @Override
  public Long getSize() {
    return (long) values.size();
  }

  public void setValues(List<List<?>> values) {
    this.values = values;
  }

  public MemoryListDataPath addValues(List<List<?>> values) {
    this.values.addAll(values);
    return this;
  }

}
