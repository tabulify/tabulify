package com.tabulify.zip.entry;

import com.tabulify.conf.Attribute;
import com.tabulify.conf.Origin;
import com.tabulify.fs.FsConnection;
import com.tabulify.fs.binary.FsBinaryDataPath;
import com.tabulify.model.RelationDef;
import com.tabulify.zip.api.ArchiveEntryAttribute;
import com.tabulify.exception.CastException;
import com.tabulify.exception.MissingSwitchBranch;
import com.tabulify.type.Casts;
import com.tabulify.type.KeyNormalizer;
import com.tabulify.type.MediaType;

import java.nio.file.Path;

public class ArchiveEntryDataPath extends FsBinaryDataPath {



  private String entryPath;

  public ArchiveEntryDataPath(FsConnection fsConnection, Path relativePath, MediaType mediaType) {
    super(fsConnection, relativePath, mediaType);
    /**
     * Populate the default
     */
    this.addVariablesFromEnumAttributeClass(ArchiveEntryDataPathAttribute.class);

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
  public ArchiveEntryDataPath addAttribute(KeyNormalizer key, Object value) {
    ArchiveEntryDataPathAttribute archiveAttribute;
    try {
      archiveAttribute = Casts.cast(key, ArchiveEntryDataPathAttribute.class);
    } catch (CastException e) {
      // may be a common attribute (logical name)
      return (ArchiveEntryDataPath) super.addAttribute(key, value);
    }
    Attribute attribute;
    try {
      attribute = this.getConnection().getTabular().getVault()
        .createVariableBuilderFromAttribute(archiveAttribute)
        .setOrigin(Origin.MANIFEST)
        .build(value);
      this.addAttribute(attribute);
    } catch (CastException e) {
      throw new IllegalArgumentException("The " + archiveAttribute + " value (" + value + ") is not conform . Error: " + e.getMessage(), e);
    }
    switch (archiveAttribute) {
      case ENTRY_PATH:
        return this.setEntryPath(String.valueOf(attribute.getValueOrDefault()));
      default:
        throw new MissingSwitchBranch("commandAttribute", archiveAttribute);
    }
  }


  public ArchiveEntryDataPath setEntryPath(String entryPath) {
    this.entryPath = entryPath;
    return this;
  }

  public String getEntryPath() {
    return this.entryPath;
  }


}
