package com.tabulify.gen.memory;

import com.tabulify.gen.*;
import com.tabulify.memory.MemoryConnection;
import com.tabulify.memory.MemoryDataPath;
import com.tabulify.memory.MemoryDataPathAbs;
import com.tabulify.spi.DataPath;
import com.tabulify.stream.InsertStream;
import com.tabulify.stream.SelectStream;
import com.tabulify.transfer.TransferPropertiesSystem;
import net.bytle.type.KeyNormalizer;

/**
 * A gen path
 * * used when the data gen path is created without a file
 * * used as target of {@link com.tabulify.gen.fs.GenManifestDataPath} because {@link DataGenerator#toGenMemDataPath(DataPath, DataPath)}
 */
public class GenMemDataPath extends MemoryDataPathAbs implements DataPath, MemoryDataPath, GenDataPath {


  private final GenDataPathUtility genDataPathUtility;

  /**
   * !!!!
   * Create a genMemDataPath with {@link DataGenerator#createGenDataPath(String)}
   * !!!
   */
  public GenMemDataPath(MemoryConnection memoryConnection, String path) {
    super(memoryConnection, path, GeneratorMediaType.DATA_GEN);
    this.genDataPathUtility = new GenDataPathUtility(this);
    this.genDataPathUtility.initAttributes();
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
  public Long getStreamRecordCount() {
    return this.genDataPathUtility.getStreamRecordCount();
  }

  @Override
  public GenMemDataPath setStreamRecordCount(Long streamRecordCount) {
    this.genDataPathUtility.setStreamRecordCount(streamRecordCount);
    return this;
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
  public GenRelationDef createEmptyRelationDef() {
    return createRelationDef();
  }

  @Override
  public GenRelationDef createRelationDef() {
    this.relationDef = new GenRelationDef(this);
    return (GenRelationDef) this.relationDef;
  }

  @Override
  public Long getSizeNotCapped() {
    return this.genDataPathUtility.getMaxCountFromGenerators();
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
  public InsertStream getInsertStream(DataPath source, TransferPropertiesSystem transferProperties) {
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

  @Override
  public DataPath addAttribute(KeyNormalizer key, Object value) {
    /**
     * In GenMem, we allow setting of common data attributes
     * for generator attribute use the setter
     * This is not an object that receive manifest data
     * so we don't care about attributes
     * They are taken into account in the {@link com.tabulify.gen.fs.GenManifestDataPath#addAttribute(KeyNormalizer, Object)}
     */
    return super.addAttribute(key, value);
  }

}
