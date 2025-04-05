package net.bytle.db.json;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import net.bytle.db.fs.FsConnection;
import net.bytle.db.fs.textfile.FsTextDataPath;
import net.bytle.db.fs.textfile.FsTextDataPathAttributes;
import net.bytle.db.model.RelationDef;
import net.bytle.db.model.RelationDefDefault;
import net.bytle.db.model.SqlTypes;
import net.bytle.db.spi.DataPath;
import net.bytle.db.stream.InsertStream;
import net.bytle.db.stream.SelectStream;
import net.bytle.db.transfer.TransferProperties;
import net.bytle.exception.NoValueException;
import net.bytle.exception.NoVariableException;
import net.bytle.fs.Fs;
import net.bytle.type.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Collectors;

public class JsonDataPath extends FsTextDataPath {


  public static final MediaType[] ACCEPTED_MEDIA_TYPES = {MediaTypes.TEXT_JSON, MediaTypes.TEXT_JSONL};

  enum JsonATTRIBUTE implements Attribute {

    STRUCTURE("How to JSON is returned (as one JSON column or as a table with the column being the first level properties", JsonStructure.class, JsonStructure.DOCUMENT);


    private final String description;
    private final Class<?> clazz;
    private final Object defaultValue;

    JsonATTRIBUTE(String description, Class<?> clazz, Object defaultValue) {

      this.description = description;
      this.clazz = clazz;
      this.defaultValue = defaultValue;

    }


    @Override
    public String getDescription() {
      return this.description;
    }

    @Override
    public Class<?> getValueClazz() {
      return this.clazz;
    }

    @Override
    public Object getDefaultValue() {
      return this.defaultValue;
    }


  }


  public static final String JSON_DEFAULT_HEADER_NAME = "json";


  public JsonDataPath(FsConnection fsConnection, Path path, MediaType mediaType) {

    super(fsConnection, path, mediaType);

    /**
     * Populate the default
     */
    this.addVariablesFromEnumAttributeClass(JsonATTRIBUTE.class);

    /**
     * Overwrite the default column name
     */
    Variable variable = Variable.create(FsTextDataPathAttributes.COLUMN_NAME, Origin.INTERNAL).setOriginalValue(JSON_DEFAULT_HEADER_NAME);
    this.addVariable(variable);

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
    if (this.getStructure() == JsonStructure.PROPERTIES) {
      try {
        JsonFactory jsonFactory = new JsonFactory();
        Path nioPath = getNioPath();
        if (!Files.exists(nioPath)) {
          throw new RuntimeException("The file " + nioPath.toAbsolutePath() + " does not exist, we can't read it");
        }
        if (Fs.getExtension(this.getNioPath()).equalsIgnoreCase("jsonl")) {
          Files.newBufferedReader(nioPath).lines().forEach(s -> parseColumns(jsonFactory, s));
        } else {
          parseColumns(jsonFactory, Files.newBufferedReader(nioPath).lines().collect(Collectors.joining(System.lineSeparator())));
        }
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    } else {
      this.relationDef.addColumn(getColumnName(), SqlTypes.JSON);
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

  @Override
  public InsertStream getInsertStream(DataPath source, TransferProperties transferProperties) {
    return super.getInsertStream(source, transferProperties);
  }


  public JsonDataPath setStructure(JsonStructure jsonStructure) {
    Variable variable = Variable.create(JsonATTRIBUTE.STRUCTURE, Origin.INTERNAL).setOriginalValue(jsonStructure);
    this.addVariable(variable);
    return this;
  }

  public JsonStructure getStructure() {

    try {
      return (JsonStructure) this.getVariable(JsonATTRIBUTE.STRUCTURE).getValueOrDefault();
    } catch (NoVariableException | NoValueException e) {
      throw new RuntimeException("Internal Error: Structure variable was not found. It should not happen");
    }

  }

  @Override
  public JsonDataPath addVariable(String key, Object value) {

    JsonATTRIBUTE attribute;
    try {
      attribute = Casts.cast(key, JsonATTRIBUTE.class);
    } catch (Exception e) {
      super.addVariable(key, value);
      return this;
    }

    try {
      Variable variable = getConnection().getTabular().createVariable(attribute, value);
      this.addVariable(variable);
      return this;
    } catch (Exception e) {
      throw new RuntimeException("An error has occurred while creating the variable (" + attribute + ") with the value (" + value + ") for the resource (" + this + ")", e);
    }


  }
}



