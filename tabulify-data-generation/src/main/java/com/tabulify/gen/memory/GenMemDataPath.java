package com.tabulify.gen.memory;

import com.tabulify.gen.*;
import com.tabulify.memory.MemoryConnection;
import com.tabulify.memory.MemoryDataPath;
import com.tabulify.memory.MemoryDataPathAbs;
import com.tabulify.spi.DataPath;
import com.tabulify.stream.InsertStream;
import com.tabulify.stream.SelectStream;
import com.tabulify.transfer.TransferProperties;

/**
 * A path specifically created for test
 * when the data gen path is created without a file
 * <p>
 * If you use a file, use the {@link GenDataPath}
 */
public class GenMemDataPath extends MemoryDataPathAbs implements DataPath, MemoryDataPath, GenDataPath {


  private final GenDataPathUtility genDataPathUtility;

  /**
   * !!!!
   * Create a genMemDataPath with {@link DataGenerator#createGenDataPath(String)}
   * !!!
   *
   */
  public GenMemDataPath(MemoryConnection memoryConnection, String path) {
    super(memoryConnection, path, GenDataPathType.DATA_GEN);
    this.genDataPathUtility = new GenDataPathUtility(this);
    this.genDataPathUtility.initVariables();
  }


  @Override
  public GenDataPath setMaxRecordCount(Long maxRecordCount) {
    this.genDataPathUtility.setMaxRecordCount(maxRecordCount);
    return this;
  }

  @Override
  public Long getMaxRecordCount() {
    return this.genDataPathUtility.getMaxRecordCount();
  }

  @Override
  public Long getSize() {
    return this.genDataPathUtility.getCount();
  }

  @Override
  public GenDataPathUtility getGenDataPathUtility() {
    return this.genDataPathUtility;
  }

  @Override
  public GenRelationDef getOrCreateRelationDef() {
    if (this.relationDef == null) {
      this.relationDef = new GenRelationDef(this);
    }
    return (GenRelationDef) this.relationDef;
  }

  @Override
  public GenRelationDef createRelationDef() {
    this.relationDef = new GenRelationDef(this);
    return (GenRelationDef) this.relationDef;
  }

  @Override
  public Long getSizeNotCapped() {
    return this.genDataPathUtility.getMaxSizeFromGenerators();
  }


  @Override
  public void truncate() {
    // Nothing to do
  }

  @Override
  public Long getCount() {
    return this.getSize();
  }

  @Override
  public void create() {
    // Nothing to do
  }


  @Override
  public InsertStream getInsertStream(DataPath source, TransferProperties transferProperties) {
    throw new RuntimeException("You can't insert in a generator data resource");
  }

  @Override
  public SelectStream getSelectStream() {
    return new GenSelectStream(this);
  }

  @Override
  public DataPath getParent() {
    return this.getConnection().getCurrentDataPath();
  }

  @Override
  public boolean hasHeaderInContent() {
    return false;
  }


}
