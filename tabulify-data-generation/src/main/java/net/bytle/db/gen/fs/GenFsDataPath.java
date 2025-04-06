package net.bytle.db.gen.fs;

import net.bytle.db.fs.FsConnection;
import net.bytle.db.fs.FsDataPath;
import net.bytle.db.fs.textfile.FsTextDataPath;
import net.bytle.db.gen.*;
import net.bytle.db.spi.DataPath;
import net.bytle.db.spi.DataPathAttribute;
import net.bytle.db.stream.InsertStream;
import net.bytle.db.stream.SelectStream;
import net.bytle.db.transfer.TransferProperties;
import net.bytle.exception.NoVariableException;
import net.bytle.type.MediaType;

import java.nio.file.Files;
import java.nio.file.Path;

public class GenFsDataPath extends FsTextDataPath implements FsDataPath, GenDataPath {


  public static final String DATA_GEN_EXTENSION = "";
  private final GenDataPathUtility genDataPathUtility;


  public GenFsDataPath(FsConnection fsConnection, Path path) {

    super(fsConnection, path, GenDataPathType.DATA_GEN);

    Path absoluteNioPath = this.getAbsoluteNioPath();
    if (Files.exists(absoluteNioPath)) {
      this.mergeDataDefinitionFromYamlFile(absoluteNioPath);
    }

    this.genDataPathUtility = new GenDataPathUtility(this);


    try {
      this.getVariable(DataPathAttribute.LOGICAL_NAME).setValueProvider(()->super.getName().replace(GenDataPathType.DATA_GEN.getExtension(), ""));
    } catch (NoVariableException e) {
      throw new IllegalStateException("The logical Name is a standard attribute and should exist");
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

}
