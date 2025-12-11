package com.tabulify.gen;

import com.tabulify.conf.Attribute;
import com.tabulify.conf.ManifestDocument;
import com.tabulify.conf.Origin;
import com.tabulify.memory.MemoryConnection;
import com.tabulify.memory.MemoryDataPath;
import com.tabulify.memory.MemoryDataPathAbs;
import com.tabulify.spi.DataPath;
import com.tabulify.stream.InsertStream;
import com.tabulify.stream.SelectStream;
import com.tabulify.transfer.TransferPropertiesSystem;
import com.tabulify.uri.DataUriBuilder;
import com.tabulify.exception.CastException;
import com.tabulify.type.Casts;
import com.tabulify.type.KeyNormalizer;

/**
 * The generator path
 */
public class GenDataPath extends MemoryDataPathAbs implements DataPath, MemoryDataPath {


  private final GenDataPathUtility genDataPathUtility;
  /**
   * The manifest or null if created via code
   */
  private ManifestDocument manifest;

  /**
   * !!!!
   * Create a genMemDataPath with {@link com.tabulify.gen.DataGenerator.DataGeneratorBuilder#createGenDataPath(String)}
   * !!!
   */
  public GenDataPath(MemoryConnection memoryConnection, String path) {
    super(memoryConnection, path, GeneratorMediaType.FS_GENERATOR_TYPE);
    this.genDataPathUtility = new GenDataPathUtility(this);
    this.genDataPathUtility.initAttributes();
  }

  /**
   * The maximum number of record generated
   */

  public GenDataPath setMaxRecordCount(Long maxRecordCount) {
    this.genDataPathUtility.setMaxRecordCount(maxRecordCount);
    return this;
  }


  public Long getMaxRecordCount() {
    return this.genDataPathUtility.getMaxRecordCount();
  }

  /**
   * The number of record created in a stream
   * May be null
   */

  public Long getStreamRecordCount() {
    return this.genDataPathUtility.getStreamRecordCount();
  }


  public GenDataPath setStreamRecordCount(Long streamRecordCount) {
    this.genDataPathUtility.setStreamRecordCount(streamRecordCount);
    return this;
  }

  @Override
  public Long getSize() {
    return this.genDataPathUtility.getCount();
  }


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

  /**
   * @return the maximum size that this data path can return, not capped to max record count
   */
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
  public GenDataPath addAttribute(KeyNormalizer key, Object value) {

    /**
     * Note: this is an object that may receive manifest data
     */
    GenDataPathAttribute genDataPathAttribute;
    try {
      genDataPathAttribute = Casts.cast(key, GenDataPathAttribute.class);
    } catch (CastException e) {
      // may be a common attribute (logical name)
      return (GenDataPath) super.addAttribute(key, value);
    }
    Attribute attribute;
    try {
      attribute = this.getConnection().getTabular().getVault()
              .createVariableBuilderFromAttribute(genDataPathAttribute)
              .setOrigin(Origin.MANIFEST)
              .build(value);
      this.addAttribute(attribute);
    } catch (CastException e) {
      throw new IllegalArgumentException("The " + genDataPathAttribute + " value (" + value + ") is not conform . Error: " + e.getMessage(), e);
    }
    switch (genDataPathAttribute) {
      case MAX_RECORD_COUNT:
        return (GenDataPath) this.setMaxRecordCount((Long) attribute.getValueOrDefault());
      case STREAM_RECORD_COUNT:
        return this.setStreamRecordCount((Long) attribute.getValueOrDefault());
      default:
        // Not an updatable attribute, super handle the error
        return (GenDataPath) super.addAttribute(key, value);
    }
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


  public void setManifest(ManifestDocument manifest) {
    this.manifest = manifest;
  }

  /**
   * @return a data uri builder with the md connection if this gen data path was
   * created from a manifest
   */
  public DataUriBuilder getDataUriBuilder() {
    DataUriBuilder.DataUriBuilderBuilder builder = DataUriBuilder.builder(this.getConnection().getTabular());
    if(this.manifest != null) {
      builder.addManifestDirectoryConnection(this.manifest.getPath().getParent());
    }
    return builder.build();
  }
}
