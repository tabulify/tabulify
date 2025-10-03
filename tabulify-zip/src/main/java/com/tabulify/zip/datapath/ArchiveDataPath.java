package com.tabulify.zip.datapath;

import com.tabulify.conf.Attribute;
import com.tabulify.conf.Origin;
import com.tabulify.fs.FsConnection;
import com.tabulify.fs.binary.FsBinaryDataPath;
import com.tabulify.model.RelationDef;
import com.tabulify.stream.SelectStream;
import com.tabulify.zip.api.Archive;
import com.tabulify.zip.api.ArchiveEntryAttribute;
import com.tabulify.zip.api.ArchiveMediaType;
import net.bytle.exception.InternalException;
import net.bytle.regexp.Glob;
import net.bytle.type.Casts;
import net.bytle.type.KeyNormalizer;

import java.nio.file.Path;

public class ArchiveDataPath extends FsBinaryDataPath {


  private final Archive archive;
  private Glob nameSelector;

  public ArchiveDataPath(FsConnection fsConnection, Path relativePath, ArchiveMediaType mediaType) {
    super(fsConnection, relativePath, mediaType);
    this.archive = Archive.builder()
      .setArchive(getAbsoluteNioPath())
      .setMediaType(mediaType)
      .build();
    this.addVariablesFromEnumAttributeClass(ArchiveDataPathAttribute.class);
  }


  @Override
  public SelectStream getSelectStream() {
    return new ArchiveSelectStream(this);
  }

  @Override
  public RelationDef createRelationDef() {
    RelationDef relationDef = super.createRelationDef();
    for (ArchiveEntryAttribute archiveEntryAttribute : ArchiveEntryAttribute.values()) {
      relationDef.addColumn(archiveEntryAttribute.toKeyNormalizer().toSqlCase(), archiveEntryAttribute.getClazz());
    }
    return relationDef;

  }

  @Override
  public ArchiveDataPath addAttribute(KeyNormalizer key, Object value) {


    ArchiveDataPathAttribute archiveDataPathAttribute;
    try {
      archiveDataPathAttribute = Casts.cast(key, ArchiveDataPathAttribute.class);
      Attribute attribute = this.getConnection().getTabular().getVault().createAttribute(archiveDataPathAttribute, value, Origin.DEFAULT);
      super.addAttribute(attribute);
    } catch (Exception e) {
      // It may be a text property
      super.addAttribute(key, value);
      return this;
    }

    switch (archiveDataPathAttribute) {
      case ENTRY_SELECTOR:
        this.setNameSelector(Glob.createOf(value.toString()));
        return this;
      default:
        throw new InternalException("The archive data path attribute (" + archiveDataPathAttribute + ") is missing in th switch branch");
    }

  }

  private ArchiveDataPath setNameSelector(Glob nameSelector) {
    this.nameSelector = nameSelector;
    return this;
  }

  public Archive getArchive() {
    return this.archive;
  }

  public Glob getNameSelector() {
    return this.nameSelector;
  }

}
