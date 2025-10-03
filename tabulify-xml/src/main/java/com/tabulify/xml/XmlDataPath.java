package com.tabulify.xml;

import com.tabulify.conf.Attribute;
import com.tabulify.fs.FsConnection;
import com.tabulify.fs.textfile.FsTextDataPath;
import com.tabulify.model.RelationDef;
import com.tabulify.model.RelationDefDefault;
import com.tabulify.model.SqlDataTypeAnsi;
import net.bytle.type.Casts;
import net.bytle.type.KeyNormalizer;
import net.bytle.type.MediaType;
import net.bytle.type.MediaTypes;

import java.nio.file.Path;

import static com.tabulify.conf.Origin.DEFAULT;

public class XmlDataPath extends FsTextDataPath {


  public static final String XML_DEFAULT_HEADER_NAME = "xml";

  public XmlDataPath(FsConnection fsConnection, Path path) {

    super(fsConnection, path);
    this.setColumnName(XML_DEFAULT_HEADER_NAME);
    this.setEndOfRecords(FsTextDataPath.EOF);

  }


  @Override
  public RelationDef getOrCreateRelationDef() {
    if (getRelationDef() == null) {

      this.relationDef = new RelationDefDefault(this);
      buildColumnNamesIfNeeded();

    }
    return super.getOrCreateRelationDef();
  }

  private void buildColumnNamesIfNeeded() {

    this.relationDef.addColumn(getColumnName(), SqlDataTypeAnsi.XML);

  }


  @Override
  public MediaType getMediaType() {

    return MediaTypes.TEXT_XML;

  }


  @Override
  public XmlDataPath addAttribute(KeyNormalizer key, Object value) {


    XmlDataPathAttribute attribute;
    try {
      attribute = Casts.cast(key, XmlDataPathAttribute.class);
    } catch (Exception e) {
      super.addAttribute(key, value);
      return this;
    }

    Attribute variable;
    try {
      variable = this.getConnection().getTabular().getVault().createAttribute(attribute, value, DEFAULT);
    } catch (Exception e) {
      throw new RuntimeException("Error while creating the variable (" + attribute + ") with the value (" + value + ") for the resource (" + this + ").", e);
    }
    super.addAttribute(variable);
    return this;
  }

}



