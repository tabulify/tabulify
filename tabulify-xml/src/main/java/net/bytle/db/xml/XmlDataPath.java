package net.bytle.db.xml;

import net.bytle.db.fs.FsConnection;
import net.bytle.db.fs.textfile.FsTextDataPath;
import net.bytle.db.model.RelationDef;
import net.bytle.db.model.RelationDefDefault;
import net.bytle.type.*;

import java.nio.file.Path;
import java.sql.Types;

public class XmlDataPath extends FsTextDataPath {


  public static final MediaType MEDIA_TYPE_EXTENSION = MediaTypes.TEXT_XML;

  enum XML_ATTRIBUTE implements Attribute {

    COLUMN_NAME("The name of the column when the JSON is returned in one column"),
    ;

    private final String description;

    XML_ATTRIBUTE(String description) {

      this.description = description;
    }

    @Override
    public String getDescription() {
      return this.description;
    }

    @Override
    public Class<?> getValueClazz() {
      return String.class;
    }

    @Override
    public Object getDefaultValue() {
      return null;
    }


  }


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

    this.relationDef.addColumn(getColumnName(), Types.SQLXML);

  }


  @Override
  public MediaType getMediaType() {

    return MediaTypes.TEXT_XML;

  }


  @Override
  public XmlDataPath addVariable(String key, Object value) {


    XML_ATTRIBUTE attribute;
    try {
      attribute = Casts.cast(key, XML_ATTRIBUTE.class);
    } catch (Exception e) {
      super.addVariable(key, value);
      return this;
    }

    Variable variable;
    try {
      variable = this.getConnection().getTabular().createVariable(attribute, value);
    } catch (Exception e) {
      throw new RuntimeException("Error while creating the variable (" + attribute + ") with the value (" + value + ") for the resource (" + this + ").", e);
    }
    super.addVariable(variable);
    return this;
  }
}



