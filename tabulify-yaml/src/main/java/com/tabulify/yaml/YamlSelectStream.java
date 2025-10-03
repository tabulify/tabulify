package com.tabulify.yaml;


import com.tabulify.model.ColumnDef;
import com.tabulify.model.RelationDef;
import com.tabulify.stream.SelectStream;
import com.tabulify.stream.SelectStreamAbs;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;
import org.yaml.snakeyaml.representer.Representer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;

public class YamlSelectStream extends SelectStreamAbs {

  private final YamlDataPath yamlDataPath;
  private final Yaml yaml;
  private int lineNumber = 0;

  // A map to hold
  private Map<String, String> currentRecordKeyValue;


  private Iterator<Object> yamlDocumentIterator;


  public YamlSelectStream(YamlDataPath dataPath) {
    super(dataPath);
    this.yamlDataPath = dataPath;
    YamlStyle style = this.yamlDataPath.getOutputStyle();
    DumperOptions dumperOptions = new DumperOptions();
    dumperOptions.setIndent(2);
    dumperOptions.setPrettyFlow(true);

    if (style == YamlStyle.BLOCK) {
      dumperOptions.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
      this.yaml = new Yaml(dumperOptions);
    } else {
      // json
      dumperOptions.setDefaultFlowStyle(DumperOptions.FlowStyle.FLOW);
      // no line break with \ (not json compatible)
      dumperOptions.setWidth(Integer.MAX_VALUE);
      // Quote parameter: https://yaml.org/spec/1.2.2/#flow-scalar-styles
      dumperOptions.setDefaultScalarStyle(DumperOptions.ScalarStyle.DOUBLE_QUOTED);
      // delete the type tag such as !!timestamp
      YamlNoImplicitTagResolver noImplicitTagResolver = new YamlNoImplicitTagResolver();
      this.yaml = new Yaml(
        new Constructor(new LoaderOptions()),
        new Representer(dumperOptions),
        dumperOptions,
        noImplicitTagResolver
      );
    }
    beforeFirst();
  }

  public static SelectStream of(YamlDataPath dataPath) {
    return new YamlSelectStream(dataPath);
  }


  @Override
  public boolean next() {

    Object currentYamlDocument;
    this.currentRecordKeyValue = new HashMap<>();
    try {
      currentYamlDocument = this.yamlDocumentIterator.next();
    } catch (NoSuchElementException e) {
      this.currentRecordKeyValue = null;
      return false;
    } catch (Exception e){
      throw new RuntimeException("An error has occurred while reading the resource ("+this.yamlDataPath+") Error: "+e.getMessage(), e);
    }

    YamlStructure structure = this.yamlDataPath.getStructure();
    if (structure != YamlStructure.PROPERTIES) {
      StringWriter stringWriter = new StringWriter();
      this.yaml.dump(currentYamlDocument, stringWriter);
      String value = stringWriter.toString();
      this.currentRecordKeyValue.put(this.yamlDataPath.getColumnName(), value);
    } else {
      throw new IllegalStateException("structure (" + structure + ") not implemented");
    }
    return true;
  }

  private boolean isClosed = false;
  @Override
  public void close() {
    this.isClosed = true;
  }

  @Override
  public boolean isClosed() {
    return this.isClosed;
  }

  private String getColumnName(int columnIndex) {
    return yamlDataPath.getOrCreateRelationDef().getColumnDef(columnIndex).getColumnName();
  }

  @Override
  public long getRecordId() {
    return lineNumber;
  }


  @Override
  public Object getObject(ColumnDef columnDef) {
    return currentRecordKeyValue.get(getColumnName(columnDef.getColumnPosition()));
  }


  @Override
  public Object getObject(String columnName) {
    return currentRecordKeyValue.get(columnName);
  }

  @Override
  public void beforeFirst() {
    try {
      lineNumber = 0;
      BufferedReader input = Files.newBufferedReader(yamlDataPath.getAbsoluteNioPath());
      yamlDocumentIterator = this.yaml.loadAll(input).iterator();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public RelationDef getRuntimeRelationDef() {
    return this.yamlDataPath.getOrCreateRelationDef();
  }

}
