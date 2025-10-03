package com.tabulify.yaml;

import com.tabulify.conf.Origin;
import com.tabulify.fs.FsConnection;
import com.tabulify.fs.textfile.FsTextDataPath;
import com.tabulify.fs.textfile.FsTextDataPathAttributes;
import com.tabulify.model.RelationDef;
import com.tabulify.model.RelationDefDefault;
import com.tabulify.model.SqlDataTypeAnsi;
import com.tabulify.stream.SelectStream;
import net.bytle.exception.NoVariableException;
import net.bytle.type.Casts;
import net.bytle.type.KeyNormalizer;
import net.bytle.type.MediaType;
import net.bytle.type.MediaTypes;

import java.nio.file.Path;

import static com.tabulify.conf.Origin.DEFAULT;

public class YamlDataPath extends FsTextDataPath {


  public static final String YAML_DEFAULT_HEADER_NAME = "yaml";

  public YamlStructure getStructure() {
    try {
      return (YamlStructure) this.getAttribute(YamDataPathAttribute.STRUCTURE).getValueOrDefault();
    } catch (NoVariableException e) {
      throw new RuntimeException("Internal Error: Structure variable was not found. It should not happen");
    }
  }


  public YamlStyle getOutputStyle() {
    try {
      return (YamlStyle) this.getAttribute(YamDataPathAttribute.STYLE).getValueOrDefault();
    } catch (NoVariableException e) {
      throw new RuntimeException("Internal Error: output style variable was not found. It should not happen");
    }
  }


  public YamlDataPath(FsConnection fsConnection, Path path) {

    super(fsConnection, path);

    /**
     * Populate the default
     */
    this.addVariablesFromEnumAttributeClass(YamDataPathAttribute.class);


    /**
     * Change the default value
     */
    try {
      this.getAttribute(FsTextDataPathAttributes.COLUMN_NAME).setPlainValue(YAML_DEFAULT_HEADER_NAME);
    } catch (NoVariableException e) {
      throw new RuntimeException("Internal Error: COLUMN_NAME variable was not found. It should not happen");
    }


  }

  @Override
  public RelationDef getOrCreateRelationDef() {
    if (getRelationDef() == null) {

      this.relationDef = new RelationDefDefault(this);
      this.buildColumnName();

    }
    return super.getOrCreateRelationDef();
  }


  @Override
  public SelectStream getSelectStream() {
    return new YamlSelectStream(this);
  }


  @Override
  public MediaType getMediaType() {
    return MediaTypes.TEXT_YAML;
  }

  public YamlDataPath setStructure(YamlStructure yamlStructure) {
    com.tabulify.conf.Attribute attribute = com.tabulify.conf.Attribute.create(YamDataPathAttribute.STRUCTURE, com.tabulify.conf.Origin.DEFAULT).setPlainValue(yamlStructure);
    this.addAttribute(attribute);
    return this;
  }


  @Override
  public YamlDataPath addAttribute(KeyNormalizer key, Object value) {


    YamDataPathAttribute attribute;
    try {
      attribute = Casts.cast(key, YamDataPathAttribute.class);
    } catch (Exception e) {
      super.addAttribute(key, value);
      return this;
    }

    com.tabulify.conf.Attribute variable;
    try {
      variable = this.getConnection().getTabular().getVault().createAttribute(attribute, value, DEFAULT);
      this.addAttribute(variable);
      return this;
    } catch (Exception e) {
      throw new RuntimeException("The variable (" + attribute + ") with the value (" + value + ") for the resource (" + this + ") returns an error", e);
    }

  }

  public YamlDataPath setOutputStyle(YamlStyle yamlStyle) {
    com.tabulify.conf.Attribute attribute = com.tabulify.conf.Attribute.create(YamDataPathAttribute.STYLE, Origin.DEFAULT).setPlainValue(yamlStyle);
    this.addAttribute(attribute);
    return this;
  }

  private void buildColumnName() {

    /**
     * JSON
     */
    this.relationDef.addColumn(getColumnName(), SqlDataTypeAnsi.JSON);

  }

}

