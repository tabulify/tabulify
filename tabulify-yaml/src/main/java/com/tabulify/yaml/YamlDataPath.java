package com.tabulify.yaml;

import com.tabulify.fs.FsConnection;
import com.tabulify.fs.textfile.FsTextDataPath;
import com.tabulify.fs.textfile.FsTextDataPathAttributes;
import com.tabulify.model.RelationDef;
import com.tabulify.model.RelationDefDefault;
import com.tabulify.model.SqlTypes;
import com.tabulify.spi.DataPath;
import com.tabulify.stream.InsertStream;
import com.tabulify.stream.SelectStream;
import com.tabulify.transfer.TransferProperties;
import net.bytle.exception.NoValueException;
import net.bytle.exception.NoVariableException;
import net.bytle.type.*;

import java.nio.file.Path;
import java.sql.Types;

public class YamlDataPath extends FsTextDataPath {


  public static final String YAML_DEFAULT_HEADER_NAME = "yaml";

  public YamlStructure getStructure() {
    try {
      return (YamlStructure) this.getVariable(YamDataPathAttribute.STRUCTURE).getValueOrDefault();
    } catch (NoValueException | NoVariableException e) {
      throw new RuntimeException("Internal Error: Structure variable was not found. It should not happen");
    }
  }

  public YamlStyle getOutputStyle() {
    try {
      return (YamlStyle) this.getVariable(YamDataPathAttribute.OUTPUT_STYLE).getValueOrDefault();
    } catch (NoValueException | NoVariableException e) {
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
      this.getVariable(FsTextDataPathAttributes.COLUMN_NAME).setPlainValue(YAML_DEFAULT_HEADER_NAME);
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
  public InsertStream getInsertStream(DataPath source, TransferProperties transferProperties) {
    return super.getInsertStream(source, transferProperties);
  }

  @Override
  public MediaType getMediaType() {
    return MediaTypes.TEXT_YAML;
  }

  public YamlDataPath setStructure(YamlStructure yamlStructure) {
    Variable variable = Variable.create(YamDataPathAttribute.STRUCTURE, Origin.RUNTIME).setPlainValue(yamlStructure);
    this.addVariable(variable);
    return this;
  }


  @Override
  public YamlDataPath addVariable(String key, Object value) {


    YamDataPathAttribute attribute;
    try {
      attribute = Casts.cast(key, YamDataPathAttribute.class);
    } catch (Exception e) {
      super.addVariable(key, value);
      return this;
    }

    Variable variable;
    try {
      variable = this.getConnection().getTabular().createVariable(attribute, value);
      this.addVariable(variable);
      return this;
    } catch (Exception e) {
      throw new RuntimeException("The variable (" + attribute + ") with the value (" + value + ") for the resource (" + this + ") returns an error", e);
    }

  }

  public YamlDataPath setOutputStyle(YamlStyle yamlStyle) {
    Variable variable = Variable.create(YamDataPathAttribute.OUTPUT_STYLE, Origin.RUNTIME).setPlainValue(yamlStyle);
    this.addVariable(variable);
    return this;
  }

  private void buildColumnName() {

    int type = Types.VARCHAR;

    if (this.getOutputStyle() == YamlStyle.JSON) {
      type = SqlTypes.JSON;
    }
    this.relationDef.addColumn(getColumnName(), type);
  }

}

