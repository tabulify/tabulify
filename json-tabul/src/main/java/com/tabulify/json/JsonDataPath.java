package com.tabulify.json;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.tabulify.conf.Origin;
import com.tabulify.fs.FsConnection;
import com.tabulify.fs.textfile.FsTextDataPath;
import com.tabulify.fs.textfile.FsTextDataPathAttributes;
import com.tabulify.model.RelationDef;
import com.tabulify.model.RelationDefDefault;
import com.tabulify.model.SqlDataType;
import com.tabulify.model.SqlDataTypeAnsi;
import com.tabulify.stream.SelectStream;
import com.tabulify.exception.InternalException;
import com.tabulify.exception.NoVariableException;
import com.tabulify.type.Casts;
import com.tabulify.type.KeyNormalizer;
import com.tabulify.type.MediaType;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Collectors;

public class JsonDataPath extends FsTextDataPath {


  public static final String JSON_DEFAULT_HEADER_NAME = "json";


  public JsonDataPath(FsConnection fsConnection, Path path, MediaType mediaType) {

    super(fsConnection, path, JsonMediaType.castSafe(mediaType));

    /**
     * Populate the default
     */
    this.addVariablesFromEnumAttributeClass(JsonDataAttributes.class);

    /**
     * Overwrite the default column name
     */
    com.tabulify.conf.Attribute attribute = com.tabulify.conf.Attribute.create(FsTextDataPathAttributes.COLUMN_NAME, com.tabulify.conf.Origin.DEFAULT).setPlainValue(JSON_DEFAULT_HEADER_NAME);
    this.addAttribute(attribute);

  }

  @Override
  public RelationDef createRelationDef() {
    this.relationDef = new RelationDefDefault(this);
    buildColumnNamesIfNeeded();
    return this.relationDef;
  }

  private void buildColumnNamesIfNeeded() {
    Path nioPath = getAbsoluteNioPath();
    if (!Files.exists(nioPath)) {
      // empty file, no structure to derived
      return;
    }
    switch (this.getStructure()) {
      case PROPERTIES: {
        try {
          JsonFactory jsonFactory = new JsonFactory();
          try (BufferedReader bufferedReader = Files.newBufferedReader(nioPath)) {
            if (this.getMediaType() == JsonMediaType.JSONL) {
              bufferedReader.lines().forEach(s -> parseColumns(jsonFactory, s));
              return;
            }
            parseColumns(jsonFactory, bufferedReader.lines().collect(Collectors.joining(System.lineSeparator())));
          }
          return;
        } catch (IOException e) {
          throw new RuntimeException(e);
        }
      }
      case DOCUMENT: {
        SqlDataType<?> jsonDataType = this.getConnection().getSqlDataType(SqlDataTypeAnsi.JSON);
        if (jsonDataType == null) {
          jsonDataType = this.getConnection().getSqlDataType(SqlDataTypeAnsi.CHARACTER_VARYING);
        }
        this.relationDef.addColumn(getColumnName(), jsonDataType);
        return;
      }
      default:
        throw new InternalException("The structure type " + this.getStructure() + " should have been processed");
    }
  }

  private void parseColumns(JsonFactory jsonFactory, String s) {
    try {
      JsonParser jsonParser = jsonFactory.createParser(s);
      // Sanity check: verify that we got "Json Object":
      if (jsonParser.nextToken() != JsonToken.START_OBJECT) {
        throw new IOException("Expected data to start with an Object");
      }

      // Iterate over object fields:
      while (jsonParser.nextToken() != JsonToken.END_OBJECT) {
        // In the while, we moved to the field name
        this.relationDef.addColumn(jsonParser.getCurrentName());
        // Let's move to value
        jsonParser.nextToken();
      }

      jsonParser.close();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public SelectStream getSelectStream() {
    return new JsonSelectStream(this);
  }


  public JsonDataPath setStructure(JsonStructure jsonStructure) {
    com.tabulify.conf.Attribute attribute = com.tabulify.conf.Attribute.create(JsonDataAttributes.STRUCTURE, Origin.DEFAULT).setPlainValue(jsonStructure);
    this.addAttribute(attribute);
    return this;
  }

  public JsonStructure getStructure() {

    try {
      return (JsonStructure) this.getAttribute(JsonDataAttributes.STRUCTURE).getValueOrDefault();
    } catch (NoVariableException e) {
      throw new RuntimeException("Internal Error: Structure variable was not found. It should not happen");
    }

  }

  @Override
  public JsonDataPath addAttribute(KeyNormalizer key, Object value) {

    JsonDataAttributes attribute;
    try {
      attribute = Casts.cast(key, JsonDataAttributes.class);
    } catch (Exception e) {
      super.addAttribute(key, value);
      return this;
    }

    try {
      com.tabulify.conf.Attribute variable = getConnection().getTabular().getVault().createAttribute(attribute, value, Origin.DEFAULT);
      this.addAttribute(variable);
      return this;
    } catch (Exception e) {
      throw new RuntimeException("An error has occurred while creating the variable (" + attribute + ") with the value (" + value + ") for the resource (" + this + ")", e);
    }


  }
}
