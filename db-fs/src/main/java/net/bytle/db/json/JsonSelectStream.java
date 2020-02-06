package net.bytle.db.json;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import net.bytle.db.model.TableDef;
import net.bytle.db.spi.SelectStreamAbs;
import net.bytle.db.stream.SelectStream;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.sql.Clob;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class JsonSelectStream extends SelectStreamAbs {

  private final JsonDataPath jsonDataPath;
  private BufferedReader reader;
  private final JsonFactory jsonFactory;
  private int lineNumber = 0;

  // A map to hold
  private Map<String,String> currentRecordKeyValue;

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
      String currentLine = reader.readLine();
      if (currentLine==null){
        return false;
      } else {
        lineNumber++;
        JsonParser jsonParser = jsonFactory.createParser(currentLine);
        if (jsonParser.nextToken() != JsonToken.START_OBJECT) {
          throw new IOException("The line ("+ lineNumber +") is not a Json object because it does not start with "+JsonToken.START_OBJECT+" ("+currentLine+")");
        }

        // Iterate over object fields
        currentRecordKeyValue = new HashMap<>();
        while (jsonParser.nextToken() != JsonToken.END_OBJECT) {

          String columnName = jsonParser.getCurrentName();
          // Let's move to value
          jsonParser.nextToken();
          String columnValue = jsonParser.getText();
          currentRecordKeyValue.put(columnName,columnValue);
        }

        jsonParser.close();
        return true;
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    }

  }

  @Override
  public void close() {

  }

  @Override
  public String getString(int columnIndex) {
    return currentRecordKeyValue.get(getColumnName(columnIndex));
  }

  private String getColumnName(int columnIndex) {
    return jsonDataPath.getDataDef().getColumnDefs().get(columnIndex).getColumnName();
  }

  @Override
  public int getRow() {
    return lineNumber;
  }

  @Override
  public Object getObject(int columnIndex) {
    return currentRecordKeyValue.get(getColumnName(columnIndex));
  }

  @Override
  public TableDef getSelectDataDef() {
    return jsonDataPath.getDataDef();
  }

  @Override
  public double getDouble(int columnIndex) {
    return Double.parseDouble(getString(columnIndex));
  }

  @Override
  public Clob getClob(int columnIndex) {
    throw new UnsupportedOperationException("Get a clob is not implemented");
  }

  @Override
  public boolean next(Integer timeout, TimeUnit timeUnit) {
    throw new UnsupportedOperationException("No Json stream iteration implemented");
  }

  @Override
  public Integer getInteger(int columnIndex) {
    return Integer.parseInt(getString(columnIndex));
  }

  @Override
  public Object getObject(String columnName) {
    return currentRecordKeyValue.get(columnName);
  }

  @Override
  public void beforeFirst() {
    try {
      lineNumber = 0;
      reader = Files.newBufferedReader(jsonDataPath.getNioPath());
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public void execute() {
    // no external request, nothing to do
  }


}
