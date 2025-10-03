package com.tabulify.json;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.tabulify.model.ColumnDef;
import com.tabulify.model.RelationDef;
import com.tabulify.stream.SelectStream;
import com.tabulify.stream.SelectStreamAbs;
import net.bytle.fs.Fs;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class JsonSelectStream extends SelectStreamAbs {

  private final JsonDataPath jsonDataPath;
  private BufferedReader reader;
  private final JsonFactory jsonFactory;
  private int lineNumber = 0;

  // A map to hold
  private Map<String, String> currentRecordKeyValue;

  /**
   * A pointer to know in case of a JSON file
   * if the whole JSON file was read
   */
  private boolean wholeJsonFileWasRead = false;

  public JsonSelectStream(JsonDataPath dataPath) {
    super(dataPath);
    this.jsonDataPath = dataPath;
    jsonFactory = new JsonFactory();
    beforeFirst();
  }

  public static SelectStream of(JsonDataPath dataPath) {
    return new JsonSelectStream(dataPath);
  }


  @Override
  public boolean next() {
    try {
      String currentLine;
      if (Fs.getExtension(jsonDataPath.getAbsoluteNioPath()).equalsIgnoreCase("jsonl")) {
        currentLine = reader.readLine();
        if (currentLine == null) {
          return false;
        }
      } else {
        if (!wholeJsonFileWasRead) {
          currentLine = reader.lines().collect(Collectors.joining(System.lineSeparator()));
          wholeJsonFileWasRead = true;
        } else {
          return false;
        }
      }

      lineNumber++;
      currentRecordKeyValue = new HashMap<>();

      if (this.jsonDataPath.getStructure() == JsonStructure.PROPERTIES) {
        JsonParser jsonParser = jsonFactory.createParser(currentLine);
        if (jsonParser.nextToken() != JsonToken.START_OBJECT) {
          throw new IOException("The line (" + lineNumber + ") is not a Json object because it does not start with " + JsonToken.START_OBJECT + " (" + currentLine + ")");
        }

        // Iterate over object fields
        while (jsonParser.nextToken() != JsonToken.END_OBJECT) {

          String columnName = jsonParser.getCurrentName();
          // Let's move to value
          jsonParser.nextToken();
          String columnValue = jsonParser.getText();
          currentRecordKeyValue.put(columnName, columnValue);
        }

        jsonParser.close();
      } else if (this.jsonDataPath.getStructure() == JsonStructure.DOCUMENT) {
        currentRecordKeyValue.put(this.jsonDataPath.getColumnName(), currentLine);
      }
      return true;

    } catch (IOException e) {
      throw new RuntimeException(e);
    }

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

  @Override
  public long getRecordId() {
    return lineNumber;
  }


  @Override
  public Object getObject(ColumnDef columnDef) {
    return currentRecordKeyValue.get(columnDef.getColumnName());
  }


  @Override
  public Object getObject(String columnName) {
    return currentRecordKeyValue.get(columnName);
  }

  @Override
  public void beforeFirst() {
    try {
      lineNumber = 0;
      reader = Files.newBufferedReader(jsonDataPath.getAbsoluteNioPath());
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public RelationDef getRuntimeRelationDef() {
    return this.jsonDataPath.getOrCreateRelationDef();
  }
}
