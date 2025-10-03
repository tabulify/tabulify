package com.tabulify.gen.fs;

import com.tabulify.conf.Attribute;
import com.tabulify.conf.ManifestDocument;
import com.tabulify.conf.Origin;
import com.tabulify.fs.FsConnection;
import com.tabulify.fs.FsDataPath;
import com.tabulify.fs.textfile.FsTextDataPath;
import com.tabulify.gen.*;
import com.tabulify.spi.DataPath;
import com.tabulify.spi.DataPathAttribute;
import com.tabulify.stream.InsertStream;
import com.tabulify.stream.SelectStream;
import com.tabulify.transfer.TransferPropertiesSystem;
import net.bytle.exception.CastException;
import net.bytle.exception.NoVariableException;
import net.bytle.type.Casts;
import net.bytle.type.KeyNormalizer;
import net.bytle.type.MediaType;

import java.nio.file.Files;
import java.nio.file.Path;

public class GenManifestDataPath extends FsTextDataPath implements FsDataPath, GenDataPath {


  private final GenDataPathUtility genDataPathUtility;


  public GenManifestDataPath(FsConnection fsConnection, Path path) {

    super(fsConnection, path, GeneratorMediaType.DATA_GEN);

    /**
     * Utility
     */
    this.genDataPathUtility = new GenDataPathUtility(this);

    /**
     * Default
     */
    try {
      this.getAttribute(DataPathAttribute.LOGICAL_NAME).setValueProvider(() -> super.getName().replace(GeneratorMediaType.DATA_GEN.getExtension(), ""));
    } catch (NoVariableException e) {
      throw new IllegalStateException("The logical Name is a standard attribute and should exist");
    }

    /**
     * Adding the attributes
     */
    this.genDataPathUtility.initAttributes();

    /**
     * Creating columns
     */
    Path absoluteNioPath = this.getAbsoluteNioPath();
    if (Files.exists(absoluteNioPath)) {
      ManifestDocument metadata = ManifestDocument.builder().setPath(absoluteNioPath).build();
      KeyNormalizer kind = metadata.getKind();
      if (!kind.equals(GeneratorMediaType.KIND)) {
        throw new IllegalArgumentException("The metadata is not a " + GeneratorMediaType.KIND + " kind but a " + kind);
      }
      this.mergeDataDefinitionFromYamlMap(metadata.getSpecMap());
    }



  }


  @Override
  public GenRelationDef createEmptyRelationDef() {
    super.relationDef = new GenRelationDef(this);
    return (GenRelationDef) super.relationDef;
  }

  @Override
  public GenRelationDef getOrCreateRelationDef() {
    return (GenRelationDef) super.getOrCreateRelationDef();
  }


  @Override
  public MediaType getMediaType() {
    return GeneratorMediaType.DATA_GEN;
  }


  @Override
  public GenManifestProvider.GenManifestManager getFileManager() {
    return GenManifestProvider.GEN_MANIFEST_MANAGER;
  }

  @Override
  public GenRelationDef createRelationDef() {
    return this.createEmptyRelationDef();
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
  public GenDataPath setStreamRecordCount(Long streamRecordCount) {
    this.genDataPathUtility.setStreamRecordCount(streamRecordCount);
    return this;
  }

  @Override
  public Long getSize() {
    return null;
  }

  @Override
  public GenDataPathUtility getGenDataPathUtility() {
    return this.genDataPathUtility;
  }

  @Override
  public Long getCount() {

    return this.genDataPathUtility.getCount();

  }


  @Override
  public InsertStream getInsertStream(DataPath source, TransferPropertiesSystem transferProperties) {
    throw new RuntimeException("A generator resource generates only data. You can't therefore insert in a generator");
  }

  @Override
  public SelectStream getSelectStream() {
    return new GenSelectStream(this);
  }

  @Override
  public Long getSizeNotCapped() {
    return this.genDataPathUtility.getMaxCountFromGenerators();
  }

  @Override
  public GenManifestDataPath addAttribute(KeyNormalizer key, Object value) {
    GenDataPathAttribute genDataPathAttribute;
    try {
      genDataPathAttribute = Casts.cast(key, GenDataPathAttribute.class);
    } catch (CastException e) {
      // may be a common attribute (logical name)
      return (GenManifestDataPath) super.addAttribute(key, value);
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
        return (GenManifestDataPath) this.setMaxRecordCount((Long) attribute.getValueOrDefault());
      case STREAM_RECORD_COUNT:
        return (GenManifestDataPath) this.setStreamRecordCount((Long) attribute.getValueOrDefault());
      default:
        // Not an updatable attribute, super handle the error
        return (GenManifestDataPath) super.addAttribute(key, value);
    }
  }


}
