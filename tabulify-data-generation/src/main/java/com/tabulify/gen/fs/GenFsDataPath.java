package com.tabulify.gen.fs;

import com.tabulify.fs.FsConnection;
import com.tabulify.fs.FsDataPath;
import com.tabulify.fs.textfile.FsTextDataPath;
import com.tabulify.gen.*;
import com.tabulify.spi.DataPath;
import com.tabulify.spi.DataPathAttribute;
import com.tabulify.stream.InsertStream;
import com.tabulify.stream.SelectStream;
import com.tabulify.transfer.TransferProperties;
import net.bytle.exception.NoVariableException;
import net.bytle.type.MediaType;

import java.nio.file.Files;
import java.nio.file.Path;

public class GenFsDataPath extends FsTextDataPath implements FsDataPath, GenDataPath {


  private final GenDataPathUtility genDataPathUtility;


  public GenFsDataPath(FsConnection fsConnection, Path path) {

    super(fsConnection, path, GenDataPathType.DATA_GEN);

    /**
     * Utility
     */
    this.genDataPathUtility = new GenDataPathUtility(this);

    /**
     * Default
     */
    try {
      this.getAttribute(DataPathAttribute.LOGICAL_NAME).setValueProvider(()->super.getName().replace(GenDataPathType.DATA_GEN.getExtension(), ""));
    } catch (NoVariableException e) {
      throw new IllegalStateException("The logical Name is a standard attribute and should exist");
    }
    this.genDataPathUtility.initVariables();

    /**
     * Overwrite Default
     */
    Path absoluteNioPath = this.getAbsoluteNioPath();
    if (Files.exists(absoluteNioPath)) {
      this.mergeDataDefinitionFromYamlFile(absoluteNioPath);
    }



  }


  @Override
  public GenRelationDef getOrCreateRelationDef() {
    if (super.relationDef == null) {
      return this.createRelationDef();
    }
    return (GenRelationDef) super.relationDef;
  }


  @Override
  public MediaType getMediaType() {
    return GenDataPathType.DATA_GEN;
  }


  @Override
  public GenFsManager getFileManager() {
    return GenFsManager.getSingletonOfFsManager();
  }

  @Override
  public GenRelationDef createRelationDef() {
    super.relationDef = new GenRelationDef(this);
    return (GenRelationDef) super.relationDef;
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
  public InsertStream getInsertStream(DataPath source, TransferProperties transferProperties) {
    throw new RuntimeException("A generator resource generates only data. You can't therefore insert in a generator");
  }

  @Override
  public SelectStream getSelectStream() {
    return new GenSelectStream(this);
  }

  @Override
  public Long getSizeNotCapped() {
    return this.genDataPathUtility.getMaxSizeFromGenerators();
  }

}
