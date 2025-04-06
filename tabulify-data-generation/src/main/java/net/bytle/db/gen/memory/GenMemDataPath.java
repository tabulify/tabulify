package net.bytle.db.gen.memory;

import net.bytle.db.gen.*;
import net.bytle.db.memory.MemoryConnection;
import net.bytle.db.memory.MemoryDataPath;
import net.bytle.db.memory.MemoryDataPathAbs;
import net.bytle.db.spi.DataPath;
import net.bytle.db.stream.InsertStream;
import net.bytle.db.stream.SelectStream;
import net.bytle.db.transfer.TransferProperties;

/**
 * A path specifically created for test
 * when the data gen path is created without a file
 * <p>
 * If you use a file, use the {@link GenDataPath}
 */
public class GenMemDataPath extends MemoryDataPathAbs implements MemoryDataPath, GenDataPath {


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


}
